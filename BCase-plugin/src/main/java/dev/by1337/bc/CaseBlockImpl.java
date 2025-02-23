package dev.by1337.bc;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.by1337.bc.world.WorldGetter;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.PluginClassLoader;
import org.by1337.blib.configuration.serialization.BukkitCodecs;
import org.by1337.blib.geom.Vec3i;

import java.io.Closeable;

public class CaseBlockImpl implements CaseBlock, Closeable {
    public static final Codec<CaseBlockImpl> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BukkitCodecs.BLOCK_DATA.fieldOf("block").forGetter(CaseBlockImpl::block),
            WorldGetter.CODEC.fieldOf("world").forGetter(CaseBlockImpl::worldGetter),
            Vec3i.CODEC.fieldOf("pos").forGetter(CaseBlockImpl::pos)
    ).apply(instance, CaseBlockImpl::new));

    private final BlockData block;
    private final WorldGetter worldGetter;
    private final Vec3i pos;
    private final BCase plugin;

    public CaseBlockImpl(BlockData block, WorldGetter worldGetter, Vec3i pos) {
        this.block = block;
        this.worldGetter = worldGetter;
        this.pos = pos;
        plugin = (BCase) ((PluginClassLoader) this.getClass().getClassLoader()).getPlugin();
    }
    public void onClick(Player player) {
        System.out.println(player + " Clicked at block " + pos);
    }

    public void onWorldLoad() {
        showBlock();
        showHologram();
    }
    public void onWorldUnload() {
        destroyHologram();
        hideBlock();
    }

    @Override
    public Vec3i pos() {
        return pos;
    }

    @Override
    public WorldGetter worldGetter() {
        return worldGetter;
    }

    @Override
    public void hideBlock() {
        pos.toBlock(worldGetter.world()).setType(Material.AIR, false);
    }

    @Override
    public void showBlock() {
        pos.toBlock(worldGetter.world()).setBlockData(block, false);
    }

    @Override
    public void hideHologram() {

    }

    public void destroyHologram() {
        hideHologram(); // todo
    }

    @Override
    public void showHologram() {
        //todo должно работать даже после #destroyHologram
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    public BlockData block() {
        return block;
    }

    @Override
    public void close() {
        hideBlock();
        destroyHologram();
    }
}
