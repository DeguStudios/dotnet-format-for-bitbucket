package ut.com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.degustudios.bitbucket.content.CodeService;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import com.degustudios.dotnetformat.DotnetFormatRunner;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

import com.atlassian.bitbucket.hook.repository.*;
import com.degustudios.bitbucket.mergechecks.IsFormattedWithDotnetFormatMergeCheck;


@RunWith (MockitoJUnitRunner.class)
public class IsFormattedWithDotnetFormatMergeCheckTest
{
    private final String commitId = "ref/master/1";

    @Mock
    private CodeService codeService;
    @Mock
    private DotnetFormatRunner dotnetFormatRunner;
    @Mock
    private PreRepositoryHookContext context;
    @Mock
    private PullRequestMergeHookRequest request;
    @Mock
    private Repository repository;
    @Mock
    private PullRequestRef pullRequestRef;

    private IsFormattedWithDotnetFormatMergeCheck checker;

    @Before
    public void initialize()
    {
        when(request.getRepository()).thenReturn(repository);
        when(request.getFromRef()).thenReturn(pullRequestRef);
        when(pullRequestRef.getLatestCommit()).thenReturn(commitId);

        checker = new IsFormattedWithDotnetFormatMergeCheck(codeService, dotnetFormatRunner);
    }

    @Test
    public void acceptsPullRequestWhenCommandReturnsZeroExitCode()
    {
        setupCodeServiceToAllowDownload();
        setupDotnetFormatRunnerToReturn(0, "SUCCESS!");

        RepositoryHookResult pullRequestResult = runChecker();

        assertThat(pullRequestResult.isAccepted(), is(true));
    }

    @Test
    public void rejectsPullRequestWhenCommandReturnsNonZeroExitCode()
    {
        setupCodeServiceToAllowDownload();
        String errorMessage = "ERROR";
        setupDotnetFormatRunnerToReturn(-1, errorMessage);

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
    public void rejectsPullRequestWhenCodeDownloadFails()
    {
        setupCodeServiceToFailDownload();

        RepositoryHookResult pullRequestResult = runChecker();

        assertThat(pullRequestResult.isRejected(), is(true));
        assertThat(
                getVeto(pullRequestResult).getSummaryMessage(),
                is("Dotnet format has found issues."));
        assertThat(
                getVeto(pullRequestResult).getDetailedMessage(),
                is("Downloading code failed. Check log file for more information."));
    }

    @Test
    public void cleansUpTemporaryDirectoryAfterFailingDownload()
    {
        ArgumentCaptor<Path> temporaryDirectoryCaptor = ArgumentCaptor.forClass(Path.class);
        setupCodeServiceToFailDownload();

        runChecker();

        verify(codeService).tryDownloadRepositoryCode(temporaryDirectoryCaptor.capture(), eq(repository), eq(commitId));
        assertThat(Files.exists(temporaryDirectoryCaptor.getValue()), is(false));
    }

    @Test
    public void cleansUpTemporaryDirectoryAfterCorrectDownload()
    {
        ArgumentCaptor<Path> temporaryDirectoryCaptor = ArgumentCaptor.forClass(Path.class);
        setupCodeServiceToAllowDownload();
        setupDotnetFormatRunnerToReturn(0, "OK!");

        runChecker();

        verify(codeService).tryDownloadRepositoryCode(temporaryDirectoryCaptor.capture(), eq(repository), eq(commitId));
        assertThat(Files.exists(temporaryDirectoryCaptor.getValue()), is(false));
    }

    @Test
    public void cleansUpNonEmptyTemporaryDirectoryCorrectly()
    {
        ArgumentCaptor<Path> temporaryDirectoryCaptor = ArgumentCaptor.forClass(Path.class);

        setupDotnetFormatRunnerToReturn(0, "OK!");
        when(codeService.tryDownloadRepositoryCode(notNull(Path.class), eq(repository), eq(commitId)))
                .thenAnswer(invocationOnMock -> {
                    Path temporaryDirectory = (Path) invocationOnMock.getArguments()[0];
                    File file = new File(new File(temporaryDirectory.toString()), "placeholder.txt");
                    file.createNewFile();
                    return true;
                });

        runChecker();

        verify(codeService).tryDownloadRepositoryCode(temporaryDirectoryCaptor.capture(), eq(repository), eq(commitId));
        assertThat(Files.exists(temporaryDirectoryCaptor.getValue()), is(false));
    }

    private RepositoryHookVeto getVeto(RepositoryHookResult pullRequestResult) {
        return pullRequestResult.getVetoes().get(0);
    }

    private RepositoryHookResult runChecker() {
        return checker.preUpdate(context, request);
    }

    private void setupDotnetFormatRunnerToReturn(int i, String s) {
        DotnetFormatCommandResult commandResult = new DotnetFormatCommandResult(i, s);
        when(dotnetFormatRunner.runDotnetFormat(notNull(Path.class))).thenReturn(commandResult);
    }

    private void setupCodeServiceToAllowDownload() {
        when(codeService.tryDownloadRepositoryCode(notNull(Path.class), eq(repository), eq(commitId)))
                .thenReturn(true);
    }

    private void setupCodeServiceToFailDownload() {
        when(codeService.tryDownloadRepositoryCode(notNull(Path.class), eq(repository), eq(commitId)))
                .thenReturn(false);
    }
}