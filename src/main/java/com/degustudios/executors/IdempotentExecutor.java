package com.degustudios.executors;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class IdempotentExecutor<T,R> {
    private final ConcurrentHashMap<String, LazyInitializer<Future<R>>> cache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final BiFunction<T, List<String>, R> executeFunc;
    private final BiFunction<T, List<String>, String> mapToKeyFunc;
    private final Function<R, Boolean> shouldCacheFunc;

    public IdempotentExecutor(BiFunction<T, List<String>, R> executeFunc, BiFunction<T, List<String>, String> mapToKeyFunc, Function<R, Boolean> shouldCacheFunc) {
        this.executeFunc = executeFunc;
        this.mapToKeyFunc = mapToKeyFunc;
        this.shouldCacheFunc = shouldCacheFunc;
    }

    public Future<R> execute(T param1, List<String> param2) throws ConcurrentException {
        LazyInitializer<Future<R>> lazyTask = getEarliestScheduledLazyTaskFor(param1, param2);
        return lazyTask.get();
    }

    private LazyInitializer<Future<R>> getEarliestScheduledLazyTaskFor(T param1, List<String> param2) {
        LazyInitializer<Future<R>> justScheduledLazyTask = wrapWithLazy((() -> scheduleForExecution(param1, param2)));
        LazyInitializer<Future<R>> earlierScheduledLazyTask = cache.putIfAbsent(
                mapToKeyFunc.apply(param1, param2),
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

    private Future<R> scheduleForExecution(T param1, List<String> param2) {
        return executor.submit(() -> {
            R result = executeFunc.apply(param1, param2);
            if (!Boolean.TRUE.equals(shouldCacheFunc.apply(result))) {
                cache.remove(mapToKeyFunc.apply(param1, param2));
            }
            return result;
        });
    }

    @Override
    public String toString() {
        return "IdempotentExecutor{" +
                ", executor=" + executor +
                ", executeFunc=" + executeFunc +
                ", mapToKeyFunc=" + mapToKeyFunc +
                '}';
    }
}
