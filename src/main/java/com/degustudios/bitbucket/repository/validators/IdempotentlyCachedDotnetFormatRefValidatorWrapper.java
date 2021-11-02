package com.degustudios.bitbucket.repository.validators;

import com.atlassian.bitbucket.repository.RepositoryRef;
import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidator;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import com.degustudios.executors.IdempotentExecutor;
import com.degustudios.executors.IdempotentExecutorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service("IdempotentlyCachedDotnetFormatRefValidatorWrapper")
public class IdempotentlyCachedDotnetFormatRefValidatorWrapper implements DotnetFormatRefValidator {
    private final IdempotentExecutor<RepositoryRef, DotnetFormatCommandResult> executor;
    private static final Logger logger = LoggerFactory.getLogger(IdempotentlyCachedDotnetFormatRefValidatorWrapper.class);

    @Autowired
    public IdempotentlyCachedDotnetFormatRefValidatorWrapper(
            @Qualifier("DotnetFormatRefValidatorImpl") DotnetFormatRefValidator validator,
            IdempotentExecutorBuilder executorBuilder) {
        this.executor = executorBuilder.build(
                validator::validate,
                IdempotentlyCachedDotnetFormatRefValidatorWrapper::mapToKey);
    }

    public DotnetFormatCommandResult validate(RepositoryRef ref, String params) {
        try {
            return executor.execute(ref, params).get();
        } catch (InterruptedException interruptedException) {
            logger.error("Interrupted exception");
            Thread.currentThread().interrupt();
            return DotnetFormatCommandResult.failed(interruptedException);
        } catch (Exception e) {
            logger.error("Failed to execute validator for Ref ID: {}", ref.getId(), e);
            return DotnetFormatCommandResult.failed(e);
        }
    }

    private static String mapToKey(RepositoryRef x, String params) {
        return x.getRepository().getId() + "/" + x.getLatestCommit();
    }
}
