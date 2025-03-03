package dev.by1337.bc;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.by1337.bc.animation.Animation;
import dev.by1337.bc.animation.AnimationContext;
import dev.by1337.bc.animation.AnimationData;
import dev.by1337.bc.bd.Database;
import dev.by1337.bc.hologram.HologramManager;
import dev.by1337.bc.menu.CaseDefaultMenu;
import dev.by1337.bc.prize.Prize;
import dev.by1337.bc.prize.PrizeMap;
import dev.by1337.bc.prize.PrizeSelector;
import dev.by1337.bc.util.AsyncCatcher;
import dev.by1337.bc.world.WorldGetter;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.PluginClassLoader;
import org.by1337.blib.chat.placeholder.Placeholder;
import org.by1337.blib.configuration.serialization.BukkitCodecs;
import org.by1337.blib.geom.Vec3i;
import org.by1337.bmenu.Menu;
import org.by1337.bmenu.MenuLoader;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public class CaseBlockImpl extends Placeholder implements CaseBlock, Closeable {
    public static final Codec<CaseBlockImpl> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BukkitCodecs.BLOCK_DATA.fieldOf("block").forGetter(CaseBlockImpl::block),
            WorldGetter.CODEC.fieldOf("world").forGetter(CaseBlockImpl::worldGetter),
            Vec3i.CODEC.fieldOf("pos").forGetter(CaseBlockImpl::pos),
            Codec.STRING.fieldOf("on_click_menu").forGetter(CaseBlockImpl::onClickMenu),
            HologramManager.Config.CODEC.fieldOf("hologram").forGetter(v -> v.hologramManager.getConfig())
    ).apply(instance, CaseBlockImpl::new));

    private final BlockData block;
    private final WorldGetter worldGetter;
    private final Vec3i pos;
    @Deprecated
    private final BCase plugin;
    private final String onClickMenu;
    private volatile Animation animation;
    private final HologramManager hologramManager;

    public CaseBlockImpl(BlockData block, WorldGetter worldGetter, Vec3i pos, String onClickMenu, HologramManager.Config config) {
        this.block = block;
        this.worldGetter = worldGetter;
        this.pos = pos;
        this.onClickMenu = onClickMenu;
        plugin = (BCase) ((PluginClassLoader) this.getClass().getClassLoader()).getPlugin(); // todo не используй ебаные костыли
        registerPlaceholder("{playing}", () -> animation != null);
        hologramManager = new HologramManager(worldGetter, pos, plugin, config);
    }

    public void onClick(Player player) {
        if (animation != null) return;
        Menu menu = plugin.menuLoader().create(onClickMenu, player, null);
        if (menu instanceof CaseDefaultMenu cdm) {
            cdm.setCaseBlock(this);
        }
        menu.open();
    }

    @Override
    public void playAnimation(Player player, String animation, String prizes) {
        AsyncCatcher.catchOp("BlockCase#playAnimation");
        if (this.animation != null) throw new IllegalStateException("Animation already set");
        PrizeSelector prizeSelector = plugin.prizeMap().getPrizes(prizes);
        AnimationData animationData;
        if ("$random".equalsIgnoreCase(animation)) {
            animationData = plugin.animationLoader().getRandomAnimation();
        } else {
            animationData = plugin.animationLoader().getAnimation(animation);
        }
        this.animation = animationData.create(this, plugin.animationContext(), this::onAnimationStop, prizeSelector, player);
        this.animation.play();
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
        hideHologram();
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
        hologramManager.hide();
    }

    @Override
    public void showHologram() {
        hologramManager.show();
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

    @Override
    public Database getDatabase() {
        return plugin.database();
    }

    public BlockData block() {
        return block;
    }

    @Override
    public void close() {
        AsyncCatcher.catchOp("BlockCase#close");
        if (animation != null) {
            animation.forceStop();
        }
        hologramManager.close();
        hideBlock();
    }

    public String onClickMenu() {
        return onClickMenu;
    }

    @Override
    public PrizeMap prizeMap() {
        return plugin.prizeMap();
    }

    @Override
    public MenuLoader menuLoader() {
        return plugin.menuLoader();
    }

    @Override
    public AnimationContext animationContext() {
        return plugin.animationContext();
    }

    @Nullable
    public Animation animation() {
        return animation;
    }
}
