package com.degustudios.bitbucket.repository.validators;

import com.atlassian.bitbucket.repository.RepositoryRef;
import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidator;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import com.degustudios.executors.IdempotentExecutor;
import com.degustudios.executors.IdempotentExecutorBuilder;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service("IdempotentlyCachedDotnetFormatRefValidatorWrapper")
public class IdempotentlyCachedDotnetFormatRefValidatorWrapper implements DotnetFormatRefValidator {
    private final IdempotentExecutor<RepositoryRef, DotnetFormatCommandResult> executor;

    @Autowired
    public IdempotentlyCachedDotnetFormatRefValidatorWrapper(
            @Qualifier("DotnetFormatRefValidatorImpl") DotnetFormatRefValidator validator,
            IdempotentExecutorBuilder executorBuilder) {
        this.executor = executorBuilder.build(
                validator::validate,
                IdempotentlyCachedDotnetFormatRefValidatorWrapper::mapToKey);
    }

    public DotnetFormatCommandResult validate(RepositoryRef ref) {
        try {
            return executor.execute(ref).get();
        } catch (InterruptedException | ExecutionException | ConcurrentException e) {
            e.printStackTrace();
            return DotnetFormatCommandResult.failed(e);
        }
    }

    private static String mapToKey(RepositoryRef x) {
        return x.getRepository().getId() + "/" + x.getLatestCommit();
    }
}
