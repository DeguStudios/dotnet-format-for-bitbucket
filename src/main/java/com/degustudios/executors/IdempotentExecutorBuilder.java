package com.degustudios.executors;

import com.atlassian.bitbucket.repository.RepositoryRef;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import org.springframework.stereotype.Service;

import java.util.function.BiFunction;
import java.util.function.Function;

@Service
public class IdempotentExecutorBuilder {
    public <T extends RepositoryRef, R extends DotnetFormatCommandResult> IdempotentExecutor<T, R> build(
            BiFunction<T,String, R> executeFunc,
            BiFunction<T, String, String> mapToKeyFunc) {
        return new IdempotentExecutor<>(executeFunc, mapToKeyFunc);
    }
}
