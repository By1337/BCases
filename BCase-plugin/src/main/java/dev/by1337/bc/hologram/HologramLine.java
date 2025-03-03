package dev.by1337.bc.hologram;

import dev.by1337.virtualentity.api.virtual.decoration.VirtualArmorStand;
import org.bukkit.entity.Player;
import org.by1337.blib.BLib;
import org.by1337.blib.geom.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HologramLine {
    private Vec3d position;
    private String text;

    private final Map<UUID, Line> lines = new HashMap<>();

    public HologramLine(Vec3d position, String text) {
        this.position = position;
        this.text = text;
    }

    public Vec3d getPos() {
        return position;
    }

    public void setPos(Vec3d pos) {
        position = pos;
        lines.values().forEach(line -> line.armorStand.setPos(this.position));
    }

    public void setText(String text) {
        this.text = text;
        lines.values().forEach(line -> line.nextName = this.text);
    }

    public void tick(Set<Player> viewers) {
        Set<UUID> uuids = new HashSet<>();
        for (Player viewer : viewers) {
            UUID uuid = viewer.getUniqueId();
            Line line = lines.computeIfAbsent(uuid, k -> createLine(viewer));
            line.tick(viewer);
            uuids.add(uuid);
        }
        var iterator = lines.keySet().iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            if (!uuids.contains(uuid)) {
                Line line = lines.get(uuid);
                line.armorStand.tick(Set.of());
                iterator.remove();
            }
        }
    }

    private Line createLine(Player player) {
        VirtualArmorStand armorStand = VirtualArmorStand.create();
        armorStand.setPos(position);
        armorStand.setCustomNameVisible(true);
        armorStand.setCustomName(BLib.getApi().getMessage().componentBuilder(text, player));
        armorStand.setSmall(true);
        armorStand.setNoGravity(true);
        armorStand.setNoBasePlate(true);
        armorStand.setMarker(true);
        armorStand.setInvisible(true);
        armorStand.setSilent(true);
        return new Line(armorStand);
    }

    private static class Line {
        private final VirtualArmorStand armorStand;
        private @Nullable String nextName;

        public Line(VirtualArmorStand armorStand) {
            this.armorStand = armorStand;
        }

        public void tick(Player viewer) {
            if (nextName != null) {
                armorStand.setCustomName(BLib.getApi().getMessage().componentBuilder(nextName, viewer));
                nextName = null;
            }
            armorStand.tick(Set.of(viewer));
        }

    }
}
