package dev.by1337.bc.task;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class SyncTask {
    private final Runnable runnable;
    private volatile boolean isDone;
    private final Plugin plugin;

    public SyncTask(Runnable runnable, Plugin plugin) {
        this.runnable = runnable;
        this.plugin = plugin;
    }

    public SyncTask start() {
        if (Bukkit.isPrimaryThread()) {
            try {
                runnable.run();
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
        synchronized (this) {
            if (isDone) return;
            wait();
        }
    }
}
