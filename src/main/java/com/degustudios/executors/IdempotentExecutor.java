package com.degustudios.executors;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

public class IdempotentExecutor<T,R> {
    private final ConcurrentHashMap<String, LazyInitializer<Future<R>>> cache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Function<T, R> executeFunc;
    private final Function<T, String> mapToKeyFunc;

    public IdempotentExecutor(Function<T,R> executeFunc, Function<T, String> mapToKeyFunc) {
        this.executeFunc = executeFunc;
        this.mapToKeyFunc = mapToKeyFunc;
    }

    public Future<R> execute(T param) throws ConcurrentException {
        LazyInitializer<Future<R>> lazyTask = getEarliestScheduledLazyTaskFor(param);
        return lazyTask.get();
    }

    private LazyInitializer<Future<R>> getEarliestScheduledLazyTaskFor(T param) {
        LazyInitializer<Future<R>> justScheduledLazyTask = wrapWithLazy((() -> scheduleForExecution(param)));
        LazyInitializer<Future<R>> earlierScheduledLazyTask = cache.putIfAbsent(
                mapToKeyFunc.apply(param),
                justScheduledLazyTask);

        if (earlierScheduledLazyTask != null) {
            return earlierScheduledLazyTask;
        } else {
            return justScheduledLazyTask;
        }
    }

    private LazyInitializer<Future<R>> wrapWithLazy(Supplier<Future<R>> supplier) {
        return new LazyInitializer<Future<R>>() {
            @Override
            protected Future<R> initialize() {
                return supplier.get();
            }
        };
    }

    private Future<R> scheduleForExecution(T param) {
        return executor.submit(() -> executeFunc.apply(param));
    }
}
