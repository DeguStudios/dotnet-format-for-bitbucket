package ut.com.degustudios.bitbucket.mergechecks.comment;

import com.atlassian.bitbucket.auth.AuthenticationContext;
import com.atlassian.bitbucket.comment.AddCommentRequest;
import com.atlassian.bitbucket.comment.Comment;
import com.atlassian.bitbucket.comment.CommentAction;
import com.atlassian.bitbucket.comment.CommentService;
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

        verifyThatCommentWithTextWasAdded(latestHashCommentHeader + "\n" + comment);
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
    public void doesCreateNewCommentWhenThereIsAlreadyOneByCurrentUserButForOtherHash() {
        String otherHashCommentHeader = MessageFormat.format(COMMENT_HEADER_FORMAT, "otherHashId");
        setupSearchToReturn(Lists.newArrayList(getPullRequestCommentActivity(otherHashCommentHeader)));

        addComment("");

        verifyThatCommentWithTextWasAdded(latestHashCommentHeader);
    }

    @Test
    public void doesCreateNewCommentWhenThereIsAlreadyOneForLatestFromRefHashButByOtherUser() {
        ApplicationUser otherUser = mock(ApplicationUser.class);
        setupSearchToReturn(Lists.newArrayList(getPullRequestCommentActivity(latestHashCommentHeader, otherUser)));

        addComment("");

        verifyThatCommentWithTextWasAdded(latestHashCommentHeader);
    }

    private void addComment(String s) {
        new PullRequestCommenterImpl(pullRequestService, commentService, authenticationContext)
                .addComment(pullRequest, s);
    }

    private void setupSearchToReturn(Iterable<PullRequestActivity> elements) {
        when(pullRequestService
                .searchActivities(argThat(correctSearchRequest), notNull(PageRequest.class)))
                .thenAnswer(invocationOnMock ->
                        new PageImpl<>((PageRequest) invocationOnMock.getArguments()[1], elements, true)
                );
    }

    private void verifyThatCommentWithTextWasAdded(String text) {
        ArgumentMatcher<AddCommentRequest> addCommentRequestMatcher = new ArgumentMatcher<AddCommentRequest>() {
            @Override
            public boolean matches(Object o) {
                AddCommentRequest addCommentRequest = (AddCommentRequest) o;
                return addCommentRequest.getText().equals(text);
            }
        };
        verify(commentService).addComment(argThat(addCommentRequestMatcher));
    }

    private PullRequestActivity getPullRequestCommentActivity(String textComment) {
        return getPullRequestCommentActivity(textComment, currentUser);
    }

    private PullRequestActivity getPullRequestCommentActivity(String textComment, ApplicationUser user) {
        PullRequestCommentActivity activity = mock(PullRequestCommentActivity.class);
        Comment comment = mock(Comment.class);
        when(comment.getText()).thenReturn(textComment);
        when(activity.getComment()).thenReturn(comment);
        when(activity.getUser()).thenReturn(user);
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
