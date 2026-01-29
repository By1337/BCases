package dev.by1337.bc.animation.impl;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.animation.AbstractAnimation;
import dev.by1337.bc.animation.AnimationContext;
import dev.by1337.bc.annotations.AsyncOnly;
import dev.by1337.bc.annotations.SyncOnly;
import dev.by1337.bc.engine.MoveEngine;
import dev.by1337.bc.particle.ParticleUtil;
import dev.by1337.bc.prize.Prize;
import dev.by1337.bc.prize.PrizeSelector;
import dev.by1337.bc.task.AsyncTask;
import dev.by1337.bc.world.WorldEditor;
import dev.by1337.bc.yaml.CashedYamlContext;
import dev.by1337.virtualentity.api.entity.EquipmentSlot;
import dev.by1337.virtualentity.api.virtual.VirtualEntity;
import dev.by1337.virtualentity.api.virtual.decoration.VirtualArmorStand;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.geom.Vec3f;
import org.by1337.blib.geom.Vec3i;

import java.util.*;

public class SwordsAnim extends AbstractAnimation {
    private final Prize winner;
    private final Set<Vec3i> stones = new HashSet<>();
    private final Map<Vec3i, VirtualArmorStand> stoneToSword = new HashMap<>();
    private volatile Vec3i selectedStone;
    private WorldEditor worldEditor;
    private volatile boolean waitClick;
    private final Config config;

    public SwordsAnim(CaseBlock caseBlock, AnimationContext context, Runnable onEndCallback, PrizeSelector prizeSelector, CashedYamlContext config, Player player) {
        super(caseBlock, context, onEndCallback, prizeSelector, config, player);
        winner = prizeSelector.getRandomPrize();
        this.config = config.get("settings", v -> v.decode(Config.CODEC).getOrThrow().getFirst());
    }

    @Override
    @SyncOnly
    protected void onStart() {
        caseBlock.hideHologram();
        worldEditor = new WorldEditor(world);
    }

    @Override
    @AsyncOnly
    protected void animate() throws InterruptedException {
        for (Vec3i spawnPoint : config.blockPositions) {
            var pos = blockPos.add(spawnPoint);
            worldEditor.setType(pos, Material.ANDESITE);
            stones.add(pos);
            playSound(location, Sound.BLOCK_STONE_PLACE, 1, 1);
            sleepTicks(3);
        }
        for (Vec3i dest : config.blockPositions) {
            spawnSword(blockPos.add(dest));
            sleepTicks(4);
        }
        sleepTicks(30);
        sendTitle("", config.title, 5, 30, 10);
        waitClick = true;
        waitUpdate(10_000);
        if (selectedStone == null) {
            selectedStone = stones.iterator().next();
        }
        var sword = stoneToSword.get(selectedStone);
        playSound(selectedStone, Sound.BLOCK_ANVIL_DESTROY, 0.8F, 0.8F);
        var loc = selectedStone.toVec3d().add(0.5, 1.1, 0.5);
        for (int i = 0; i < 2; i++) {
            sword.setPos(sword.getPos().add(0, -0.1, 0));
            spawnParticle(Particle.BLOCK_CRACK, loc, 15, Material.COBBLESTONE.createBlockData());
            sleepTicks(10);
        }
        worldEditor.setType(selectedStone, Material.AIR);
        trackEntity(winner.createVirtualItem(selectedStone.toVec3d().add(0.5, 0.3, 0.5)));
        removeEntity(sword);
        spawnParticle(Particle.BLOCK_CRACK, selectedStone.x + 0.5, selectedStone.y + 0.5, selectedStone.z + 0.5, 60, Material.CRACKED_STONE_BRICKS.createBlockData());

        new AsyncTask() {
            final Vec3d pos = selectedStone.toVec3d().add(0.5, 0, 0.5);

            @Override
            public void run() {
                ParticleUtil.spawnBlockOutlining(pos, SwordsAnim.this, Particle.FLAME, 0.1);
            }
        }.timer().delay(6).start(this);

        sleepTicks(40);
        for (Vec3i spawnPoint : config.blockPositions) {
            Vec3i pos = blockPos.add(spawnPoint);
            if (pos.equals(selectedStone)) continue;
            worldEditor.setType(pos, Material.AIR);
            var blockCenter = pos.toVec3d().add(0.5, 0.5, 0.5);
            spawnParticle(Particle.BLOCK_CRACK, blockCenter, 30, Material.COBBLESTONE.createBlockData());
            playSound(blockCenter, Sound.BLOCK_STONE_BREAK, 1, 1);
            trackEntity(prizeSelector.getRandomPrize().createVirtualItem(pos.toVec3d().add(0.5, 0.3, 0.5)));
        }
        stoneToSword.values().forEach(this::removeEntity);
        sleepTicks(3 * 20);
    }

    private void spawnSword(Vec3i dest) {
        VirtualArmorStand armorStand = createSword();
        trackEntity(armorStand);

        stoneToSword.put(dest, armorStand);
        Vec3d destPos = new Vec3d(dest).add(0.9, 0.9, 0.3);
        playSound(Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1);
        Vec3d startPos = armorStand.getPos();
        spawnParticle(Particle.SPELL_WITCH, startPos.x, startPos.y + 1.3, startPos.z, 30, 0, 0, 0, 0.01);
        spawnParticle(Particle.REVERSE_PORTAL, startPos.x, startPos.y + 1.3, startPos.z, 30, 0, 0, 0, 0.1);
        MoveEngine.goToParabola(
                        armorStand,
                        destPos,
                        2,
                        5
                )
                .onEnd(() -> {
                    playSound(destPos, Sound.ITEM_TRIDENT_HIT_GROUND, 1, 1);
                    playSound(destPos, Sound.ITEM_TRIDENT_RETURN, 10, 10);
                    Vec3d pos = destPos.add(0.5, 1, 0.5);
                    spawnParticle(Particle.CLOUD, pos.x, pos.y, pos.z, 3, 0, 0, 0, 0.1);
                    worldEditor.setType(dest, Material.COBBLESTONE);
                })
                .start(this);

    }

    private VirtualArmorStand createSword() {
        VirtualArmorStand armorStand = VirtualArmorStand.create();
        armorStand.setPos(center);
        armorStand.setNoBasePlate(true);
        armorStand.setSilent(true);
        armorStand.setMarker(true);
        armorStand.setSmall(true);
        armorStand.setInvisible(true);
        armorStand.setNoGravity(true);
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 2);
        sword.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        armorStand.setEquipment(EquipmentSlot.HEAD, sword);
        armorStand.setHeadPose(
                new Vec3f(
                        (float) Math.toDegrees(Math.toRadians(180)),
                        (float) Math.toDegrees(Math.toRadians(0)),
                        (float) Math.toDegrees(Math.toRadians(45))
                )
        );
        return armorStand;
    }

    @Override
    @SyncOnly
    protected void onEnd() {
        if (worldEditor != null) {
            worldEditor.close();
        }
        caseBlock.showHologram();
        caseBlock.givePrize(winner, player);
    }

    @Override
    @SyncOnly
    protected void onClick(VirtualEntity entity, Player clicker) {

    }

    @Override
    @SyncOnly
    public void onInteract(PlayerInteractEvent event) {
        if (!waitClick) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Vec3i pos = new Vec3i(block);
        if (!stoneToSword.containsKey(pos)) return;
        selectedStone = pos;
        waitClick = false;
        update();
    }

    private static class Config {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Vec3i.CODEC.listOf().fieldOf("block_positions").forGetter(v -> v.blockPositions),
                Codec.STRING.fieldOf("title").forGetter(v -> v.title)
        ).apply(instance, Config::new));
        private final List<Vec3i> blockPositions;
        private final String title;

        public Config(List<Vec3i> blockPositions, String title) {
            this.blockPositions = blockPositions;
            this.title = title;
        }
    }
}
