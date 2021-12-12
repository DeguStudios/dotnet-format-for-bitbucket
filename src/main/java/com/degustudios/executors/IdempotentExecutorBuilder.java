package com.degustudios.executors;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Service
public class IdempotentExecutorBuilder {
    public <T,R> IdempotentExecutor<T,R> build(
            BiFunction<T, List<String>, R> executeFunc,
            BiFunction<T, List<String>, String> mapToKeyFunc,
            Function<R, Boolean> shouldCacheFunc) {
        return new IdempotentExecutor<>(executeFunc, mapToKeyFunc, shouldCacheFunc);
    }
}
