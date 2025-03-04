package dev.by1337.bc.tracker;

import dev.by1337.virtualentity.api.virtual.VirtualEntity;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.util.collection.IdentityHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ViewerTracker {
    private final World world;
    private final Vec3d center;
    private final int radiusSq;
    private final Map<Integer, VirtualEntity> entities = new ConcurrentHashMap<>();

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
        entities.values().forEach(e -> e.tick(actualViewers));
    }

    public int size(){
        return entities.size();
    }
    public Collection<VirtualEntity> values(){
        return entities.values();
    }

    public boolean hasEntity(int id) {
        return entities.containsKey(id);
    }

    @Nullable
    public VirtualEntity getEntity(int id) {
        return entities.get(id);
    }

    public void removeAll() {
        entities.values().forEach(e -> e.tick(Collections.emptySet()));
        entities.clear();
    }

    public void addEntity(final VirtualEntity entity) {
        entities.put(entity.getId(), entity);
    }

    public void removeEntity(final VirtualEntity entity) {
        entity.tick(Collections.emptySet());
        entities.remove(entity.getId());
    }

    public boolean isEmpty() {
        return entities.isEmpty();
    }
}
