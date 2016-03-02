package com.dabsquared.gitlabjenkins.testhelpers;


import com.dabsquared.gitlabjenkins.model.Commit;
import com.dabsquared.gitlabjenkins.model.User;

import java.util.Date;

public class CommitBuilder {

	public static Commit buildWithDefaults() {
		return new CommitBuilder().withCommitSha("4bf0fcd937085dc2f69dcbe31f2ef960ec9ca7eb").build();
	}

	private Commit commit;

	public CommitBuilder() {
		commit = new Commit();
		commit.setAuthor(new User());
		commit.getAuthor().setName("author name");
		commit.getAuthor().setEmail("author@example.com");
		commit.setTimestamp(new Date());
	}

	public CommitBuilder withCommitSha(String commitSha) {
		commit.setId(commitSha);
		commit.setUrl("http://gitlabserver.example.com/test-group/test-repo/commit/" + commitSha);
		return this;
	}

	public Commit build() {
		return commit;
	}

}
