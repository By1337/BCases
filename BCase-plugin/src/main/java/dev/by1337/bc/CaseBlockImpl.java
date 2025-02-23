package dev.by1337.bc;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.by1337.bc.animation.Animation;
import dev.by1337.bc.animation.AnimationLoader;
import dev.by1337.bc.animation.impl.RandMobs;
import dev.by1337.bc.prize.Prize;
import dev.by1337.bc.util.AsyncCatcher;
import dev.by1337.bc.world.WorldGetter;
import dev.by1337.bc.yaml.CashedYamlContext;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.PluginClassLoader;
import org.by1337.blib.configuration.YamlContext;
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
    private volatile Animation animation;

    public CaseBlockImpl(BlockData block, WorldGetter worldGetter, Vec3i pos) {
        this.block = block;
        this.worldGetter = worldGetter;
        this.pos = pos;
        plugin = (BCase) ((PluginClassLoader) this.getClass().getClassLoader()).getPlugin();
    }

    public void onClick(Player player) {
        System.out.println(player + " Clicked at block " + pos);
        if (animation == null) {
            animation = plugin.animationLoader().getAnimation("random_mobs").create(
                    this,
                    plugin.animationContext(),
                    this::onAnimationStop,
                    plugin.prizeMap().getPrizes("default"),
                    player
            );
            animation.play();
        }
    }
    public boolean onUseUnknownEntity(int entityId, Player player) {
        if (animation == null) return false;
        return animation.onClick(entityId, player);
    }

    private void onAnimationStop() {
        AsyncCatcher.catchOp("BlockCase#onAnimationStop");
        animation = null;
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
    public void givePrize(Prize prize, Player player) {
        AsyncCatcher.catchOp("BlockCase#givePrize");
        for (String giveCommand : prize.giveCommands()) {
            System.out.println("Типо выдал игроку " + giveCommand);
        }
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
        AsyncCatcher.catchOp("BlockCase#close");
        if (animation != null){
            animation.forceStop();
        }
        destroyHologram();
        hideBlock();
    }
}
