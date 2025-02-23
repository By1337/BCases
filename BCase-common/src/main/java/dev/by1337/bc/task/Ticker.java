package dev.by1337.bc.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Ticker {
    public static final int TPS = 20;
    public static final int TICK_TIME_MILS = 1000 / TPS;
    private final Logger logger;
    private final List<AsyncTask> tasks = new CopyOnWriteArrayList<>();
    private long nextTick;
    private long lastOverloadTime;
    private volatile boolean stopped;


    public Ticker(Logger logger) {
        this.logger = logger;
    }

    public void start() {
        nextTick = getMonotonicMillis();
        while (!stopped) {
            long i = (System.nanoTime() / (1000L * 1000L)) - nextTick;

            if (i > 5000L && nextTick - lastOverloadTime >= 30_000L) {
                long j = i / TICK_TIME_MILS;

                logger.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", i, j);

                nextTick += j * TICK_TIME_MILS;
                lastOverloadTime = nextTick;
            }
            nextTick += TICK_TIME_MILS;


            for (AsyncTask task : tasks) {
                if (task.shouldBeTick()) {
                    task.tick();
                }
                task.incrementTicks();
                if (task.isCancelled() || !task.isTimer()) {
                    tasks.remove(task);
                    task.cancel();
                    task.onStop();
                }
            }
            while (getMonotonicMillis() < nextTick) {
                LockSupport.parkNanos(1000L);
            }
        }
    }

    public static long getMonotonicMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    public void addTask(AsyncTask task) {
        tasks.add(task);
    }

    public void removeTask(AsyncTask task) {
        tasks.remove(task);
    }

    public void stop() {
        System.out.println("Ticker.stop");
        stopped = true;
    }
}
