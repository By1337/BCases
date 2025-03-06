package dev.by1337.bc.animation.impl;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.animation.AbstractAnimation;
import dev.by1337.bc.animation.AnimationContext;
import dev.by1337.bc.annotations.AsyncOnly;
import dev.by1337.bc.annotations.SyncOnly;
import dev.by1337.bc.particle.ParticleUtil;
import dev.by1337.bc.prize.Prize;
import dev.by1337.bc.prize.PrizeSelector;
import dev.by1337.bc.task.AsyncTask;
import dev.by1337.bc.yaml.CashedYamlContext;
import dev.by1337.virtualentity.api.entity.EntityEvent;
import dev.by1337.virtualentity.api.virtual.VirtualEntity;
import dev.by1337.virtualentity.api.virtual.VirtualLivingEntity;
import dev.by1337.virtualentity.api.virtual.item.VirtualItem;
import dev.by1337.virtualentity.api.virtual.monster.VirtualCreeper;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.by1337.blib.geom.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class CreepersAnim extends AbstractAnimation {

    private final Config config;
    private volatile boolean waitClick;
    private volatile VirtualCreeper clickedEntity;
    private final Prize winner;

    public CreepersAnim(CaseBlock caseBlock, AnimationContext context, Runnable onEndCallback, PrizeSelector prizeSelector, CashedYamlContext config, Player player) {
        super(caseBlock, context, onEndCallback, prizeSelector, config, player);
        this.config = config.get("settings", v -> v.decode(Config.CODEC).getOrThrow().getFirst(), new Config());
        winner = prizeSelector.getRandomPrize();
    }

    @Override
    @SyncOnly
    protected void onStart() {
        caseBlock.hideBlock();
        caseBlock.hideHologram();
    }

    @Override
    @AsyncOnly
    protected void animate() throws InterruptedException {
        var lookTask = AsyncTask.create(() -> {
            tracker.forEach(value -> {
                if (value instanceof VirtualLivingEntity) {
                    value.lookAt(new Vec3d(player.getLocation()));
                }
            });
        }).timer().delay(1).start(this);
        for (Vec3d position : config.positions) {
            Vec3d pos = center.add(position);
            VirtualCreeper creeper = VirtualCreeper.create();
            creeper.setPos(pos);
            trackEntity(creeper);
            playSound(Sound.BLOCK_GILDED_BLACKSTONE_FALL, 1, 1);
            world.spawnParticle(Particle.CLOUD, pos.add(0, 0.3, 0).toLocation(world), 15, 0, 0, 0, 0.05);
            sleep(config.spawnDelay);
        }
        sendTitle("", config.title, 5, 30, 10);
        waitClick = true;
        waitUpdate(config.clickWait);
        waitClick = false;
        if (clickedEntity == null) {
            clickedEntity = (VirtualCreeper) tracker.getRandom();
        }

        clickedEntity.setIgnited(true);
        clickedEntity.setPowered(true);
        clickedEntity.broadcastEntityEvent(EntityEvent.HURT);
        playSound(Sound.ENTITY_CREEPER_PRIMED, 1, 1);
        sleepTicks(30);
        tracker.removeEntity(clickedEntity);

        world.playSound(clickedEntity.getPos().toLocation(world), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION_HUGE, clickedEntity.getPos().toLocation(world), 0);
        world.spawnParticle(Particle.CLOUD, clickedEntity.getPos().toLocation(world), 30, 0, 0, 0, 0.05);

        for (int i = 0; i < 35; i++) {
            Vec3d motion = new Vec3d(
                    (RANDOM.nextDouble() - 0.5) * 0.6,
                    (RANDOM.nextDouble() + 0.15) * 0.8,
                    (RANDOM.nextDouble() - 0.5) * 0.6
            );
            tracker.addEntity(
                    createVirtualItem(
                            new ItemStack(Material.GUNPOWDER),
                            clickedEntity.getPos().add(0, 1, 0),
                            motion
                    )
            );
        }

        world.spawnParticle(Particle.FLAME, clickedEntity.getPos().toLocation(world), 50, 0, 0, 0, 0.5);
        new AsyncTask() {
            final Vec3d pos = clickedEntity.getPos();

            @Override
            public void run() {
                ParticleUtil.spawnBlockOutlining(pos, world, Particle.FLAME, 0.1);
            }
        }.timer().delay(6).start(this);

        trackEntity(winner.createVirtualItem(clickedEntity.getPos()));

        sleepTicks(3 * 20);

        sleep(config.idle);
        lookTask.cancel();
        tracker.forEach(value -> {
            if (value instanceof VirtualLivingEntity livingEntity) {
                livingEntity.setHealth(0);
                livingEntity.broadcastEntityEvent(EntityEvent.DEATH);
                trackEntity(prizeSelector.getRandomPrize().createVirtualItem(livingEntity.getPos()));
            }
        });
        sleep(config.beforeEnd);
    }

    private VirtualItem createVirtualItem(ItemStack display, Vec3d pos, Vec3d motion) {
        VirtualItem item = VirtualItem.create();
        item.setPos(pos);
        item.setItem(display);
        item.setMotion(motion);
        return item;
    }

    @Override
    @SyncOnly
    protected void onEnd() {
        caseBlock.showBlock();
        caseBlock.showHologram();
        caseBlock.givePrize(winner, player);
    }

    @Override
    @SyncOnly
    protected void onClick(VirtualEntity entity, Player clicker) {
        if (!clicker.equals(player)) return;
        if (waitClick && entity instanceof VirtualCreeper livingEntity) {
            clickedEntity = livingEntity;
            update();
        }
    }

    @Override
    @SyncOnly
    public void onInteract(PlayerInteractEvent event) {

    }

    private static class Config {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Vec3d.CODEC.listOf().fieldOf("mobs_positions").forGetter(v -> v.positions),
                Codec.LONG.fieldOf("spawn_delay").forGetter(v -> v.spawnDelay),
                Codec.LONG.fieldOf("click_wait").forGetter(v -> v.clickWait),
                Codec.LONG.fieldOf("idle").forGetter(v -> v.idle),
                Codec.LONG.fieldOf("before_end").forGetter(v -> v.beforeEnd),
                Codec.STRING.fieldOf("title").forGetter(v -> v.title)
        ).apply(instance, Config::new));
        private final List<Vec3d> positions;
        private final long spawnDelay;
        private final long clickWait;
        private final long idle;
        private final long beforeEnd;
        private final String title;

        public Config(List<Vec3d> positions, long spawnDelay, long clickWait, long idle, long beforeEnd, String title) {
            this.positions = positions;
            this.spawnDelay = spawnDelay;
            this.clickWait = clickWait;
            this.idle = idle;
            this.beforeEnd = beforeEnd;
            this.title = title;
        }

        public Config() {
            positions = new ArrayList<>();
            positions.add(new Vec3d(1, 0.0, 3));
            positions.add(new Vec3d(-0, 0.0, 3));
            positions.add(new Vec3d(-2, 0.0, 1));
            positions.add(new Vec3d(-2, 0.0, -0));
            positions.add(new Vec3d(-0, 0.0, -2));
            positions.add(new Vec3d(1, 0.0, -2));
            positions.add(new Vec3d(3, 0.0, -0));
            positions.add(new Vec3d(3, 0.0, 1));
            spawnDelay = 150;
            clickWait = 10_000;
            idle = 3_000;
            beforeEnd = 4_000;
            title = "&cNONE";
        }
    }
}
