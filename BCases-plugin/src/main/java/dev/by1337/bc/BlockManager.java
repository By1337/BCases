package dev.by1337.bc;

import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.by1337.blib.block.custom.registry.WorldRegistry;
import org.by1337.blib.util.ResourceUtil;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public class BlockManager implements Listener, Closeable {
    private final WorldRegistry<CaseBlockImpl> blocks = new WorldRegistry<>();
    private final BCases plugin;
    private final List<CaseBlockImpl> blockList = new ArrayList<>();
    private final NamespacedKey cooldownKey;

    public BlockManager(BCases plugin) {
        this.plugin = plugin;
        cooldownKey = new NamespacedKey(plugin, "cooldown");
        var list = ResourceUtil.load("blocks.yml", plugin).get("blocks").decode(CaseBlockImpl.CODEC.listOf()).getOrThrow().getFirst();
        blockList.addAll(list);
        for (CaseBlockImpl caseBlock : blockList) {
            var pos = caseBlock.pos();
            blocks.add(caseBlock.worldGetter().worldName(), pos.x, pos.y, pos.z, caseBlock);
            if (caseBlock.worldGetter().world() != null) {
                caseBlock.onWorldLoad();
            }
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void addBlock(CaseBlockImpl caseBlock) {
        blockList.add(caseBlock);
        var pos = caseBlock.pos();
        blocks.add(caseBlock.worldGetter().worldName(), pos.x, pos.y, pos.z, caseBlock);
        if (caseBlock.worldGetter().world() != null) {
            caseBlock.onWorldLoad();
        }
    }

    @Nullable
    public CaseBlockImpl removeBlock(CaseBlockImpl caseBlock) {
        var pos = caseBlock.pos();
        return removeBlock(caseBlock.worldGetter().worldName(), pos.x, pos.y, pos.z);
    }

    public CaseBlockImpl removeBlock(String worldName, int x, int y, int z) {
        CaseBlockImpl caseBlock = blocks.remove(worldName, x, y, z);
        if (caseBlock != null) {
            caseBlock.close();
            blockList.remove(caseBlock);
        }
        return caseBlock;
    }

    @EventHandler
    public void onUseUnknownEntity(PlayerUseUnknownEntityEvent event){
        int entityId = event.getEntityId();
        Player player = event.getPlayer();
        for (CaseBlockImpl caseBlock : blockList) {
            if (caseBlock.onUseUnknownEntity(entityId, player)) {
                return;
            }
        }
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent event) {
        var block = event.getClickedBlock();
        if (block == null) return;
        Player player = event.getPlayer();
        var clickedBlock = blocks.get(block.getLocation());
        if (clickedBlock == null) {
            for (CaseBlockImpl caseBlock : blockList) {
                var anim = caseBlock.animation();
                if (anim == null || !anim.getPlayer().equals(player)) continue;
                anim.onInteract(event);
                return;
            }
            return;
        }
        event.setCancelled(true);
        Long cd = player.getPersistentDataContainer().get(cooldownKey, PersistentDataType.LONG);
        if (cd != null && cd > System.currentTimeMillis()) {
            return;
        }
        player.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, System.currentTimeMillis() + 200);
        clickedBlock.onClick(player);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        for (CaseBlockImpl caseBlock : blocks.getAllInWorld(event.getWorld().getName())) {
            caseBlock.onWorldLoad();
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        for (CaseBlockImpl caseBlock : blocks.getAllInWorld(event.getWorld().getName())) {
            caseBlock.onWorldUnload();
        }
    }

    @Override
    public void close() {
        for (CaseBlockImpl caseBlock : blockList) {
            caseBlock.close();
        }
    }
}
