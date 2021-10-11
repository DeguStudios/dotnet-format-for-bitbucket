package com.degustudios.bitbucket.repository.validators;

import com.atlassian.bitbucket.repository.RepositoryRef;
import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidator;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import com.degustudios.executors.IdempotentExecutor;
import com.degustudios.executors.IdempotentExecutorBuilder;
import org.apache.commons.lang3.concurrent.ConcurrentException;

import java.util.concurrent.ExecutionException;

public class IdempotentlyCachedDotnetFormatRefValidatorWrapper implements DotnetFormatRefValidator {
    private final IdempotentExecutor<RepositoryRef, DotnetFormatCommandResult> executor;

    public IdempotentlyCachedDotnetFormatRefValidatorWrapper(
            DotnetFormatRefValidator validator,
            IdempotentExecutorBuilder executorBuilder) {
        this.executor = executorBuilder.build(
                validator::validate,
                IdempotentlyCachedDotnetFormatRefValidatorWrapper::mapToKey);
    }

    public DotnetFormatCommandResult validate(RepositoryRef ref) {
        try {
            return executor.execute(ref).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return DotnetFormatCommandResult.failed(e);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return DotnetFormatCommandResult.failed(e);
        } catch (ConcurrentException e) {
            e.printStackTrace();
            return DotnetFormatCommandResult.failed(e);
        }
    }

    private static String mapToKey(RepositoryRef x) {
        return x.getRepository().getId() + "/" + x.getLatestCommit();
    }
}
