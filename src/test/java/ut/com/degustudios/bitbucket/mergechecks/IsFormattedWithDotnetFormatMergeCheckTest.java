package ut.com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryHookVeto;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidator;
import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidatorParameterCalculator;
import com.degustudios.bitbucket.mergechecks.IsFormattedWithDotnetFormatMergeCheck;
import com.degustudios.bitbucket.mergechecks.PullRequestCommenter;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class IsFormattedWithDotnetFormatMergeCheckTest {

    @Mock
    private DotnetFormatRefValidator validator;
    @Mock
    private PullRequestCommenter pullRequestCommenter;
    @Mock
    private DotnetFormatRefValidatorParameterCalculator parameterCalculator;
    @Mock
    private PreRepositoryHookContext context;
    @Mock
    private PullRequestMergeHookRequest request;
    @Mock
    private PullRequestRef pullRequestFromRef;
    @Mock
    private PullRequest pullRequest;
    @Mock
    Repository repository;
    @Mock
    Settings settings;

    private IsFormattedWithDotnetFormatMergeCheck checker;

    @Before
    public void initialize() {
        when(request.getFromRef()).thenReturn(pullRequestFromRef);
        when(request.getPullRequest()).thenReturn(pullRequest);
        when(pullRequest.getFromRef()).thenReturn(pullRequestFromRef);
        when(pullRequestFromRef.getRepository()).thenReturn(repository);
        when(repository.getId()).thenReturn(1);
        when(context.getSettings()).thenReturn(settings);

        checker = new IsFormattedWithDotnetFormatMergeCheck(validator, pullRequestCommenter, parameterCalculator);
    }

    @Test
    public void acceptsPullRequestWhenCommandReturnsZeroExitCode() {
        when(validator.validate(eq(request.getFromRef()), any()))
                .thenReturn(DotnetFormatCommandResult.executedCorrectly(0, "SUCCESS!"));

        RepositoryHookResult pullRequestResult = runChecker();

        assertThat(pullRequestResult.isAccepted(), is(true));
    }

    @Test
    public void rejectsPullRequestWhenCommandReturnsNonZeroExitCode() {
        String errorMessage = "ERROR";
        when(validator.validate(eq(request.getFromRef()), any()))
                .thenReturn(DotnetFormatCommandResult.executedCorrectly(-1, errorMessage));

        RepositoryHookResult pullRequestResult = runChecker();

        assertThat(pullRequestResult.isAccepted(), is(false));
        assertThat(
                getVeto(pullRequestResult).getSummaryMessage(),
                is("Dotnet format has found issues."));
        assertThat(
                getVeto(pullRequestResult).getDetailedMessage(),
                is("Dotnet format exit code: -1"));
    }

    @Test
    public void addCommentToPullRequestWhenCommandReturnsNonZeroExitCode() {
        String errorMessage = "ERROR";
        String comment = "dotnet-format results:" + System.lineSeparator() + errorMessage;
        when(validator.validate(eq(request.getFromRef()), any()))
                .thenReturn(DotnetFormatCommandResult.executedCorrectly(-1, errorMessage));

        runChecker();

        verify(pullRequestCommenter).addComment(pullRequest, comment);
    }

    @Test
    public void rejectsPullRequestWhenDotnetFormatCouldNotBeRun() {
        String errorMessage = "ERROR";
        when(validator.validate(eq(request.getFromRef()), any()))
                .thenReturn(DotnetFormatCommandResult.failed(errorMessage));

        RepositoryHookResult pullRequestResult = runChecker();

        assertThat(pullRequestResult.isAccepted(), is(false));
        assertThat(
                getVeto(pullRequestResult).getSummaryMessage(),
                is("Dotnet format could not be run."));
        assertThat(
                getVeto(pullRequestResult).getDetailedMessage(),
                is(errorMessage));
    }

    @Test
    public void doesNotAddCommentToPullRequestWhenDotnetFormatCouldNotBeRun() {
        String errorMessage = "ERROR";
        when(validator.validate(eq(request.getFromRef()), any()))
                .thenReturn(DotnetFormatCommandResult.failed(errorMessage));

        runChecker();

        verify(pullRequestCommenter, times(0)).addComment(eq(pullRequest), any());
    }

    @Test
    public void testThatParamsAreCalculatedAndPassedCorrectly() {
        List<String> expectedParams = Arrays.asList(new String[]{"1", "2", "3"});
        when(parameterCalculator.calculateParameters(settings, pullRequest)).thenReturn(expectedParams);
        when(validator.validate(any(), any())).thenReturn(DotnetFormatCommandResult.failed("STUB"));

        checker.preUpdate(context, request);

        verify(validator).validate(pullRequest.getFromRef(), expectedParams);
    }

    private RepositoryHookVeto getVeto(RepositoryHookResult pullRequestResult) {
        return pullRequestResult.getVetoes().get(0);
    }

    private RepositoryHookResult runChecker() {
        return checker.preUpdate(context, request);
    }
}