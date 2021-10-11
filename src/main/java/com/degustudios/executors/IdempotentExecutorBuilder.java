package com.degustudios.executors;

import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class IdempotentExecutorBuilder {
    public <T,R> IdempotentExecutor<T,R> build(
            Function<T,R> executeFunc,
            Function<T, String> mapToKeyFunc) {
        return new IdempotentExecutor<>(executeFunc, mapToKeyFunc);
    }
}
