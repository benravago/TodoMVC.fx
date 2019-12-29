package fx.mvc.util;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Running implements Future<Throwable> {

    private volatile boolean done;
    private volatile Throwable end;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) { return false; }

    @Override
    public boolean isCancelled() { return false; }

    @Override
    public boolean isDone() { return done; }

    @Override
    public Throwable get() throws InterruptedException {
        return done ? end : get(0);
    }

    @Override
    public Throwable get(long timeout, TimeUnit unit) throws InterruptedException {
        return done ? end : get(TimeUnit.MILLISECONDS.convert(timeout,unit));
    }

    private synchronized Throwable get(long timeoutMillis) throws InterruptedException {
        wait(timeoutMillis);
        return end;
    }

    public synchronized void put(Throwable result) {
        done = true;
        end = result;
        notifyAll();
    }

}
