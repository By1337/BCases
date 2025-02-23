package dev.by1337.bc.animation;

import org.bukkit.plugin.Plugin;

public interface AnimationContext {
    Thread createThread(String name, Runnable runnable);
    Plugin plugin();
}
