package com.dabsquared.gitlabjenkins;

import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.model.PushHook;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilter;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterFactory;
import com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterType;
import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerConfig;
import com.dabsquared.gitlabjenkins.trigger.handler.merge.MergeRequestHookTriggerHandler;
import com.dabsquared.gitlabjenkins.trigger.handler.merge.MergeRequestHookTriggerHandlerFactory;
import com.dabsquared.gitlabjenkins.trigger.handler.push.PushHookTriggerHandler;
import com.dabsquared.gitlabjenkins.trigger.handler.push.PushHookTriggerHandlerFactory;
import com.dabsquared.gitlabjenkins.webhook.GitLabWebHook;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Item;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.util.SequentialExecutionQueue;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import jenkins.triggers.SCMTriggerItem.SCMTriggerItems;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;


/**
 * Triggers a build when we receive a GitLab WebHook.
 *
 * @author Daniel Brooks
 */
public class GitLabPushTrigger extends Trigger<Job<?, ?>> implements WebHookTriggerConfig {
	private static final Logger LOGGER = Logger.getLogger(GitLabPushTrigger.class.getName());
	private transient boolean triggerOnPush = true;
    private transient boolean triggerOnMergeRequest = true;
    private final String triggerOpenMergeRequestOnPush;
    private boolean ciSkip = true;
    private boolean setBuildDescription = true;
    private boolean addNoteOnMergeRequest = true;
    private boolean addCiMessage = false;
    private boolean addVoteOnMergeRequest = true;
    private transient boolean allowAllBranches = false;
    private transient String branchFilterName;
    private transient String includeBranchesSpec;
    private transient String excludeBranchesSpec;
    private transient String targetBranchRegex;
    private BranchFilter branchFilter;
    private PushHookTriggerHandler pushHookTriggerHandler;
    private MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler;
    private boolean acceptMergeRequestOnSuccess = false;


    @DataBoundConstructor
    public GitLabPushTrigger(boolean triggerOnPush, boolean triggerOnMergeRequest, String triggerOpenMergeRequestOnPush,
                             boolean ciSkip, boolean setBuildDescription, boolean addNoteOnMergeRequest, boolean addCiMessage,
                             boolean addVoteOnMergeRequest, boolean acceptMergeRequestOnSuccess, BranchFilterType branchFilterType,
                             String includeBranchesSpec, String excludeBranchesSpec, String targetBranchRegex) {
        mergeRequestHookTriggerHandler = MergeRequestHookTriggerHandlerFactory.newMergeRequestHookTriggerHandler(triggerOnMergeRequest);
        pushHookTriggerHandler = PushHookTriggerHandlerFactory.newPushHookTriggerHandler(triggerOnPush);
        this.triggerOpenMergeRequestOnPush = triggerOpenMergeRequestOnPush;
        this.ciSkip = ciSkip;
        this.setBuildDescription = setBuildDescription;
        this.addNoteOnMergeRequest = addNoteOnMergeRequest;
        this.addCiMessage = addCiMessage;
        this.addVoteOnMergeRequest = addVoteOnMergeRequest;
        this.branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec(includeBranchesSpec)
                .withExcludeBranchesSpec(excludeBranchesSpec)
                .withTargetBranchRegex(targetBranchRegex)
                .build(branchFilterType));
        this.acceptMergeRequestOnSuccess = acceptMergeRequestOnSuccess;
    }

    @Initializer(after = InitMilestone.JOB_LOADED)
    public static void migrate() throws IOException {
        for (AbstractProject<?, ?> project : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
            GitLabPushTrigger trigger = project.getTrigger(GitLabPushTrigger.class);
            if (trigger != null) {
                if (trigger.branchFilter == null) {
                    String name = StringUtils.isNotEmpty(trigger.branchFilterName) ? trigger.branchFilterName : "All";
                    trigger.branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                            .withIncludeBranchesSpec(trigger.includeBranchesSpec)
                            .withExcludeBranchesSpec(trigger.excludeBranchesSpec)
                            .withTargetBranchRegex(trigger.targetBranchRegex)
                            .build(BranchFilterType.valueOf(name)));
                }
                if (trigger.pushHookTriggerHandler == null) {
                    trigger.pushHookTriggerHandler = PushHookTriggerHandlerFactory.newPushHookTriggerHandler(trigger.triggerOnPush);
                }
                if (trigger.mergeRequestHookTriggerHandler == null) {
                    trigger.mergeRequestHookTriggerHandler =
                            MergeRequestHookTriggerHandlerFactory.newMergeRequestHookTriggerHandler(trigger.triggerOnMergeRequest);
                }
                project.save();
            }
        }
    }

    public boolean getTriggerOnPush() {
    	return pushHookTriggerHandler.isEnabled();
    }

    public boolean getTriggerOnMergeRequest() {
    	return mergeRequestHookTriggerHandler.isEnabled();
    }

    public String getTriggerOpenMergeRequestOnPush() {
        return triggerOpenMergeRequestOnPush;
    }

    public boolean getSetBuildDescription() {
        return setBuildDescription;
    }

    public boolean getAddNoteOnMergeRequest() {
        return addNoteOnMergeRequest;
    }

    public boolean getAddVoteOnMergeRequest() {
        return addVoteOnMergeRequest;
    }

    public boolean getAcceptMergeRequestOnSuccess() {
        return acceptMergeRequestOnSuccess;
    }

    /**
     * @deprecated see {@link com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher}
     */
    @Deprecated
    public boolean getAddCiMessage() {
        return addCiMessage;
    }

    /**
     * @deprecated see {@link com.dabsquared.gitlabjenkins.publisher.GitLabCommitStatusPublisher}
     */
    @Deprecated
    public void setAddCiMessage(boolean addCiMessage) {
        this.addCiMessage = addCiMessage;
    }

    @Override
    public boolean getCiSkip() {
        return ciSkip;
    }

    @Override
    public BranchFilter getBranchFilter() {
        return branchFilter;
    }

    // executes when the Trigger receives a push request
    public void onPost(final PushHook hook) {
        pushHookTriggerHandler.handle(this, job, hook);
    }

    // executes when the Trigger receives a merge request
    public void onPost(final MergeRequestHook hook) {
        mergeRequestHookTriggerHandler.handle(this, job, hook);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.get();
    }

    public static DescriptorImpl getDesc() {
        return DescriptorImpl.get();
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        Job project;
        private String gitlabApiToken;
        private String gitlabHostUrl = "";
        private boolean ignoreCertificateErrors = false;
        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);
        private transient GitLab gitlab;

        public DescriptorImpl() {
        	load();
        }

        @Override
        public boolean isApplicable(Item item) {
            if(item instanceof Job && SCMTriggerItems.asSCMTriggerItem(item) != null
                    && item instanceof ParameterizedJobMixIn.ParameterizedJob) {
                project = (Job) item;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getDisplayName() {
            if(project == null) {
                return "Build when a change is pushed to GitLab, unknown URL";
            }

            final List<String> projectParentsUrl = new ArrayList<String>();

            try {
				for (Object parent = project.getParent(); parent instanceof Item; parent = ((Item) parent)
						.getParent()) {
					projectParentsUrl.add(0, ((Item) parent).getName());
				}
			} catch (IllegalStateException e) {
				return "Build when a change is pushed to GitLab, unknown URL";
			}
			final StringBuilder projectUrl = new StringBuilder();
            projectUrl.append(Jenkins.getInstance().getRootUrl());
            projectUrl.append(GitLabWebHook.WEBHOOK_URL);
            projectUrl.append('/');
            for (final String parentUrl : projectParentsUrl) {
                projectUrl.append(Util.rawEncode(parentUrl));
                projectUrl.append('/');
            }
            projectUrl.append(Util.rawEncode(project.getName()));

            return "Build when a change is pushed to GitLab. GitLab CI Service URL: " + projectUrl;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            gitlab = new GitLab();
            return super.configure(req, formData);
        }

        public ListBoxModel doFillTriggerOpenMergeRequestOnPushItems(@QueryParameter String triggerOpenMergeRequestOnPush) {
            return new ListBoxModel(new Option("Never", "never", triggerOpenMergeRequestOnPush.matches("never") ),
                    new Option("On push to source branch", "source", triggerOpenMergeRequestOnPush.matches("source") ),
                    new Option("On push to source or target branch", "both", triggerOpenMergeRequestOnPush.matches("both") ));
        }

        private List<String> getProjectBranches(final Job<?, ?> job) throws IOException, IllegalStateException {
            if (!(job instanceof AbstractProject<?, ?>)) {
                return Lists.newArrayList();
            }

            final URIish sourceRepository = getSourceRepoURLDefault(job);

            if (sourceRepository == null) {
                throw new IllegalStateException(Messages.GitLabPushTrigger_NoSourceRepository());
            }

            if (!getGitlabHostUrl().isEmpty()) {
                return GitLabProjectBranchesService.instance().getBranches(getGitlab(), sourceRepository.toString());
            } else {
                LOGGER.log(Level.WARNING, "getProjectBranches: gitlabHostUrl hasn't been configured globally. Job {0}.",
                        job.getFullName());
                return Lists.newArrayList();
            }
        }

        private GitSCM getGitSCM(SCMTriggerItem item) {
            if(item != null) {
                for(SCM scm : item.getSCMs()) {
                    if(scm instanceof GitSCM) {
                        return (GitSCM) scm;
                    }
                }
            }
            return null;
        }

        private static List<String> splitBranchSpec(final String spec) {
            return Lists.newArrayList(Splitter.on(',').omitEmptyStrings().trimResults().split(spec));
        }

        private AutoCompletionCandidates doAutoCompleteBranchesSpec(final Job<?, ?> job, @QueryParameter final String value) {
            String query = value.toLowerCase();

            final AutoCompletionCandidates ac = new AutoCompletionCandidates();
            List<String> values = ac.getValues();

            try {
                List<String> branches = this.getProjectBranches(job);
                // show all suggestions for short strings
                if (query.length() < 2){
                    values.addAll(branches);
                } else {
                    for (String branch : branches){
                      if (branch.toLowerCase().indexOf(query) > -1){
                        values.add(branch);
                      }
                    }
                }
            } catch (final IllegalStateException ex) {
                LOGGER.log(Level.FINEST, "Unexpected IllegalStateException. Please check the logs and your configuration.", ex);
            } catch (final IOException ex) {
                LOGGER.log(Level.FINEST, "Unexpected IllegalStateException. Please check the logs and your configuration.", ex);
            }

            return ac;
        }

        public AutoCompletionCandidates doAutoCompleteIncludeBranchesSpec(@AncestorInPath final Job<?, ?> job, @QueryParameter final String value) {
            return this.doAutoCompleteBranchesSpec(job, value);
        }

        public AutoCompletionCandidates doAutoCompleteExcludeBranchesSpec(@AncestorInPath final Job<?, ?> job, @QueryParameter final String value) {
            return this.doAutoCompleteBranchesSpec(job, value);
        }

        private FormValidation doCheckBranchesSpec(@AncestorInPath final Job<?, ?> project, @QueryParameter final String value) {
            if (!project.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }

            final List<String> branchSpecs = splitBranchSpec(value);
            if (branchSpecs.isEmpty()) {
                return FormValidation.ok();
            }

            final List<String> projectBranches;
            try {
                projectBranches = this.getProjectBranches(project);
            } catch (final IllegalStateException ex) {
                return FormValidation.warning(Messages.GitLabPushTrigger_CannotConnectToGitLab(ex.getMessage()));
            } catch (final IOException ex) {
                return FormValidation.warning(project.hasPermission(Jenkins.ADMINISTER) ? ex : null,
                                              Messages.GitLabPushTrigger_CannotCheckBranches());
            }

            final Multimap<String, String> matchedSpecs = HashMultimap.create();
            final AntPathMatcher matcher = new AntPathMatcher();
            for (final String projectBranch : projectBranches) {
                for (final String branchSpec : branchSpecs) {
                    if (matcher.match(branchSpec, projectBranch)) {
                        matchedSpecs.put(branchSpec, projectBranch);
                    }
                }
            }

            branchSpecs.removeAll(matchedSpecs.keySet());
            if (!branchSpecs.isEmpty()) {
                final String unknownBranchNames = StringUtils.join(branchSpecs, ", ");
                return FormValidation.warning(Messages.GitLabPushTrigger_BranchesNotFound(unknownBranchNames));
            } else {
                final int matchedBranchesCount = Sets.newHashSet(matchedSpecs.values()).size();
                return FormValidation.ok(Messages.GitLabPushTrigger_BranchesMatched(matchedBranchesCount));
            }
        }

        public FormValidation doCheckIncludeBranchesSpec(@AncestorInPath final Job<?, ?> project, @QueryParameter final String value) {
            return this.doCheckBranchesSpec(project, value);
        }

        public FormValidation doCheckExcludeBranchesSpec(@AncestorInPath final Job<?, ?> project, @QueryParameter final String value) {
            return this.doCheckBranchesSpec(project, value);
        }

        /**
         * Get the URL of the first declared repository in the project configuration.
         * Use this as default source repository url.
         *
         * @return URIish the default value of the source repository url
         * @throws IllegalStateException Project does not use git scm.
         */
        protected URIish getSourceRepoURLDefault(Job job) {
            URIish url = null;
            SCMTriggerItem item = SCMTriggerItems.asSCMTriggerItem(job);
            GitSCM gitSCM = getGitSCM(item);
            if(gitSCM == null) {
                LOGGER.log(
                        Level.WARNING,
                        "Could not find GitSCM for project. Project = {1}, next build = {2}",
                        new String[] {
                                project.getName(),
                                String.valueOf(project.getNextBuildNumber()) });
                throw new IllegalStateException("This project does not use git:" + project.getName());
            }

            List<RemoteConfig> repositories = gitSCM.getRepositories();
            if (!repositories.isEmpty()) {
                RemoteConfig defaultRepository = repositories.get(repositories.size() - 1);
                List<URIish> uris = defaultRepository.getURIs();
                if (!uris.isEmpty()) {
                    return uris.get(uris.size() - 1);
                }
            }

            return null;
        }

        public GitLab getGitlab() {
            if (gitlab == null) {
                gitlab = new GitLab();
            }
            return gitlab;
        }

        public String getGitlabApiToken() {
            return gitlabApiToken;
        }

        public String getGitlabHostUrl() {
            return gitlabHostUrl;
        }

        public boolean getIgnoreCertificateErrors() {
        	return ignoreCertificateErrors;
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

    }
}
