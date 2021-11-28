package ut.com.degustudios.executors;

import com.degustudios.executors.IdempotentExecutor;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class IdempotentExecutorTest {
    private static final int SLEEP_TIME_MS = 1000;
    public static final int EMPTY_EXECUTION_TIME_MS = 100;
    private static final Logger logger = LoggerFactory.getLogger(IdempotentExecutorTest.class);

    @Test
    public void executeReturnsFutureValueFromFunction() throws ExecutionException, InterruptedException {
        String returnValue = "TEST";
        assertThat(tryExecute((x, y) -> returnValue).get(), is(returnValue));
    }

    @Test
    public void executePassesParameterToFunction() throws ExecutionException, InterruptedException {
        String returnValue = "TEST";
        assertThat(tryExecute((String x, List<String> y) -> x, returnValue).get(), is(returnValue));
    }

    @Test
    public void executeReturnsImmediately() {
        IdempotentExecutor executor = getDefaultKeyCacheAllExecutor(IdempotentExecutorTest::sleepPassthrough);
        long start = System.currentTimeMillis();
        tryExecute(executor);
        long end = System.currentTimeMillis();

        assertThat(end - start < 100, is(true));
    }

    @Test
    public void canExecuteMultipleItemsAtTheSameTime() {
        String expectedResult = "This should run asynchronously!";
        String[] parameters = expectedResult.split(" ");
        IdempotentExecutor<String, String> executor = getDefaultKeyCacheAllExecutor(IdempotentExecutorTest::sleepPassthrough);

        long start = System.currentTimeMillis();
        String actualResult = Arrays.stream(parameters)
                .map(x -> tryExecute(executor, x))
                .collect(Collectors.toList())
                .stream()
                .map(IdempotentExecutorTest::unwrap)
                .collect(Collectors.joining(" "));
        long end = System.currentTimeMillis();

        assertThat(actualResult, is(expectedResult));
        assertThat(end - start < SLEEP_TIME_MS + EMPTY_EXECUTION_TIME_MS, is(true));
    }

    @Test
    public void willOnlyExecuteTheSameParametersOnceIfCacheFunctionReturnsFalse() {
        AtomicInteger invocationCounter = new AtomicInteger(0);
        String[] parameters = Collections.nCopies(1000, "SAME").toArray(new String[0]);
        IdempotentExecutor<String, String> executor = getDefaultKeyCacheAllExecutor((x, y) -> countingPassthrough(invocationCounter, x, y));

        Arrays.stream(parameters)
                .map(x -> tryExecute(executor, x))
                .collect(Collectors.toList())
                .stream()
                .map(IdempotentExecutorTest::unwrap)
                .collect(Collectors.toList());

        assertThat(invocationCounter.get(), is(1));
    }

    @Test
    public void willExecuteTheSameParametersAgainIfCacheFunctionReturnsFalse() {
        AtomicInteger invocationCounter = new AtomicInteger(0);
        String param = "SAME";
        IdempotentExecutor<String, String> executor = getDefaultKeyCacheNoneExecutor((x,y) -> countingPassthrough(invocationCounter, x, y));

        unwrap(tryExecute(executor, param));
        unwrap(tryExecute(executor, param));

        assertThat(invocationCounter.get(), is(2));
    }

    private <T,R> IdempotentExecutor<T,R> getDefaultKeyCacheAllExecutor(BiFunction<T, List<String>, R> executeFunc) {
        return new IdempotentExecutor<>(executeFunc, Object::toString, r -> true);
    }

    private <T,R> IdempotentExecutor<T,R> getDefaultKeyCacheNoneExecutor(BiFunction<T, List<String>, R> executeFunc) {
        return new IdempotentExecutor<>(executeFunc, Object::toString, r -> false);
    }

    private <T> Future<String> tryExecute(BiFunction<T,List<String>,String> executeFunc, String x) {
        return tryExecute(getDefaultKeyCacheAllExecutor(executeFunc), x);
    }

    private <T> Future<String> tryExecute(BiFunction<T,List<String>,String> executeFunc) {
        return tryExecute(getDefaultKeyCacheAllExecutor(executeFunc));
    }

    private Future<String> tryExecute(IdempotentExecutor executor) {
        return tryExecute(executor, "STUB");
    }

    private <V> Future<V> tryExecute(IdempotentExecutor executor, V x) {
        try {
            return executor.execute(x, Collections.singletonList("--mockParameter"));
        } catch (ConcurrentException e) {
            logger.error("Exception for conurent excception for exectur: {}", executor, e);
        }
        return null;
    }

    private static <V> V unwrap(Future<V> x) {
        try {
            return x.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("error for x: {}", x, e);
        }
        return null;
    }

    private <V> V countingPassthrough(AtomicInteger invocationCounter, V x, List<String> params) {
        invocationCounter.incrementAndGet();
        return x;
    }

    private static <V> V sleepPassthrough(V x, List<String> params) {
        try {
            Thread.sleep(SLEEP_TIME_MS);
        } catch (InterruptedException e) {
        }
        return x;
    }
}
