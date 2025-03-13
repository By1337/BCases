package dev.by1337.bc.animation.impl;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.animation.AbstractAnimation;
import dev.by1337.bc.animation.AnimationContext;
import dev.by1337.bc.engine.MoveEngine;
import dev.by1337.bc.prize.Prize;
import dev.by1337.bc.prize.PrizeSelector;
import dev.by1337.bc.task.AsyncTask;
import dev.by1337.bc.yaml.CashedYamlContext;
import dev.by1337.virtualentity.api.entity.EquipmentSlot;
import dev.by1337.virtualentity.api.virtual.VirtualEntity;
import dev.by1337.virtualentity.api.virtual.decoration.VirtualArmorStand;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Lidded;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.by1337.blib.configuration.serialization.DefaultCodecs;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.geom.Vec3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class WheelAnim extends AbstractAnimation {
    private final Prize winner;
    private final Config config;
    private volatile boolean prizeGiven;

    public WheelAnim(CaseBlock caseBlock, AnimationContext context, Runnable onEndCallback, PrizeSelector prizeSelector, CashedYamlContext config, Player player) {
        super(caseBlock, context, onEndCallback, prizeSelector, config, player);
        winner = prizeSelector.getRandomPrize();
        this.config = config.get("settings", v -> v.decode(Config.CODEC).getOrThrow().getFirst(), new Config());
    }


    @Override
    protected void onStart() {
        caseBlock.hideHologram();
    }

    @Override
    protected void animate() throws InterruptedException {
        List<WheelItemData> prizes = new ArrayList<>();
        WheelItemData winnerData = null;
        int offsetDegrees = 360 / config.prizeCount;


        List<AsyncTask> moveTask = new ArrayList<>();

        Vec3d wheelCenter = center.add(config.offsets);

        int degrees = fixDegrees(config.topDegrees + offsetDegrees);

        openChest();
        var particleTask = new AsyncTask() {
            final Vec3d pos = center.add(0, 0.8, 0);
            @Override
            public void run() {
                spawnParticle(Particle.SPELL_WITCH, pos, 10, 0.15, 0.2, 0.15);
            }
        }.timer().delay(2).start(this);

        for (int ignored = config.prizeCount; ignored > 0; ignored--) {
            VirtualArmorStand armorStand = VirtualArmorStand.create();
            armorStand.setSmall(true);
            armorStand.setNoBasePlate(true);
            armorStand.setNoGravity(true);
            armorStand.setInvisible(true);

            armorStand.setCustomNameVisible(true);

            WheelItemData data;
            if (winnerData == null && (prizes.size() == config.prizeCount - 1 || RANDOM.nextBoolean())) {
                winnerData = data = new WheelItemData(armorStand, winner, degrees);
            } else {
                data = new WheelItemData(armorStand, prizeSelector.getRandomPrize(config.fakePrizeExponent), degrees);
            }
            armorStand.setEquipment(EquipmentSlot.HEAD, data.prize.itemStack());

            armorStand.setCustomName(data.prize.displayNameComponent());
            armorStand.setPos(wheelCenter);

            trackEntity(armorStand);
            playSound(Sound.ITEM_ARMOR_EQUIP_GENERIC, 1, 1);

            prizes.add(data);

            var v = createRotateTask(armorStand, wheelCenter, config.offset, config.topDegrees, degrees, -5);
            moveTask.add(v);
            MoveEngine.goTo(armorStand, wheelCenter.add(config.rotateAround.apply(config.offset, Math.toRadians(config.topDegrees))), 1.5)
                    .onEnd(() -> v.start(this))
                    .start(this)
            ;
            sleepTicks(10);

            degrees = fixDegrees(degrees + offsetDegrees);
        }
        AsyncTask.joinAll(moveTask);

        particleTask.cancel();
        closeChest();

        VirtualArmorStand arrow = VirtualArmorStand.create();
        arrow.setSmall(true);
        arrow.setNoBasePlate(true);
        arrow.setNoGravity(true);
        arrow.setInvisible(true);
        arrow.setMarker(true);

        if (config.type == RotateType.Z) {
            arrow.setHeadPose(new Vec3f(0, 0, -45));
            arrow.setEquipment(EquipmentSlot.HEAD, new ItemStack(Material.DIAMOND_SWORD));
            arrow.setPos(center.add(0.38, 0.1, 0.2));
        } else {
            arrow.setRightArmPose(new Vec3f(260, 0, 0));
            arrow.setEquipment(EquipmentSlot.MAINHAND, new ItemStack(Material.DIAMOND_SWORD));
            arrow.setPos(center.add(0.2, 0.3, -0.25));
        }

        trackEntity(arrow);

        double speed = config.maxSpeed;
        while (speed > config.minSpeed) {
            int speedCel = (int) speed;
            for (WheelItemData prize : prizes) {
                var d = fixDegrees(prize.degrees + speedCel);
                if ((prize.degrees < config.topDegrees && d >= config.topDegrees) ||
                        (prize.degrees > config.topDegrees && d <= config.topDegrees)) {
                    playSound(Sound.UI_BUTTON_CLICK, 1, 1);
                }
                prize.degrees = d;
                prize.entity.setPos(wheelCenter.add(config.rotateAround.apply(config.offset, Math.toRadians(prize.degrees))));
            }
            speed -= config.speedFade;
            sleepTicks(1);
        }

        while (Math.abs(winnerData.degrees - config.topDegrees) > config.minSpeed) {
            for (WheelItemData prize : prizes) {
                var d = fixDegrees(prize.degrees + config.minSpeed);
                if ((prize.degrees < config.topDegrees && d >= config.topDegrees) ||
                        (prize.degrees > config.topDegrees && d <= config.topDegrees)) {
                    playSound(Sound.UI_BUTTON_CLICK, 1, 1);
                }
                prize.degrees = d;
                prize.entity.setPos(wheelCenter.add(config.rotateAround.apply(config.offset, Math.toRadians(prize.degrees))));
            }
            sleepTicks(1);
        }
        removeEntity(arrow);
        sync(() -> {
            prizeGiven = true;
            caseBlock.givePrize(winner, player);
        }).start();

        sleep(2000);

        openChest();
        while (!prizes.isEmpty()) {
            var iterator = prizes.iterator();
            while (iterator.hasNext()) {
                WheelItemData prize = iterator.next();
                prize.degrees = fixDegrees(prize.degrees + config.removeSpeed);
                prize.entity.setPos(wheelCenter.add(config.rotateAround.apply(config.offset, Math.toRadians(prize.degrees))));

                if (Math.abs(prize.degrees - config.topDegrees) <= config.removeSpeed) {
                    iterator.remove();
                    MoveEngine.goTo(prize.entity, center, 1.5).onEnd(() -> {
                        removeEntity(prize.entity);
                        playSound(Sound.ENTITY_CHICKEN_EGG, 1, 1);
                    }).start(this);

                }
            }
            sleepTicks(1);
        }
        sleep(2000);
        closeChest();
    }

    private void closeChest() {
        modifyLidded((block, lidded) -> {
            lidded.close();
            if (block.getType() == Material.CHEST) {
                playSound(Sound.BLOCK_CHEST_CLOSE, 1, 1);
            } else {
                playSound(Sound.BLOCK_ENDER_CHEST_CLOSE, 1, 1);
            }
        });
    }

    private void openChest() {
        modifyLidded((block, lidded) -> {
            lidded.open();
            if (block.getType() == Material.CHEST) {
                playSound(Sound.BLOCK_CHEST_OPEN, 1, 1);
            } else {
                playSound(Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
            }
        });
    }

    private void modifyLidded(BiConsumer<Block, Lidded> consumer) {
        sync(() -> {
            Block block = blockPos.toBlock(world);
            var state = block.getState();
            if (state instanceof Lidded lidded) {
                consumer.accept(block, lidded);
                state.update();
            }
        }).start();
    }

    @Override
    protected void onEnd() {
        caseBlock.showHologram();
        if (!prizeGiven) caseBlock.givePrize(winner, player);
    }

    @Override
    protected void onClick(VirtualEntity entity, Player clicker) {

    }

    @Override
    public void onInteract(PlayerInteractEvent event) {

    }

    private AsyncTask createRotateTask(VirtualEntity entity, Vec3d center, Vec3d offset, int currentDegrees, int degrees, int speed) {
        return new AsyncTask() {
            private double current = currentDegrees;

            @Override
            public void run() {
                double nextStep = fixDegrees(current + speed);
                if (Math.abs(Math.abs(nextStep) - Math.abs(degrees)) <= Math.abs(speed)) {
                    current = degrees;
                }
                entity.setPos(center.add(config.rotateAround.apply(offset, Math.toRadians(current))));
                if (current == degrees) {
                    cancel();
                }
                current = nextStep;
            }
        }.timer().delay(1);
    }


    private static int fixDegrees(double d) {
        d = d % 360;
        if (d < 0) {
            d += 360;
        }
        int res = (int) d;
        return res == 360 ? 0 : res;
    }


    private static class Config {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("prize_count").forGetter(v -> v.prizeCount),
                Codec.DOUBLE.fieldOf("radius").forGetter(v -> v.radius),
                Vec3d.CODEC.fieldOf("offsets").forGetter(v -> v.offsets),
                Codec.INT.fieldOf("max_speed").forGetter(v -> v.maxSpeed),
                Codec.INT.fieldOf("min_speed").forGetter(v -> v.minSpeed),
                Codec.INT.fieldOf("remove_speed").forGetter(v -> v.removeSpeed),
                Codec.DOUBLE.fieldOf("speed_fade").forGetter(v -> v.speedFade),
                Codec.DOUBLE.fieldOf("fake_prize_exponent").forGetter(v -> v.fakePrizeExponent),
                RotateType.CODEC.fieldOf("type").forGetter(v -> v.type)
        ).apply(instance, Config::new));

        private final int prizeCount;
        private final double radius;
        private final Vec3d offsets;
        private final int maxSpeed;
        private final int minSpeed;
        private final int removeSpeed;
        private final double speedFade;
        private final double fakePrizeExponent;
        private final BiFunction<Vec3d, Double, Vec3d> rotateAround;
        private final Vec3d offset;
        private final int topDegrees;
        private final RotateType type;

        public Config() {
            prizeCount = 8;
            radius = 1.5;
            offsets = new Vec3d(0, -0.5, 0);
            maxSpeed = 15;
            minSpeed = 2;
            removeSpeed = 5;
            speedFade = 0.07;
            fakePrizeExponent = 0.5;
            rotateAround = Vec3d::rotateAroundZ;
            offset = new Vec3d(radius, 0, 0);
            topDegrees = 90;
            type = RotateType.Z;
        }

        public Config(int prizeCount, double radius, Vec3d offsets, int maxSpeed, int minSpeed, int removeSpeed, double speedFade, double fakePrizeExponent, RotateType type) {
            this.prizeCount = prizeCount;
            this.radius = radius;
            this.offsets = offsets;
            this.maxSpeed = maxSpeed;
            this.minSpeed = minSpeed;
            this.removeSpeed = removeSpeed;
            this.speedFade = speedFade;
            this.fakePrizeExponent = fakePrizeExponent;
            this.type = type;
            if (type == RotateType.Z) {
                rotateAround = Vec3d::rotateAroundZ;
                offset = new Vec3d(radius, 0, 0);
                topDegrees = 90;
            } else {
                rotateAround = Vec3d::rotateAroundX;
                offset = new Vec3d(0, 0, radius);
                topDegrees = 270;
            }
        }
    }

    private enum RotateType {
        X,
        Z;
        public static final Codec<RotateType> CODEC = DefaultCodecs.createEnumCodec(RotateType.class);
    }

    private static class WheelItemData {
        private final VirtualArmorStand entity;
        private final Prize prize;
        private int degrees;

        public WheelItemData(VirtualArmorStand entity, Prize prize, int degrees) {
            this.entity = entity;
            this.prize = prize;
            this.degrees = degrees;
        }
    }
}
