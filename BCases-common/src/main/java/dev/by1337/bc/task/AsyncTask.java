package dev.by1337.bc.task;

import dev.by1337.bc.animation.AbstractAnimation;

import java.util.Collection;

public abstract class AsyncTask implements Runnable {
    private boolean isTimer;
    private long delay = 1;
    private volatile boolean cancelled;
    private long ticks;
    private Runnable onEnd;

    public AsyncTask() {
    }

    public AsyncTask(final boolean isTimer, final long delay) {
        this.isTimer = isTimer;
        this.delay = Math.max(1, delay);
    }

    public AsyncTask delay(final long delay) {
        this.delay = Math.max(1, delay);
        return this;
    }

    public static AsyncTask create(Runnable runnable) {
        return new AsyncTask() {
            @Override
            public void run() {
                runnable.run();
            }
        };
    }

    public static void joinAll(Collection<? extends AsyncTask> collection) throws InterruptedException {
        for (AsyncTask asyncTask : collection) {
            asyncTask.join();
        }
    }

    public AsyncTask timer() {
        isTimer = true;
        return this;
    }

    public AsyncTask onEnd(final Runnable onEnd) {
        this.onEnd = onEnd;
        return this;
    }

    public AsyncTask startSync(AbstractAnimation animation) throws InterruptedException {
        start(animation);
        join();
        return this;
    }

    public AsyncTask start(AbstractAnimation animation) {
        animation.addTask(this);
        return this;
    }

    public void cancel() {
        cancelled = true;
    }

    public final void onStop() {
        synchronized (this) {
            cancelled = true;
            notifyAll();
        }
        if (onEnd != null) {
            onEnd.run();
        }
    }

    public void tick() {
        if (cancelled) return;
        run();
    }

    public void join() throws InterruptedException {
        synchronized (this) {
            if (cancelled) return;
            this.wait();
        }
    }

    public boolean shouldBeTick() {
        return !cancelled && (!isTimer || ticks % delay == 0);
    }

    public void incrementTicks() {
        ticks++;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isTimer() {
        return isTimer;
    }

    public long getDelay() {
        return delay;
    }
}
