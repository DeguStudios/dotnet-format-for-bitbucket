package com.degustudios.executors;

import java.util.function.Function;

public class IdempotentExecutorBuilder {
    public <T,R> IdempotentExecutor<T,R> build(
            Function<T,R> executeFunc,
            Function<T, String> mapToKeyFunc) {
        return new IdempotentExecutor<>(executeFunc, mapToKeyFunc);
    }
}
