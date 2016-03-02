package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.model.Commit;
import com.dabsquared.gitlabjenkins.model.PushHook;

import java.util.ArrayList;

public class PushHookBuilder {

	public static final String ZERO_SHA = "0000000000000000000000000000000000000000";

	private PushHook pushRequest;

	public PushHookBuilder() {
		pushRequest = new PushHook();
		pushRequest.setUserId(123);
		pushRequest.setUserName("admin@example");
		pushRequest.setProjectId(345);
		pushRequest.setRepository(RepositoryBuilder.buildWithDefaults());
		pushRequest.setCommits(new ArrayList<Commit>());
	}

	public PushHookBuilder withBefore(String beforeSha) {
		pushRequest.setBefore(beforeSha);
		return this;
	}

	public PushHookBuilder withAfter(String afterSha) {
		pushRequest.setAfter(afterSha);
		return this;
	}

	public PushHookBuilder withRef(String ref) {
		pushRequest.setRef(ref);
		return this;
	}

	public PushHookBuilder addCommit(String commitSha) {
		pushRequest.getCommits().add(new CommitBuilder().withCommitSha(commitSha).build());
		return this;
	}

	public PushHook build() {
		pushRequest.setTotalCommitsCount(pushRequest.getCommits().size());
		return pushRequest;
	}

}
