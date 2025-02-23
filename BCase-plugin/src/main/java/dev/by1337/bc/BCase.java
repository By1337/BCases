package dev.by1337.bc;

import dev.by1337.bc.bd.Database;
import dev.by1337.bc.db.DebugDatabase;
import dev.by1337.bc.prize.PrizeMap;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.util.ResourceUtil;

import java.io.IOException;

public class BCase extends JavaPlugin {
    private PrizeMap prizeMap;
    private BlockManager blockManager;
    private Database database;

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {
        prizeMap = ResourceUtil.load("prizes.yml", this).get().decode(PrizeMap.CODEC).getOrThrow().getFirst();
        blockManager = new BlockManager(this);
        database = new DebugDatabase();
    }

    @Override
    public void onDisable() {
        blockManager.close();
        try {
            database.close();
        } catch (IOException e) {
            getSLF4JLogger().error("Failed to close database", e);
        }
    }
}
