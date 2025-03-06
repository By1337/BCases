package dev.by1337.bc.util;

import dev.by1337.bc.BlockManager;
import dev.by1337.bc.CaseBlockImpl;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Nullable;

public class LookingAtCaseBlockUtil {
    private final BlockManager blockManager;

    public LookingAtCaseBlockUtil(BlockManager blockManager) {
        this.blockManager = blockManager;
    }

    @Nullable
    public CaseBlockImpl getLookingAtCaseBlock(Player player) {
        Block block = getBlock(player);
        if (block == null) return null;
        return blockManager.getBlock(block.getLocation());
    }


    @Nullable
    private Block getBlock(Player player) {
        RayTraceResult result = player.rayTraceBlocks(10);
        if (result == null) return null;
        return result.getHitBlock() == null ? null : result.getHitBlock();
    }
}
