package dev.by1337.bc;

import dev.by1337.bc.prize.PrizeMap;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.util.ResourceUtil;

public class BCase extends JavaPlugin {
    private PrizeMap prizeMap;
    private BlockManager blockManager;

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {
        prizeMap = ResourceUtil.load("prizes.yml", this).get().decode(PrizeMap.CODEC).getOrThrow().getFirst();
        blockManager = new BlockManager(this);
    }

    @Override
    public void onDisable() {
        blockManager.close();
    }
}
