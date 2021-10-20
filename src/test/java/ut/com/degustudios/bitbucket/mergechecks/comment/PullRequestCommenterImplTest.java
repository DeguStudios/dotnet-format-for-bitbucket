package ut.com.degustudios.bitbucket.mergechecks.comment;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.comment.*;
import com.atlassian.bitbucket.pull.*;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.util.PageImpl;
import com.atlassian.bitbucket.util.PageRequest;
import com.degustudios.bitbucket.mergechecks.comment.PullRequestCommenterImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PullRequestCommenterImplTest {
    private static final String COMMENT_HEADER_FORMAT = "For commit: [{0}]";

    @Mock
    private PullRequestService pullRequestService;
    @Mock
    private CommentService commentService;
    @Mock
    private AuthenticationContext authenticationContext;
    @Mock
    private PullRequest pullRequest;
    @Mock
    private ApplicationUser currentUser;

    private String latestHashCommentHeader;
    private ArgumentMatcher<PullRequestActivitySearchRequest> correctSearchRequest;

    @Before
    public void initialize()
    {
        String commitHashFromRef = "11111111";
        String commitHashToRef = "9999999";
        PullRequestRef toRef = mock(PullRequestRef.class);
        PullRequestRef fromRef = mock(PullRequestRef.class);
        Repository repository = mock(Repository.class);
        when(pullRequest.getToRef()).thenReturn(toRef);
        when(pullRequest.getFromRef()).thenReturn(fromRef);
        when(fromRef.getLatestCommit()).thenReturn(commitHashFromRef);
        when(fromRef.getRepository()).thenReturn(repository);
        when(toRef.getLatestCommit()).thenReturn(commitHashToRef);
        when(toRef.getRepository()).thenReturn(repository);
        when(authenticationContext.getCurrentUser()).thenReturn(currentUser);
        latestHashCommentHeader = MessageFormat.format(COMMENT_HEADER_FORMAT, commitHashFromRef);

        correctSearchRequest = new ArgumentMatcher<PullRequestActivitySearchRequest>() {
            @Override
            public boolean matches(Object o) {
                PullRequestActivitySearchRequest searchRequest = (PullRequestActivitySearchRequest) o;
                return searchRequest
                        .getTypes()
                        .equals(Sets.newHashSet(PullRequestActivityType.COMMENT))
                        && searchRequest
                        .getCommentActions()
                        .equals(Sets.newHashSet(CommentAction.ADDED, CommentAction.EDITED))
                        && searchRequest.getCommentIds().isEmpty();
            }
        };
    }

    @Test
    public void canCreateEmptyComment() {
        setupSearchToReturn(Lists.newArrayList());

        addComment("");

        verifyThatCommentWithTextWasAdded(latestHashCommentHeader);
    }

    @Test
    public void createsNewComment() {
        String comment = "COMMENT!";
        setupSearchToReturn(Lists.newArrayList());

        addComment(comment);

        verifyThatCommentWithTextWasAdded(getCommentWithLatestHeader(comment));
    }

    @Test
    public void trimsCommentWhenItsTooLong() {
        String footer = " (...)";
        int limit = 32768;
        int headerLength = (latestHashCommentHeader + "\n").length();
        int footerLength = footer.length();
        String maximumLengthComment = getStringOfLength(limit - headerLength - footerLength);
        String tooLongString = getStringOfLength(limit*2);
        setupSearchToReturn(Lists.newArrayList());

        addComment(tooLongString);

        verifyThatCommentWithTextWasAdded(latestHashCommentHeader + "\n" + maximumLengthComment + footer);
    }

    @Test
    public void doesNotCreateNewCommentWhenThereIsAlreadyOneByCurrentUserForLatestFromRefHash() {
        String comment = "COMMENT!";
        setupSearchToReturn(Lists.newArrayList(getPullRequestCommentActivity(latestHashCommentHeader)));

        addComment(comment);

        verify(commentService, times(0)).addComment(any());
    }

    @Test
    public void createsNewCommentWhenThereIsAlreadyOneByCurrentUserButForOtherHash() {
        String otherHashCommentHeader = MessageFormat.format(COMMENT_HEADER_FORMAT, "otherHashId");
        setupSearchToReturn(Lists.newArrayList(getPullRequestCommentActivity(otherHashCommentHeader)));

        addComment("");

        verifyThatCommentWithTextWasAdded(latestHashCommentHeader);
    }

    @Test
    public void createsNewCommentWhenThereIsAlreadyOneForLatestFromRefHashButByOtherUser() {
        ApplicationUser otherUser = mock(ApplicationUser.class);
        setupSearchToReturn(Lists.newArrayList(getPullRequestCommentActivity(latestHashCommentHeader, otherUser)));

        addComment("");

        verifyThatCommentWithTextWasAdded(latestHashCommentHeader);
    }

    @Test
    public void updatesExistingCommentThereIsAlreadyOneByCurrentUserAndForLatestFromRefHash() {
        String comment = "COMMENT!";
        long commentId = 123;
        int commentVersion = 7;
        setupSearchToReturn(Lists.newArrayList(getPullRequestCommentActivity(
                latestHashCommentHeader,
                1L,
                commentId,
                commentVersion)));

        addComment(comment);

        verifyThatCommentWithTextWasUpdated(getCommentWithLatestHeader(comment), commentId, commentVersion);
    }

    @Test
    public void updatesOnlyLatestExistingComment() {
        String comment = "COMMENT!";
        int commentVersion = 1;
        int commentIdOffset = 100;
        List<PullRequestCommentActivity> activities = IntStream
                .range(0, 100)
                .mapToObj(index -> getPullRequestCommentActivity(
                        latestHashCommentHeader,
                        index,
                        index+commentIdOffset,
                        commentVersion))
                .collect(Collectors.toList());
        long commentIdToUpdate = activities
                .stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId())*-1)
                .map(activity -> activity.getComment().getId())
                .findFirst()
                .get();

        setupSearchToReturn(activities);

        addComment(comment);

        verifyThatCommentWithTextWasUpdated(getCommentWithLatestHeader(comment), commentIdToUpdate, commentVersion);
    }

    private String getCommentWithLatestHeader(String comment) {
        return latestHashCommentHeader + "\n" + comment;
    }

    private void addComment(String s) {
        new PullRequestCommenterImpl(pullRequestService, commentService, authenticationContext)
                .addComment(pullRequest, s);
    }

    private void setupSearchToReturn(Collection<PullRequestCommentActivity> elements) {
        when(pullRequestService
                .searchActivities(argThat(correctSearchRequest), notNull(PageRequest.class)))
                .thenAnswer(invocationOnMock -> {
                    PageRequest request = (PageRequest) invocationOnMock.getArguments()[1];
                    return new PageImpl<>(
                            request,
                            elements.stream().skip(request.getStart()).limit(request.getLimit()).collect(Collectors.toList()),
                            elements.size() <= request.getStart() + request.getLimit());
                        }
                );
    }

    private void verifyThatCommentWithTextWasAdded(String text) {
        ArgumentMatcher<AddCommentRequest> matcher = new ArgumentMatcher<AddCommentRequest>() {
            @Override
            public boolean matches(Object o) {
                AddCommentRequest request = (AddCommentRequest) o;
                return request.getText().equals(text);
            }
        };
        verify(commentService).addComment(argThat(matcher));
    }

    private void verifyThatCommentWithTextWasUpdated(String text, long commentId, int commentVersion) {
        ArgumentMatcher<CommentUpdateRequest> matcher = new ArgumentMatcher<CommentUpdateRequest>() {
            @Override
            public boolean matches(Object o) {
                CommentUpdateRequest request = (CommentUpdateRequest) o;
                return request.getText().equals(text)
                        && request.getCommentId() == commentId
                        && request.getVersion() == commentVersion;
            }
        };
        verify(commentService).updateComment(argThat(matcher));
    }

    private PullRequestCommentActivity getPullRequestCommentActivity(String textComment) {
        return getPullRequestCommentActivity(textComment, currentUser);
    }

    private PullRequestCommentActivity getPullRequestCommentActivity(String textComment, ApplicationUser user) {
        return getPullRequestCommentActivity(textComment, user, 1,1, 0);
    }

    private PullRequestCommentActivity getPullRequestCommentActivity(String textComment, long activityId, long commentId, int commentVersion) {
        return getPullRequestCommentActivity(textComment, currentUser, activityId, commentId, commentVersion);
    }

    private PullRequestCommentActivity getPullRequestCommentActivity(String textComment, ApplicationUser user, long activityId, long commentId, int commentVersion) {
        PullRequestCommentActivity activity = mock(PullRequestCommentActivity.class);
        Comment comment = mock(Comment.class);
        when(comment.getText()).thenReturn(textComment);
        when(comment.getVersion()).thenReturn(commentVersion);
        when(comment.getId()).thenReturn(commentId);
        when(activity.getComment()).thenReturn(comment);
        when(activity.getUser()).thenReturn(user);
        when(activity.getId()).thenReturn(activityId);
        return activity;
    }

    private String getStringOfLength(int i) {
        return IntStream
                .range(0, i)
                .mapToObj(index -> "A")
                .reduce((left, right) -> left + right)
                .get();
    }
}
