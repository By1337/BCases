package dev.by1337.bc.animation;

import org.bukkit.plugin.Plugin;
import org.by1337.blib.chat.util.Message;

public class AnimationContextImpl implements AnimationContext {
    private final Plugin plugin;
    private final Message message;

    public AnimationContextImpl(Plugin plugin) {
        this.plugin = plugin;
        message = new Message(plugin.getLogger());
    }

    @Override
    public Thread createThread(String name, Runnable runnable) {
        Thread t = new Thread(runnable);
        t.setName(name);
        return t;
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    @Override
    public Message message() {
        return message;
    }
}
