package ut.com.degustudios.bitbucket.repository.validators;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryRef;
import com.degustudios.bitbucket.content.CodeService;
import com.degustudios.bitbucket.repository.validators.DotnetFormatRefValidatorImpl;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import com.degustudios.dotnetformat.DotnetFormatRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


@RunWith (MockitoJUnitRunner.class)
public class DotnetFormatRefValidatorImplTest
{
    private static final String commitId = "ref/master/1";
    private static final int repositoryId = 123;

    @Mock
    private CodeService codeService;
    @Mock
    private DotnetFormatRunner dotnetFormatRunner;
    @Mock
    private Repository repository;
    @Mock
    private RepositoryRef ref;

    private List<String> params = Arrays.asList(new String[]{"--check22"});

    private DotnetFormatRefValidatorImpl validator;

    @Before
    public void initialize()
    {
        when(ref.getLatestCommit()).thenReturn(commitId);
        when(ref.getRepository()).thenReturn(repository);
        when(repository.getId()).thenReturn(repositoryId);

        validator = new DotnetFormatRefValidatorImpl(codeService, dotnetFormatRunner);
    }

    @Test
    public void returnsDotnetFormatResultWhenCodeIsDownloadedCorrectly()
    {
        setupCodeServiceToAllowDownload();
        DotnetFormatCommandResult expectedResult = setupDotnetFormatRunnerToReturn(-1, "ERROR!");

        DotnetFormatCommandResult actualResult = runValidator();

        assertThat(expectedResult, is(actualResult));
    }

    @Test
    public void returnFailedResultWhenCodeDownloadFails()
    {
        setupCodeServiceToFailDownload();

        DotnetFormatCommandResult result = runValidator();

        assertThat(result.hasExecutedCorrectly(), is(false));
        assertThat(result.getExitCode(), is(-1));
        assertThat(result.getException(), is(nullValue()));
        assertThat(
                result.getMessage(),
                is("Downloading code failed. Check log file for more information."));
    }

    @Test
    public void cleansUpTemporaryDirectoryAfterFailingDownload()
    {
        ArgumentCaptor<Path> temporaryDirectoryCaptor = ArgumentCaptor.forClass(Path.class);
        setupCodeServiceToFailDownload();

        runValidator();

        verify(codeService).tryDownloadRepositoryCode(temporaryDirectoryCaptor.capture(), eq(repository), eq(commitId));
        assertThat(Files.exists(temporaryDirectoryCaptor.getValue()), is(false));
    }

    @Test
    public void cleansUpTemporaryDirectoryAfterCorrectDownload()
    {
        ArgumentCaptor<Path> temporaryDirectoryCaptor = ArgumentCaptor.forClass(Path.class);
        setupCodeServiceToAllowDownload();
        setupDotnetFormatRunnerToReturn(0, "OK!");

        runValidator();

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

        runValidator();

        verify(codeService).tryDownloadRepositoryCode(temporaryDirectoryCaptor.capture(), eq(repository), eq(commitId));
        assertThat(Files.exists(temporaryDirectoryCaptor.getValue()), is(false));
    }

    private DotnetFormatCommandResult runValidator() {
        return validator.validate(ref, params);
    }

    private DotnetFormatCommandResult setupDotnetFormatRunnerToReturn(int exitCode, String message) {
        DotnetFormatCommandResult commandResult = DotnetFormatCommandResult.executedCorrectly(exitCode, message);
        when(dotnetFormatRunner.runDotnetFormat(notNull(Path.class), eq(params))).thenReturn(commandResult);
        return commandResult;
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