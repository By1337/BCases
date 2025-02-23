package dev.by1337.bc;

import dev.by1337.bc.animation.AnimationContext;
import dev.by1337.bc.animation.AnimationContextImpl;
import dev.by1337.bc.animation.AnimationLoader;
import dev.by1337.bc.bd.Database;
import dev.by1337.bc.db.DebugDatabase;
import dev.by1337.bc.prize.PrizeMap;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.util.ResourceUtil;
import org.by1337.blib.util.invoke.LambdaMetafactoryUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.function.BiConsumer;

public class BCase extends JavaPlugin {
    private static final BiConsumer<JavaPlugin, Boolean> IS_ENABLED_SETTER;
    private PrizeMap prizeMap;
    private BlockManager blockManager;
    private Database database;
    private AnimationContext animationContext;
    private AnimationLoader animationLoader;

    @Override
    public void onLoad() {
        if (!new File(getDataFolder(), "animations").exists()) {
            ResourceUtil.saveIfNotExist("animations/randMobs.yml", this);
        }
    }

    @Override
    public void onEnable() {
        prizeMap = ResourceUtil.load("prizes.yml", this).get().decode(PrizeMap.CODEC).getOrThrow().getFirst();
        blockManager = new BlockManager(this);
        database = new DebugDatabase();
        animationContext = new AnimationContextImpl(this);
        //todo register custom animations
        animationLoader = new AnimationLoader(this);
        animationLoader.load();
    }

    @Override
    public void onDisable() {
        IS_ENABLED_SETTER.accept(this, true);
        try {
            onDisable0();
        } finally {
            IS_ENABLED_SETTER.accept(this, false);
        }
    }

    private void onDisable0() {
        blockManager.close();
        try {
            database.close();
        } catch (IOException e) {
            getSLF4JLogger().error("Failed to close database", e);
        }
    }

    public PrizeMap prizeMap() {
        return prizeMap;
    }

    public BlockManager blockManager() {
        return blockManager;
    }

    public Database database() {
        return database;
    }

    public AnimationContext animationContext() {
        return animationContext;
    }

    public AnimationLoader animationLoader() {
        return animationLoader;
    }

    static {
        try {
            Field field = JavaPlugin.class.getDeclaredField("isEnabled");
            field.setAccessible(true);
            IS_ENABLED_SETTER = LambdaMetafactoryUtil.setterOf(field);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
