package dev.by1337.bc.animation;

import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.annotations.AsyncOnly;
import dev.by1337.bc.annotations.SyncOnly;
import dev.by1337.bc.prize.PrizeSelector;
import dev.by1337.bc.task.AsyncTask;
import dev.by1337.bc.task.SyncTask;
import dev.by1337.bc.task.Ticker;
import dev.by1337.bc.tracker.ViewerTracker;
import dev.by1337.bc.util.AsyncCatcher;
import dev.by1337.bc.util.ThrowingRunnable;
import dev.by1337.bc.yaml.CashedYamlContext;
import dev.by1337.virtualentity.api.virtual.VirtualEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.by1337.blib.chat.util.Message;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.geom.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public abstract class AbstractAnimation implements Animation {
    public static final Random RANDOM = new Random();
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
    protected final ViewerTracker tracker;
    protected final Vec3i blockPos;
    protected final Vec3d center;
    protected final Location location;
    protected final World world;
    protected final Message message;
    protected final Player player;
    protected final Ticker ticker;

    public AbstractAnimation(CaseBlock caseBlock, AnimationContext context, Runnable onEndCallback, PrizeSelector prizeSelector, CashedYamlContext config, Player player) {
        this.caseBlock = caseBlock;
        world = caseBlock.worldGetter().world();
        blockPos = caseBlock.pos();
        this.player = player;
        center = new Vec3d(blockPos).add(0.5, 0, 0.5);
        location = center.toLocation(world);
        message = context.message();
        this.context = context;
        this.onEndCallback = onEndCallback;
        this.prizeSelector = prizeSelector;
        this.config = config;
        String className = this.getClass().getSimpleName();
        logger = LoggerFactory.getLogger("Animation#" + className);
        main = context.createThread("Animation#main#" + className, this::run);
        taskThread = context.createThread("Animation#task#" + className, this::taskTicker);
        ticker = new Ticker(logger);
        tracker = new ViewerTracker(caseBlock.worldGetter().world(), new Vec3d(caseBlock.pos()), 30);
        addTask(AsyncTask.create(tracker::tick).delay(1).timer());
    }

    @AsyncOnly
    private void run() {
        System.out.println("AbstractAnimation.run");
        if (closed) throw new IllegalStateException("Animation is closed");
        try {
            sync(this::onStart).start().join();
            try {
                animate();
            } catch (InterruptedException ignored) {
            }
        } catch (Throwable t) {
            logger.error("Failed to play animation", t);
        } finally {
            running = false;
            if (!closed) {
                try {
                    sync(this::onEnd0).start().join();
                } catch (Throwable e) {
                    logger.error("Failed to end animation", e);
                }
            }
        }
    }

    @AsyncOnly
    private void taskTicker() {
        System.out.println("AbstractAnimation.taskTicker");
        if (closed) throw new IllegalStateException("Animation is closed");
        try {
            ticker.start();
        } catch (Throwable t) {
            logger.error("Failed to tick animation", t);
            forceStop();
        }
    }

    @SyncOnly
    protected abstract void onStart();

    @AsyncOnly
    protected abstract void animate() throws InterruptedException;

    @SyncOnly
    protected abstract void onEnd();

    @SyncOnly
    protected abstract void onClick(VirtualEntity entity, Player clicker);

    @Override
    public boolean onClick(int intId, Player clicker) {
        AsyncCatcher.catchOp("AbstractAnimation#onClick");
        System.out.println("AbstractAnimation.onClick");
        var entity = tracker.getEntity(intId);
        if (entity == null) return false;
        try {
            onClick(entity, clicker);
        } catch (Throwable t) {
            logger.error("Failed to process a click on an entity", t);
            forceStop();
        }
        return true;
    }

    public void playSound(Sound sound, float volume, float pitch) {
        world.playSound(location, sound, volume, pitch);
    }

    public void trackEntity(VirtualEntity entity) {
        tracker.addEntity(entity);
    }

    public void removeEntity(VirtualEntity entity) {
        tracker.removeEntity(entity);
    }

    public void sleepTicks(long ticks) throws InterruptedException {
        System.out.println("AbstractAnimation.sleepTicks");
        sleep(ticks * 50);
    }

    public void update() {
        System.out.println("AbstractAnimation.update");
        synchronized (main) {
            main.notifyAll();
        }
    }

    public void waitUpdateTicks(long ticks) throws InterruptedException {
        System.out.println("AbstractAnimation.waitUpdateTicks");
        waitUpdate(ticks * 50);
    }

    public void waitUpdate(long ms) throws InterruptedException {
        System.out.println("AbstractAnimation.waitUpdate");
        if (Thread.currentThread() != main) throw new IllegalStateException("Only allowed in the animation thread!");
        synchronized (main) {
            main.wait(ms);
        }
    }

    public void sleep(long ms) throws InterruptedException {
        System.out.println("AbstractAnimation.sleep");
        if (Thread.currentThread() != main) throw new IllegalStateException("Only allowed in the animation thread!");
        Thread.sleep(ms);
    }

    public SyncTask sync(ThrowingRunnable runnable) {
        System.out.println("AbstractAnimation.sync");
        return new SyncTask(runnable, context.plugin());
    }

    private void onEnd0() {
        AsyncCatcher.catchOp("AbstractAnimation#onEnd0");
        ticker.stop();
        tracker.removeAll();
        System.out.println("AbstractAnimation.onEnd0");
        try {
            onEnd();
        } catch (Throwable t) {
            logger.error("Failed to run onEnd", t);
        }
        onEndCallback.run();
    }

    @Override
    public void play() {
        System.out.println("AbstractAnimation.play");
        if (running || closed) return;
        running = true;
        taskThread.start();
        main.start();
    }

    @Override
    public void forceStop() {
        System.out.println("AbstractAnimation.forceStop");
        if (!running) return;
        closed = true;
        running = false;
        main.interrupt();
        ticker.stop();
        if (Bukkit.isPrimaryThread()) {
            onEnd0();
        } else {
            try {
                sync(this::onEnd0).start().join();
            } catch (Throwable e) {
                logger.error("Failed to end animation", e);
            }
        }
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public void addTask(AsyncTask task) {
        ticker.addTask(task);
    }

    public void removeTask(AsyncTask task) {
        ticker.removeTask(task);
    }

    public void sendMsg(@NotNull String msg) {
        message.sendMsg(player, msg);
    }

    public void sendMsg(@NotNull String msg, @NotNull Object... format) {
        message.sendMsg(player, msg, format);
    }

    public void sendTitle(@NotNull String title, @NotNull String subTitle, int fadeIn, int stay, int fadeOut) {
        message.sendTitle(player, title, subTitle, fadeIn, stay, fadeOut);
    }

    public void sendActionBar(@NotNull String msg) {
        message.sendActionBar(player, msg);
    }

}
