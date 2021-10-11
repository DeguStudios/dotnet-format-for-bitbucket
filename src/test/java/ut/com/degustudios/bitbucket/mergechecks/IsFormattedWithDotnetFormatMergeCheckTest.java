package ut.com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryHookVeto;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidator;
import com.degustudios.bitbucket.mergechecks.IsFormattedWithDotnetFormatMergeCheck;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;


@RunWith (MockitoJUnitRunner.class)
public class IsFormattedWithDotnetFormatMergeCheckTest
{
    @Mock
    private DotnetFormatRefValidator validator;
    @Mock
    private PreRepositoryHookContext context;
    @Mock
    private PullRequestMergeHookRequest request;
    @Mock
    private PullRequestRef pullRequestRef;

    private IsFormattedWithDotnetFormatMergeCheck checker;

    @Before
    public void initialize()
    {
        when(request.getFromRef()).thenReturn(pullRequestRef);

        checker = new IsFormattedWithDotnetFormatMergeCheck(validator);
    }

    @Test
    public void acceptsPullRequestWhenCommandReturnsZeroExitCode()
    {
        setupValidatorToReturn(0, "SUCCESS!");

        RepositoryHookResult pullRequestResult = runChecker();

        assertThat(pullRequestResult.isAccepted(), is(true));
    }

    @Test
    public void rejectsPullRequestWhenCommandReturnsNonZeroExitCode()
    {
        String errorMessage = "ERROR";
        setupValidatorToReturn(-1, errorMessage);

        RepositoryHookResult pullRequestResult = runChecker();

        assertThat(pullRequestResult.isAccepted(), is(false));
        assertThat(
                getVeto(pullRequestResult).getSummaryMessage(),
                is("Dotnet format has found issues."));
        assertThat(
                getVeto(pullRequestResult).getDetailedMessage(),
                is(errorMessage));
    }

    @Test
    public void rejectsPullRequestWhenDotnetFormatCouldNotBeRun()
    {
        String errorMessage = "ERROR";
        setupValidatorToFail(errorMessage);

        RepositoryHookResult pullRequestResult = runChecker();

        assertThat(pullRequestResult.isAccepted(), is(false));
        assertThat(
                getVeto(pullRequestResult).getSummaryMessage(),
                is("Dotnet format could not be run."));
        assertThat(
                getVeto(pullRequestResult).getDetailedMessage(),
                is(errorMessage));
    }

    private RepositoryHookVeto getVeto(RepositoryHookResult pullRequestResult) {
        return pullRequestResult.getVetoes().get(0);
    }

    private RepositoryHookResult runChecker() {
        return checker.preUpdate(context, request);
    }

    private void setupValidatorToReturn(int exitCode, String message) {
        DotnetFormatCommandResult commandResult = DotnetFormatCommandResult.executedCorrectly(exitCode, message);
        when(validator.validate(eq(pullRequestRef))).thenReturn(commandResult);
    }

    private void setupValidatorToFail(String errorMessage) {
        DotnetFormatCommandResult commandResult = DotnetFormatCommandResult.failed(errorMessage);
        when(validator.validate(eq(pullRequestRef))).thenReturn(commandResult);
    }
}