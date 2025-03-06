package dev.by1337.bc.addon;

import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.io.File;

public abstract class AbstractAddon {
    private Logger logger;
    private boolean isEnabled = false;
    private AddonDescription description;
    private ClassLoader classLoader;
    private File file;
    private Plugin plugin;


    public AbstractAddon() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        if (classLoader instanceof AddonClassLoader loader) {
            loader.initialize(this);
        } else {
            throw new IllegalArgumentException("AbstractAddon requires " + AddonClassLoader.class.getName());
        }
    }

    void init(Logger logger, AddonDescription description, ClassLoader classLoader, File file, Plugin plugin) {
        this.logger = logger;
        this.description = description;
        this.classLoader = classLoader;
        this.file = file;
        this.plugin = plugin;
    }

    protected abstract void onEnable();

    protected abstract void onDisable();

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (enabled) {
            logger.info("enabling...");
            onEnable();
        } else {
            logger.info("disabling...");
            onDisable();
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public AddonDescription getDescription() {
        return description;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
