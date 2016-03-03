package com.dabsquared.gitlabjenkins.cause;

import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.model.ObjectAttributes;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Run;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Robin Müller
 */
public class GitLabMergeCause extends GitLabWebHookCause<MergeRequestHook> {

    public GitLabMergeCause(MergeRequestHook mergeRequestHook) {
        this(mergeRequestHook, "");
    }

    public GitLabMergeCause(MergeRequestHook mergeRequestHook, String pollingLog) {
        super(mergeRequestHook, pollingLog);
    }

    public GitLabMergeCause(MergeRequestHook mergeRequestHook, File logFile) throws IOException {
        super(mergeRequestHook, logFile);
    }

    @Override
    public String getBranch() {
        return getRequest().getObjectAttributes().getSourceBranch();
    }

    @Override
    public String getSourceBranch() {
        return getRequest().getObjectAttributes().getSourceBranch();
    }

    @Override
    public GitLabWebHookCause.ActionType getActionType() {
        return ActionType.MERGE;
    }

    @Override
    public String getUserName() {
        return getRequest().getObjectAttributes().getLastCommit().getAuthor().getName();
    }

    @Override
    public String getUserEmail() {
        return getRequest().getObjectAttributes().getLastCommit().getAuthor().getEmail();
    }

    @Override
    public String getSourceRepoHomepage() {
        return getRequest().getObjectAttributes().getSource().getHomepage();
    }

    @Override
    public String getSourceRepoName() {
        return getRequest().getObjectAttributes().getSource().getName();
    }

    @Override
    public String getSourceRepoUrl() {
        return getRequest().getObjectAttributes().getSource().getUrl();
    }

    @Override
    public String getSourceRepoSshUrl() {
        return getRequest().getObjectAttributes().getSource().getSshUrl();
    }

    @Override
    public String getSourceRepoHttpUrl() {
        return getRequest().getObjectAttributes().getSource().getHttpUrl();
    }

    @Override
    public String getShortDescription() {
        ObjectAttributes objectAttribute = getRequest().getObjectAttributes();
        return "GitLab Merge Request #" + objectAttribute.getIid() + " : " + objectAttribute.getSourceBranch() +
                " => " + objectAttribute.getTargetBranch();
    }

    @Override
    public Map<String, String> getBuildVariables() {
        Map<String, String> variables = super.getBuildVariables();
        putIfNotNull(variables, "gitlabMergeRequestTitle", getMergeRequestTitle());
        putIfNotNull(variables, "gitlabMergeRequestId", getMergeRequestId());
        putIfNotNull(variables, "gitlabTargetBranch", getTargetBranch());
        putIfNotNull(variables, "gitlabTargetRepoName", getTargetRepoName());
        putIfNotNull(variables, "gitlabTargetRepoSshUrl", getTargetRepoSshUrl());
        putIfNotNull(variables, "gitlabTargetRepoHttpUrl", getTargetRepoHttpUrl());
        return variables;
    }

    public String getMergeRequestTitle() {
        return getRequest().getObjectAttributes().getTitle();
    }

    public String getMergeRequestId() {
        return getRequest().getObjectAttributes().getId() == null ? null : getRequest().getObjectAttributes().getId().toString();
    }

    public String getTargetBranch() {
        return getRequest().getObjectAttributes().getTargetBranch();
    }

    public String getTargetRepoName() {
        return getRequest().getObjectAttributes().getTarget().getName();
    }

    public String getTargetRepoSshUrl() {
        return getRequest().getObjectAttributes().getTarget().getSshUrl();
    }

    public String getTargetRepoHttpUrl() {
        return getRequest().getObjectAttributes().getTarget().getHttpUrl();
    }

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void addAliases() {
        Run.XSTREAM2.addCompatibilityAlias("com.dabsquared.gitlabjenkins.GitLabMergeCause", GitLabMergeCause.class);
    }
}
