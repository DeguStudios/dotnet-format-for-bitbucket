package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.pull.PullRequest;

public interface PullRequestCommenter {
    void addComment(PullRequest pullRequest, String comment);
}
