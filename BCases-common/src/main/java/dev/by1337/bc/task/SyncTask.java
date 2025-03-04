package dev.by1337.bc.task;

import dev.by1337.bc.util.ThrowingRunnable;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class SyncTask {
    private final ThrowingRunnable runnable;
    private volatile boolean isDone;
    private final Plugin plugin;
    private @Nullable Throwable throwable;

    public SyncTask(ThrowingRunnable runnable, Plugin plugin) {
        this.runnable = runnable;
        this.plugin = plugin;
    }

    public SyncTask start() {
        if (Bukkit.isPrimaryThread()) {
            try {
                runnable.run();
            } catch (Throwable e) {
                throwable = e;
                plugin.getSLF4JLogger().error("Failed to run task", e);
            } finally {
                synchronized (SyncTask.this) {
                    isDone = true;
                    SyncTask.this.notifyAll();
                }
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    throwable = e;
                    plugin.getSLF4JLogger().error("Failed to run task", e);
                } finally {
                    synchronized (SyncTask.this) {
                        isDone = true;
                        SyncTask.this.notifyAll();
                    }
                }
            });
        }
        return this;
    }

    public void join() throws InterruptedException {
        if (Bukkit.isPrimaryThread()) {
            if (isDone) return;
            start();
            return;
        }
        synchronized (this) {
            if (isDone) return;
            wait();
        }
        if (throwable != null){
            throw new RuntimeException(throwable);
        }
    }
}
