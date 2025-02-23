package dev.by1337.bc.animation;

import org.bukkit.plugin.Plugin;
import org.by1337.blib.chat.util.Message;

public interface AnimationContext {
    Thread createThread(String name, Runnable runnable);
    Plugin plugin();
    Message message();
}
