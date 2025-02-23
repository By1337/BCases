package dev.by1337.bc.animation;

import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.prize.PrizeSelector;
import dev.by1337.bc.task.AsyncTask;
import dev.by1337.bc.task.SyncTask;
import dev.by1337.bc.yaml.CashedYamlContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public abstract class AbstractAnimation implements Animation {
    protected final Logger logger;
    private final AnimationContext context;
    private final Runnable onEndCallback;
    protected final PrizeSelector prizeSelector;
    protected final CashedYamlContext config;
    protected final CaseBlock caseBlock;
    private final Thread main;
    private final Thread taskThread;
    private volatile boolean running = false;
    private volatile boolean closed = false;
    private final List<AsyncTask> tasks = new CopyOnWriteArrayList<>();

    public AbstractAnimation(CaseBlock caseBlock, AnimationContext context, Runnable onEndCallback, PrizeSelector prizeSelector, CashedYamlContext config) {
        this.caseBlock = caseBlock;
        this.context = context;
        this.onEndCallback = onEndCallback;
        this.prizeSelector = prizeSelector;
        this.config = config;
        String className = this.getClass().getSimpleName();
        logger = LoggerFactory.getLogger("Animation#" + className);
        main = context.createThread("Animation#main#" + className, this::run);
        taskThread = context.createThread("Animation#task#" + className, this::taskTicker);
    }

    private void run() {
        if (closed) throw new IllegalStateException("Animation is closed");
        try {
            onStart();
            try {
                animate();
            } catch (InterruptedException ignored) {
            }
        } finally {
            try {
                sync(this::onEnd0).join();
            } catch (Throwable e) {
                logger.error("Failed to end animation", e);
            }
        }
    }

    private void taskTicker() {
        if (closed) throw new IllegalStateException("Animation is closed");
        while (running) {
            long startTime = System.nanoTime();
            for (AsyncTask task : tasks) {
                if (task.shouldBeTick()) {
                    try {
                        task.tick();
                    } catch (Throwable t) {
                        logger.error("Произошла ошибка во время выполнения задачи {}", task, t);
                        forceStop();
                        return;
                    }
                } else {
                    task.incrementTicks();
                }
                if (task.isCancelled() || !task.isTimer()) {
                    tasks.remove(task);
                    task.cancel();
                    task.onStop();
                }
            }
            long endTime = System.nanoTime();
            long elapsedTime = endTime - startTime;
            long sleepTime = 50 - TimeUnit.NANOSECONDS.toMillis(elapsedTime);
            if (sleepTime <= 0) {
                logger.warn("Поток {} для анимации {} не успевает выполнять асинхронные задания! опоздание на {} ms.", taskThread, this.getClass().getCanonicalName(), elapsedTime);
            } else {
                try {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(sleepTime));
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                    forceStop();
                }
            }

        }
    }

    protected abstract void onStart();

    protected abstract void animate() throws InterruptedException;

    protected abstract void onEnd();

    public void sleepTicks(long ticks) throws InterruptedException {
        sleep(ticks * 50);
    }

    public void sleep(long ms) throws InterruptedException {
        if (Thread.currentThread() != main) throw new IllegalStateException("Only allowed in the animation thread!");
        Thread.sleep(ms);
    }

    public SyncTask sync(Runnable runnable) {
        return new SyncTask(runnable, context.plugin());
    }

    private void onEnd0() {
        try {
            onEnd();
        } catch (Throwable t) {
            logger.error("Failed to run onEnd", t);
        }
        onEndCallback.run();
    }

    @Override
    public void play() {
        if (running || closed) return;
        running = true;
        taskThread.start();
        main.start();
    }

    @Override
    public void forceStop() {
        close();
    }

    private void close() {
        if (!running) return;
        closed = true;
        running = false;
        taskThread.interrupt();
        main.interrupt();
    }

    public void addTask(final AsyncTask task) {
        tasks.add(task);
    }

}
