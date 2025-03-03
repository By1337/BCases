package dev.by1337.bc.hologram;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.by1337.bc.world.WorldGetter;
import dev.by1337.virtualentity.api.util.PlayerHashSet;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.geom.Vec3i;

import java.io.Closeable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HologramManager implements Closeable {
    private final DefaultHologram hologram;
    private final WorldGetter worldGetter;
    private volatile boolean hided = false;
    private final BukkitTask bukkitTask;
    private final Vec3d pos;
    private final Lock lock = new ReentrantLock();
    private final Config config;

    public HologramManager(final WorldGetter worldGetter, Vec3i pos, Plugin plugin, Config config) {
        this.worldGetter = worldGetter;
        this.pos = new Vec3d(pos);
        hologram = new DefaultHologram(
                new Vec3d(pos).add(config.offsets),
                0.3,
                config.lines,
                5
        );
        bukkitTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::tick, 20, 20);

        this.config = config;
    }

    private void tick() {
        lock.lock();
        try {
            if (hided) return;
            World world = worldGetter.world();
            if (world == null) return;
            Set<Player> actualViewers = new PlayerHashSet();
            for (Player player : world.getPlayers()) {
                Location loc = player.getLocation();
                if (pos.distanceSquared(new Vec3d(loc.getX(), loc.getY(), loc.getZ())) <= 30 * 30) {
                    actualViewers.add(player);
                }
            }
            hologram.tick(actualViewers);
        } finally {
            lock.unlock();
        }
    }

    public void hide() {
        lock.lock();
        try {
            hided = true;
            hologram.tick(Set.of());
        } finally {
            lock.unlock();
        }
    }

    public void show() {
        lock.lock();
        try {
            hided = false;
            tick();
        } finally {
            lock.unlock();
        }
    }

    public Config getConfig() {
        return config;
    }

    @Override
    public void close() {
        bukkitTask.cancel();
        hologram.tick(Set.of());
    }

    public record Config(Vec3d offsets, List<String> lines) {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Vec3d.CODEC.fieldOf("offsets").forGetter(Config::offsets),
                Codec.STRING.listOf().fieldOf("lines").forGetter(Config::lines)
        ).apply(instance, Config::new));
    }
}

