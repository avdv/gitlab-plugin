package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty;
import com.dabsquared.gitlabjenkins.model.Commit;
import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.model.ObjectAttributes;
import com.dabsquared.gitlabjenkins.model.PushHook;
import com.dabsquared.gitlabjenkins.util.GsonUtil;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import hudson.model.AbstractProject;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabBranch;
import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.kohsuke.stapler.StaplerResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public class PushBuildAction implements WebHookAction {

    private final static Logger LOGGER = Logger.getLogger(PushBuildAction.class.getName());
    private final AbstractProject<?, ?> project;

    private PushHook pushHook;

    public PushBuildAction(AbstractProject<?, ?> project, String json) {
        this.project = project;
        this.pushHook = GsonUtil.getGson().fromJson(json, PushHook.class);
    }

    public void execute(StaplerResponse response) {
        String repositoryUrl = pushHook.getRepository().getUrl();
        if (repositoryUrl == null) {
            LOGGER.log(Level.WARNING, "No repository url found.");
            return;
        }

        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            public void run() {
                GitLabPushTrigger trigger = project.getTrigger(GitLabPushTrigger.class);
                if (trigger != null) {
                    trigger.onPost(pushHook);

                    if (!trigger.getTriggerOpenMergeRequestOnPush().equals("never")) {
                        // Fetch and build open merge requests with the same source branch
                        buildOpenMergeRequests(trigger, pushHook.getProjectId(), pushHook.getRef());
                    }
                }
            }
        });
        throw HttpResponses.ok();
    }

    protected void buildOpenMergeRequests(final GitLabPushTrigger trigger, final Integer projectId, String projectRef) {
        try {
            GitLabConnectionProperty property = project.getProperty(GitLabConnectionProperty.class);
            if (property != null && property.getClient() != null) {
                GitlabAPI client = property.getClient();
                for (final GitlabMergeRequest mergeRequest : client.getOpenMergeRequests(projectId)) {
                    String sourceBranch = mergeRequest.getSourceBranch();
                    String targetBranch = mergeRequest.getTargetBranch();
                    if (projectRef.endsWith(sourceBranch) || (trigger.getTriggerOpenMergeRequestOnPush().equals("both") && projectRef.endsWith(targetBranch))) {
                        if (trigger.getCiSkip() && mergeRequest.getDescription().contains("[ci-skip]")) {
                            LOGGER.log(Level.INFO, "Skipping MR " + mergeRequest.getTitle() + " due to ci-skip.");
                            continue;
                        }
                        final Commit lastCommit = createLastCommit(projectId, client.getBranch(createProject(projectId), sourceBranch));
                        ACL.impersonate(ACL.SYSTEM, new Runnable() {
                            public void run() {
                                trigger.onPost(createMergeRequest(projectId, mergeRequest, lastCommit));
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to communicate with gitlab server to determine if this is an update for a merge request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private GitlabProject createProject(Integer projectId) {
        GitlabProject project = new GitlabProject();
        project.setId(projectId);
        return project;
    }

    private Commit createLastCommit(Integer projectId, GitlabBranch branch) {
        Commit result = new Commit();
        result.setId(branch.getCommit().getId());
        result.setMessage(branch.getCommit().getMessage());
        result.setUrl(GitlabProject.URL + "/" + projectId + "/repository" + GitlabCommit.URL + "/"
                + branch.getCommit().getId());
        return result;
    }

    private MergeRequestHook createMergeRequest(Integer projectId, GitlabMergeRequest mergeRequest, Commit lastCommit) {
        MergeRequestHook result = new MergeRequestHook();
        result.setObjectKind("merge_request");
        result.setObjectAttributes(createObjectAttributes(projectId, mergeRequest, lastCommit));
        return result;
    }

    private ObjectAttributes createObjectAttributes(Integer projectId, GitlabMergeRequest mergeRequest, Commit lastCommit) {
        ObjectAttributes result = new ObjectAttributes();
        result.setAssigneeId(mergeRequest.getAssignee() == null ? null : mergeRequest.getAssignee().getId());
        result.setAuthorId(mergeRequest.getAuthor().getId());
        result.setDescription(mergeRequest.getDescription());
        result.setId(mergeRequest.getId());
        result.setIid(mergeRequest.getIid());
        result.setMergeStatus(mergeRequest.getState());
        result.setSourceBranch(mergeRequest.getSourceBranch());
        result.setSourceProjectId(mergeRequest.getSourceProjectId());
        result.setTargetBranch(mergeRequest.getTargetBranch());
        result.setTargetProjectId(projectId);
        result.setTitle(mergeRequest.getTitle());
        result.setLastCommit(lastCommit);
        return result;
    }
}
