package com.dabsquared.gitlabjenkins.testhelpers;

import static com.dabsquared.gitlabjenkins.testhelpers.PushHookBuilder.ZERO_SHA;

import com.dabsquared.gitlabjenkins.GitLabPushRequest;
import com.dabsquared.gitlabjenkins.model.PushHook;

public class GitLabPushRequestSamples_7_10_5_489b413 implements GitLabPushRequestSamples {

	private static final String COMMIT_7A = "7a5db3baf5d42b4218a2a501c88f745559b1d24c";
	private static final String COMMIT_21 = "21d67fe28097b49a1a6fb5c82cbfe03d389e8685";
	private static final String COMMIT_9d = "9dbdd7a128a2789d0f436222ce116f1d8229e083";

	public PushHook pushBrandNewMasterBranchRequest() {
		return new PushHookBuilder().withRef("refs/heads/master").withBefore(ZERO_SHA)
				.withAfter(COMMIT_7A)
				// no commit on new branches
				.build();
	}

	public PushHook pushNewBranchRequest() {
		return new PushHookBuilder().withRef("refs/heads/test-new-branch1")
				.withBefore(ZERO_SHA).withAfter(COMMIT_7A)
				// no commit on new branches
				.build();
	}

	public PushHook pushCommitRequest() {
		return new PushHookBuilder().withRef("refs/heads/test-new-branch1")
				.withBefore(COMMIT_7A).withAfter(COMMIT_21).addCommit(COMMIT_21).build();
	}

	public PushHook mergePushRequest() {
		return new PushHookBuilder().withRef("refs/heads/master")
				.withBefore("ca84f96a846b0e241808ea7b75dfa3bf4cd3b98b").withAfter(COMMIT_9d)
				.addCommit(COMMIT_21).addCommit("c04c8822d1df397fb7e6dd3dd133018a0af567a8").addCommit(COMMIT_9d)
				.build();
	}

	public PushHook pushNewTagRequest() {
		return new PushHookBuilder().withRef("refs/tags/test-tag-1")
				.withBefore(ZERO_SHA).withAfter(COMMIT_21).addCommit(COMMIT_21).build();
	}

	public PushHook deleteBranchRequest() {
		return new PushHookBuilder().withRef("refs/heads/test-branch-3-delete")
				.withBefore("c34984ff6ed9935b3d843237947adbaaa85fc5f9").withAfter(ZERO_SHA)
				.build();
	}

}
