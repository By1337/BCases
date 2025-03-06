package dev.by1337.bc.tracker;

import dev.by1337.virtualentity.api.virtual.VirtualEntity;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.util.collection.IdentityHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class ViewerTracker {
    private static final Random RANDOM = new Random();
    private final World world;
    private final Vec3d center;
    private final int radiusSq;
    private final Map<Integer, VirtualEntity> entities = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock();

    public ViewerTracker(World world, Vec3d center, int radius) {
        this.world = world;
        this.center = center;
        this.radiusSq = radius * radius;
    }

    public void tick() {
        Set<Player> actualViewers = new IdentityHashSet<>();
        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            if (center.distanceSquared(new Vec3d(loc.getX(), loc.getY(), loc.getZ())) <= radiusSq) {
                actualViewers.add(player);
            }
        }
        lock.lock();
        try {
            entities.values().forEach(e -> e.tick(actualViewers));
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return entities.size();
        } finally {
            lock.unlock();
        }
    }

    public void forEach(Consumer<? super VirtualEntity> consumer) {
        lock.lock();
        try {
            entities.values().forEach(consumer);
        } finally {
            lock.unlock();
        }
    }

    public VirtualEntity getRandom() {
        lock.lock();
        try {
            int index = RANDOM.nextInt(entities.size());
            var iterator = entities.entrySet().iterator();
            for (int i = 0; i < index; i++) {
                iterator.next();
            }
            return iterator.next().getValue();
        } finally {
            lock.unlock();
        }
    }

    public boolean hasEntity(int id) {
        lock.lock();
        try {
            return entities.containsKey(id);
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    public VirtualEntity getEntity(int id) {
        lock.lock();
        try {
            return entities.get(id);
        } finally {
            lock.unlock();
        }
    }

    public void removeAll() {
        lock.lock();
        try {
            entities.values().forEach(e -> e.tick(Set.of()));
            entities.clear();
        } finally {
            lock.unlock();
        }
    }

    public void addEntity(final VirtualEntity entity) {
        lock.lock();
        try {
            entities.put(entity.getId(), entity);
        } finally {
            lock.unlock();
        }
    }

    public void removeEntity(final VirtualEntity entity) {
        lock.lock();
        try {
            entity.tick(Collections.emptySet());
            entities.remove(entity.getId());
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return entities.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}
