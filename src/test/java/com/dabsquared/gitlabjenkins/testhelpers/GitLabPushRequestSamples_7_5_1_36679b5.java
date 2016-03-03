package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.model.PushHook;

import static com.dabsquared.gitlabjenkins.testhelpers.PushHookBuilder.ZERO_SHA;

public class GitLabPushRequestSamples_7_5_1_36679b5 implements GitLabPushRequestSamples {

	public PushHook pushBrandNewMasterBranchRequest() {
		return new PushHookBuilder().withRef("refs/heads/master").withBefore(ZERO_SHA)
				.withAfter("d91a0f248625f6dc808fb7cda75c4ee01516b609")
				// no checkout_sha and no commit on new branches
				.build();
	}

	public PushHook pushNewBranchRequest() {
		return new PushHookBuilder().withRef("refs/heads/test-new-branch1")
				.withBefore(ZERO_SHA).withAfter("2bf4170829aedd706d7485d40091a01637b9abf4")
				// no checkout_sha and no commit on new branches
				.build();
	}

	public PushHook pushCommitRequest() {
		return new PushHookBuilder().withRef("refs/heads/test-new-branch1")
				.withBefore("2bf4170829aedd706d7485d40091a01637b9abf4")
				.withAfter("4bf0fcd937085dc2f69dcbe31f2ef960ec9ca7eb")
				// no checkout_sha
				.addCommit("4bf0fcd937085dc2f69dcbe31f2ef960ec9ca7eb").build();
	}

	public PushHook mergePushRequest() {
		return new PushHookBuilder().withRef("refs/heads/master")
				.withBefore("27548e742f40971f75c715aaa7920404eeff6616")
				.withAfter("3ebb6927ad4afbe8a11830938b3584cdaf4d657b")
				// no checkout_sha
				.addCommit("4bf0fcd937085dc2f69dcbe31f2ef960ec9ca7eb")
				.addCommit("be473fcc670b920cc9795581a5cd8f00fa7afddd")
				.addCommit("3ebb6927ad4afbe8a11830938b3584cdaf4d657b").build();
		// and afterwards the "delete branch" request comes in
	}

	public PushHook pushNewTagRequest() {
		return new PushHookBuilder().withRef("refs/tags/test-tag-2")
				.withBefore(ZERO_SHA).withAfter("f10d9d7b648e5a3e55fe8fe865aba5aa7404df7c")
				// no checkout_sha and no commit on new branches
				.build();
	}

	public PushHook deleteBranchRequest() {
		return new PushHookBuilder().withRef("refs/heads/test-branch-delete-1")
				.withBefore("3ebb6927ad4afbe8a11830938b3584cdaf4d657b").withAfter(ZERO_SHA)
				// no checkout_sha and no commit on new branches
				.build();
	}

}
