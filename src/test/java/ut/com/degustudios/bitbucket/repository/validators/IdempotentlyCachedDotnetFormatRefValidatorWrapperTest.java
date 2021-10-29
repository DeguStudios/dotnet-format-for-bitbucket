package ut.com.degustudios.bitbucket.repository.validators;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryRef;
import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidator;
import com.degustudios.bitbucket.repository.validators.IdempotentlyCachedDotnetFormatRefValidatorWrapper;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import com.degustudios.executors.IdempotentExecutor;
import com.degustudios.executors.IdempotentExecutorBuilder;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IdempotentlyCachedDotnetFormatRefValidatorWrapperTest {
    @Mock
    private DotnetFormatRefValidator validator;
    @Mock
    private IdempotentExecutorBuilder executorBuilder;
    @Mock
    private RepositoryRef ref;
    @Mock
    private IdempotentExecutor<RepositoryRef, DotnetFormatCommandResult> executor;
    @Captor
    private ArgumentCaptor<BiFunction<RepositoryRef, String, DotnetFormatCommandResult>> scheduleFuncCaptor;
    @Captor
    private ArgumentCaptor<BiFunction<RepositoryRef, String, String>> keyMapFuncCaptor;
    @Mock
    private Repository repository;

    private String params;

    @Before
    public void initialize(){
        when(executorBuilder.<RepositoryRef, DotnetFormatCommandResult>build(any(), any())).thenReturn(executor);
        params = "--check";
    }

    @Test
    public void schedulesDotnetFormatValidation() throws ConcurrentException {
        when(executor.execute(ref, params)).thenReturn(CompletableFuture.completedFuture(null));

        runValidatorWrapper();

        verify(executorBuilder).build(scheduleFuncCaptor.capture(), any());
        verify(validator, times(0)).validate(any(), eq(params));
        scheduleFuncCaptor.getValue().apply(ref, params);
        verify(validator, times(1)).validate(ref, params);
    }

    @Test
    public void correctlyMapsRefToKey() throws ConcurrentException {
        String commitId = "19873";
        int repositoryId = 124;
        when(ref.getLatestCommit()).thenReturn(commitId);
        when(ref.getRepository()).thenReturn(repository);
        when(repository.getId()).thenReturn(repositoryId);
        when(executor.execute(ref, params)).thenReturn(CompletableFuture.completedFuture(null));

        runValidatorWrapper();

        verify(executorBuilder).build(any(), keyMapFuncCaptor.capture());
        assertThat(keyMapFuncCaptor.getValue().apply(ref, params), is(repositoryId + "/" + commitId));
    }

    @Test
    public void returnsDotnetFormatValidationFromExecutor() throws ConcurrentException {
        DotnetFormatCommandResult expectedResult = DotnetFormatCommandResult.executedCorrectly(0, "OK!");
        when(executorBuilder.<RepositoryRef, DotnetFormatCommandResult>build(any(), any())).thenReturn(executor);
        when(executor.execute(ref, params)).thenReturn(CompletableFuture.completedFuture(expectedResult));

        DotnetFormatCommandResult actualResult = runValidatorWrapper();

        assertThat(actualResult, is(expectedResult));
    }

    @Test
    public void onlyBuildsOneExecutor() throws ConcurrentException {
        when(executor.execute(ref, params)).thenReturn(CompletableFuture.completedFuture(null));

        IdempotentlyCachedDotnetFormatRefValidatorWrapper wrapper = new IdempotentlyCachedDotnetFormatRefValidatorWrapper(validator, executorBuilder);
        wrapper.validate(ref, params);
        wrapper.validate(ref, params);
        wrapper.validate(ref, params);

        verify(executorBuilder, times(1)).build(any(), any());
    }

    @Test
    public void handlesInterruptedException() throws ConcurrentException {
        InterruptedException exception = new InterruptedException("ERROR!");
        CompletableFuture<DotnetFormatCommandResult> future = new CompletableFuture<>();
        future.completeExceptionally(exception);
        when(executor.execute(any(), eq(params))).thenReturn(future);

        DotnetFormatCommandResult result = runValidatorWrapper();

        assertThat(result.getException(), is(notNullValue()));
        assertThat(result.getExitCode(), is(-1));
        assertThat(result.getMessage(), is("Failed to execute dotnet-format command: java.lang.InterruptedException: " + exception.getMessage()));
        assertThat(result.hasExecutedCorrectly(), is(false));
    }

    @Test
    public void handlesExecutionException() throws ConcurrentException {
        when(executor.execute(any(), eq(params))).thenReturn(CompletableFuture.supplyAsync(this::throwException));

        DotnetFormatCommandResult result = runValidatorWrapper();

        assertThat(result.getException(), is(notNullValue()));
        assertThat(result.getExitCode(), is(-1));
        assertThat(result.getMessage(), is("Failed to execute dotnet-format command: java.lang.RuntimeException"));
        assertThat(result.hasExecutedCorrectly(), is(false));
    }

    @Test
    public void handlesConcurrentException() throws ConcurrentException {
        when(executor.execute(any(), eq(params))).thenThrow(ConcurrentException.class);

        DotnetFormatCommandResult result = runValidatorWrapper();

        assertThat(result.getException(), is(notNullValue()));
        assertThat(result.getExitCode(), is(-1));
        assertThat(result.getMessage(), is("Failed to execute dotnet-format command: null"));
        assertThat(result.hasExecutedCorrectly(), is(false));
    }

    private DotnetFormatCommandResult throwException() {
        throw new RuntimeException();
    }

    private DotnetFormatCommandResult runValidatorWrapper() {
        return new IdempotentlyCachedDotnetFormatRefValidatorWrapper(validator, executorBuilder).validate(ref, params);
    }
}
