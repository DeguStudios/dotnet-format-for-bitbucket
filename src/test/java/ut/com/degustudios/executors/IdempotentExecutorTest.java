package ut.com.degustudios.executors;

import com.degustudios.executors.IdempotentExecutor;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class IdempotentExecutorTest {
    private static final int SLEEP_TIME_MS = 1000;
    public static final int EMPTY_EXECUTION_TIME_MS = 100;

    @Test
    public void executeReturnsFutureValueFromFunction() throws ExecutionException, InterruptedException {
        String returnValue = "TEST";
        assertThat(tryExecute(x -> returnValue).get(), is(returnValue));
    }

    @Test
    public void executePassesParameterToFunction() throws ExecutionException, InterruptedException {
        String returnValue = "TEST";
        assertThat(tryExecute((String x) -> x, returnValue).get(), is(returnValue));
    }

    @Test
    public void executeReturnsImmediately() {
        IdempotentExecutor executor = getDefaultKeyExecutor(IdempotentExecutorTest::sleepPassthrough);
        long start = System.currentTimeMillis();
        tryExecute(executor);
        long end = System.currentTimeMillis();

        assertThat(end - start < 100, is(true));
    }

    @Test
    public void canExecuteMultipleItemsAtTheSameTime() {
        String expectedResult = "This should run asynchronously!";
        String[] parameters = expectedResult.split(" ");
        IdempotentExecutor<String, String> executor = getDefaultKeyExecutor(IdempotentExecutorTest::sleepPassthrough);

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
    public void willOnlyExecuteTheSameParametersOnce() {
        AtomicInteger invocationCounter = new AtomicInteger(0);
        String[] parameters = Collections.nCopies(1000, "SAME").toArray(new String[0]);
        IdempotentExecutor<String, String> executor = getDefaultKeyExecutor(x -> countingPassthrough(invocationCounter, x));

        Arrays.stream(parameters)
                .map(x -> tryExecute(executor, x))
                .collect(Collectors.toList())
                .stream()
                .map(IdempotentExecutorTest::unwrap)
                .collect(Collectors.toList());

        assertThat(invocationCounter.get(), is(1));
    }

    private <T,R> IdempotentExecutor<T,R> getDefaultKeyExecutor(Function<T,R> executeFunc) {
        return new IdempotentExecutor<>(executeFunc, x -> x.toString());
    }

    private <T> Future<String> tryExecute(Function<T,String> executeFunc, String x) {
        return tryExecute(getDefaultKeyExecutor(executeFunc), x);
    }

    private <T> Future<String> tryExecute(Function<T,String> executeFunc) {
        return tryExecute(getDefaultKeyExecutor(executeFunc));
    }

    private Future<String> tryExecute(IdempotentExecutor executor) {
        return tryExecute(executor, "STUB");
    }

    private <V> Future<V> tryExecute(IdempotentExecutor executor, V x) {
        try {
            return executor.execute(x);
        } catch (ConcurrentException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static <V> V unwrap(Future<V> x) {
        try {
            return x.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <V> V countingPassthrough(AtomicInteger invocationCounter, V x) {
        invocationCounter.incrementAndGet();
        return x;
    }

    private static <V> V sleepPassthrough(V x) {
        try {
            Thread.sleep(SLEEP_TIME_MS);
        } catch (InterruptedException e) {
        }
        return x;
    }
}
