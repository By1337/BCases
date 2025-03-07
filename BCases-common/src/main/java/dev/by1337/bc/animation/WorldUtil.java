package dev.by1337.bc.animation;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.geom.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class WorldUtil {

    public abstract void forEachViewers(Consumer<Player> consumer);

    public abstract Location getCenter();

    public void playEffect(@NotNull Location location, @NotNull Effect effect, int data) {
        forEachViewers(player -> player.playEffect(location, effect, data));
    }

    public <T> void playEffect(@NotNull Location location, @NotNull Effect effect, @Nullable T data) {
        forEachViewers(player -> player.playEffect(location, effect, data));
    }

    public <T> void playEffect(@NotNull Effect effect, @Nullable T data) {
        playEffect(getCenter(), effect, data);
    }

    public void playSound(@NotNull Vec3d pos, @NotNull Sound sound, float volume, float pitch) {
        playSound(pos.toLocation(getCenter().getWorld()), sound, volume, pitch);
    }

    public void playSound(@NotNull Vec3i pos, @NotNull Sound sound, float volume, float pitch) {
        playSound(pos.toLocation(getCenter().getWorld()), sound, volume, pitch);
    }

    public void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch) {
        forEachViewers(player -> player.playSound(location, sound, volume, pitch));
    }

    public void playSound(@NotNull Sound sound, float volume, float pitch) {
        playSound(getCenter(), sound, volume, pitch);
    }

    public void playSound(@NotNull Location location, @NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch) {
        forEachViewers(player -> player.playSound(location, sound, category, volume, pitch));
    }

    public void playSound(@NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch) {
        playSound(getCenter(), sound, category, volume, pitch);
    }


    public void spawnParticle(Particle particle, Location location, int count) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count);
    }

    public void spawnParticle(Particle particle, Vec3d location, int count) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count);
    }

    public void spawnParticle(Particle particle, Vec3i location, int count) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count);
    }

    public void spawnParticle(Particle particle, double x, double y, double z, int count) {
        this.spawnParticle(particle, x, y, z, count, null);
    }

    public <T> void spawnParticle(Particle particle, Location location, int count, T data) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, data);
    }

    public <T> void spawnParticle(Particle particle, Vec3d location, int count, T data) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, data);
    }

    public <T> void spawnParticle(Particle particle, Vec3i location, int count, T data) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, data);
    }

    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, T data) {
        this.spawnParticle(particle, x, y, z, count, 0.0, 0.0, 0.0, data);
    }

    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ);
    }

    public void spawnParticle(Particle particle, Vec3d location, int count, double offsetX, double offsetY, double offsetZ) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ);
    }

    public void spawnParticle(Particle particle, Vec3i location, int count, double offsetX, double offsetY, double offsetZ) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ);
    }

    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ) {
        this.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, null);
    }

    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, T data) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, data);
    }

    public <T> void spawnParticle(Particle particle, Vec3d location, int count, double offsetX, double offsetY, double offsetZ, T data) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, data);
    }

    public <T> void spawnParticle(Particle particle, Vec3i location, int count, double offsetX, double offsetY, double offsetZ, T data) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, data);
    }

    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, T data) {
        this.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, 1.0, data);
    }

    public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, extra);
    }

    public void spawnParticle(Particle particle, Vec3d location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        this.spawnParticle(particle, location.x, location.y, location.z, count, offsetX, offsetY, offsetZ, extra);
    }

    public void spawnParticle(Particle particle, Vec3i location, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        this.spawnParticle(particle, location.x, location.y, location.z, count, offsetX, offsetY, offsetZ, extra);
    }

    public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        this.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, null);
    }

    public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
        this.spawnParticle(particle, location.getX(), location.getY(), location.getZ(), count, offsetX, offsetY, offsetZ, extra, data);
    }

    public <T> void spawnParticle(Particle particle, Vec3d location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
        this.spawnParticle(particle, location.x, location.y, location.z, count, offsetX, offsetY, offsetZ, extra, data);
    }

    public <T> void spawnParticle(Particle particle, Vec3i location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
        this.spawnParticle(particle, location.x, location.y, location.z, count, offsetX, offsetY, offsetZ, extra, data);
    }

    public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {
        forEachViewers(pl -> pl.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, data));
    }
}
