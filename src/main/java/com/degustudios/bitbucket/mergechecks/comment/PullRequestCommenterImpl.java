package com.degustudios.bitbucket.mergechecks.comment;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.comment.*;
import com.atlassian.bitbucket.pull.*;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.degustudios.bitbucket.mergechecks.PullRequestCommenter;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PullRequestCommenterImpl implements PullRequestCommenter {
    private final PullRequestService pullRequestService;
    private final Pattern commentTextPattern = Pattern.compile("^For commit: \\[([a-f0-9]*)\\]\n.*", Pattern.MULTILINE | Pattern.DOTALL);
    private final CommentService commentService;
    private final AuthenticationContext authenticationContext;
    private int pageSize = 1;
    private int characterLimitPerComment = 32768;

    public PullRequestCommenterImpl(@ComponentImport PullRequestService pullRequestService,
                                    @ComponentImport CommentService commentService,
                                    @ComponentImport AuthenticationContext authenticationContext) {
        this.pullRequestService = pullRequestService;
        this.commentService = commentService;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public void addComment(PullRequest pullRequest, String textComment) {
        ApplicationUser user = authenticationContext.getCurrentUser();
        Set<PullRequestCommentActivity> serviceComments = new HashSet<>();
        Page<PullRequestActivity> query = null;
        do {
            PageRequest pageRequest = query == null
                    ? new PageRequestImpl(0, pageSize)
                    : query.getNextPageRequest().buildRestrictedPageRequest(pageSize);
            query = getPageOfComments(pullRequest, pageRequest);

            serviceComments.addAll(query.stream()
                    .map(activity -> (PullRequestCommentActivity)activity)
                    .filter(activity -> activity.getUser().equals(user))
                    .filter(activity -> isAboutCurrentChange(activity.getComment(), pullRequest))
                    .collect(Collectors.toSet()));
        }
        while (!query.getIsLastPage());

        Optional<PullRequestCommentActivity> optionalActivity = serviceComments.stream()
                .sorted(Comparator.comparing(PullRequestActivity::getId).reversed())
                .findAny();

        if (optionalActivity.isPresent()) {
            Comment comment = optionalActivity.get().getComment();
            commentService.updateComment(new CommentUpdateRequest.Builder(comment.getId())
                    .text(formatComment(pullRequest, textComment))
                    .version(comment.getVersion())
                    .build());
        } else {
            commentService.addComment(
                    new AddCommentRequest.Builder(pullRequest, formatComment(pullRequest, textComment)).build());
        }
    }

    private String formatComment(PullRequest pullRequest, String textComment) {
        return TrimTo(
                "For commit: [" + getLatestCommit(pullRequest) + "]" + "\n" + textComment,
                characterLimitPerComment);
    }

    private String TrimTo(String text, int limt) {
        if (text.length() <= limt) {
            return text;
        }
        String trimEnder = " (...)";
        return text.substring(0, limt-trimEnder.length()) + trimEnder;
    }

    private String getLatestCommit(PullRequest pullRequest) {
        return pullRequest.getFromRef().getLatestCommit();
    }

    private boolean isAboutCurrentChange(Comment comment, PullRequest pullRequest) {
        Matcher matcher = commentTextPattern.matcher(comment.getText());
        if (!matcher.matches()) {
            return false;
        }
        String commitId = matcher.group(1);
        return getLatestCommit(pullRequest).equals(commitId);
    }

    private Page<PullRequestActivity> getPageOfComments(PullRequest pullRequest, PageRequest page) {
        return pullRequestService.searchActivities(new PullRequestActivitySearchRequest.Builder(pullRequest)
                .types(PullRequestActivityType.COMMENT)
                .commentActions(CommentAction.ADDED, CommentAction.EDITED)
                .build(), page);
    }
}
