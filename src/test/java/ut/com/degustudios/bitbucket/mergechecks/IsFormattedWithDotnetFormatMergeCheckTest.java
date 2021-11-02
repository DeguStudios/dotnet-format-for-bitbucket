package ut.com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryHookVeto;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.Settings;
import com.degustudios.bitbucket.content.CodeService;
import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidator;
import com.degustudios.bitbucket.mergechecks.IsFormattedWithDotnetFormatMergeCheck;
import com.degustudios.bitbucket.mergechecks.PullRequestCommenter;
import com.degustudios.bitbucket.repository.validators.DotnetFormatRefValidatorImpl;
import com.degustudios.bitbucket.repository.validators.IdempotentlyCachedDotnetFormatRefValidatorWrapper;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import com.degustudios.dotnetformat.DotnetFormatRunner;
import com.degustudios.executors.IdempotentExecutorBuilder;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class IsFormattedWithDotnetFormatMergeCheckTest {

    private DotnetFormatRefValidator validator;

    @Mock
    CodeService codeService;
    @Mock
    private PreRepositoryHookContext context;
    @Mock
    private PullRequestMergeHookRequest request;
    @Mock
    private PullRequestRef pullRequestFromRef;
    @Mock
    private PullRequestCommenter pullRequestCommenter;
    @Mock
    private PullRequest pullRequest;
    @Mock
    Repository repository;
    @Mock
    Settings settings;

    private String PARAMS = "--testParam2";
    private IsFormattedWithDotnetFormatMergeCheck checker;
    private String testedParams;

    private NativeCommandRunner nativeCommandRunner = new NativeCommandRunner() {
        @Override
        public DotnetFormatCommandResult runCommand(File directory, String... command) {
            testedParams = command[3];
            return result;
        }
    };

    private DotnetFormatCommandResult result;

    @Before
    public void initialize() {
        when(request.getFromRef()).thenReturn(pullRequestFromRef);
        when(request.getPullRequest()).thenReturn(pullRequest);
        when(pullRequest.getFromRef()).thenReturn(pullRequestFromRef);

        when(pullRequestFromRef.getRepository()).thenReturn(repository);
        when(repository.getId()).thenReturn(1);

        when((codeService.tryDownloadRepositoryCode(Mockito.any(), Mockito.any(), Mockito.anyString()))).thenReturn(true);

        when(context.getSettings()).thenReturn(settings);

        DotnetFormatRunner dotnetFormatRunner = new DotnetFormatRunner(nativeCommandRunner);

        DotnetFormatRefValidator innerValidator = new DotnetFormatRefValidatorImpl(codeService, dotnetFormatRunner);
        validator = new IdempotentlyCachedDotnetFormatRefValidatorWrapper(innerValidator, new IdempotentExecutorBuilder());
        checker = new IsFormattedWithDotnetFormatMergeCheck(validator, pullRequestCommenter);
    }

    @Test
    public void acceptsPullRequestWhenCommandReturnsZeroExitCode() {
        result = DotnetFormatCommandResult.executedCorrectly(0, "SUCCESS!");

        RepositoryHookResult pullRequestResult = runChecker();

        assertThat(pullRequestResult.isAccepted(), is(true));
    }

    @Test
    public void rejectsPullRequestWhenCommandReturnsNonZeroExitCode() {
        String errorMessage = "ERROR";
        result = DotnetFormatCommandResult.executedCorrectly(-1, errorMessage);

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
        result = DotnetFormatCommandResult.executedCorrectly(-1, errorMessage);

        runChecker();

        verify(pullRequestCommenter).addComment(pullRequest, comment);
    }

    @Test
    public void rejectsPullRequestWhenDotnetFormatCouldNotBeRun() {
        String errorMessage = "ERROR";
        result= DotnetFormatCommandResult.failed(errorMessage);

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
        result= DotnetFormatCommandResult.failed(errorMessage);

        runChecker();

        verify(pullRequestCommenter, times(0)).addComment(eq(pullRequest), any());
    }

    private RepositoryHookVeto getVeto(RepositoryHookResult pullRequestResult) {
        return pullRequestResult.getVetoes().get(0);
    }

    private RepositoryHookResult runChecker() {
        return checker.preUpdate(context, request);
    }


    @Test
    public void testThatParamsArePassedToChecker() {
        when(settings.getString(IsFormattedWithDotnetFormatMergeCheck.DOTNET_FORMAT_PARAMS)).thenReturn(PARAMS);
        result = DotnetFormatCommandResult.executedCorrectly(0, "SUCCESS!");

        checker.preUpdate(context, request);
        assertThat(testedParams, is(PARAMS));
    }

    @Test
    public void testThatParamsNullArePassedToChecker() {
        when(settings.getString(IsFormattedWithDotnetFormatMergeCheck.DOTNET_FORMAT_PARAMS)).thenReturn(null);
        result = DotnetFormatCommandResult.executedCorrectly(0, "SUCCESS!");

        checker.preUpdate(context, request);
        assertThat(testedParams, IsNull.nullValue());
    }
}