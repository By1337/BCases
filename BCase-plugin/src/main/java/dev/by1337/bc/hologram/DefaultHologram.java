package dev.by1337.bc.hologram;

import dev.by1337.virtualentity.api.virtual.ViewTracker;
import org.bukkit.entity.Player;
import org.by1337.blib.geom.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DefaultHologram implements ViewTracker {
    private Vec3d position;
    private List<String> lines;
    private final double lineSpacing;
    private final List<HologramLine> hologramLines;
    private final int updatePeriod;
    private int tick;

    public DefaultHologram(Vec3d position, double lineSpacing, List<String> lines, int updatePeriod) {
        this.lines = lines;
        this.position = position;
        this.lineSpacing = lineSpacing;

        this.updatePeriod = updatePeriod;
        hologramLines = new ArrayList<>();
        recreateHologramLines();
    }

    private void recreateHologramLines() {
        hologramLines.forEach(line -> line.tick(Set.of()));
        int x = 0;
        for (String line : lines) {
            hologramLines.add(new HologramLine(position.add(0, -(lineSpacing * x++), 0), line));
        }
    }

    private void updatePosition() {
        int x = 0;
        for (HologramLine hologramLine : hologramLines) {
            hologramLine.setPos(position.add(0, -(lineSpacing * x++), 0));
        }
    }

    public void setPosition(Vec3d position) {
        this.position = position;
        updatePosition();
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
        recreateHologramLines();
    }

    @Override
    public void tick(Set<Player> viewers) {
        if (tick++ % updatePeriod == 0) {
            for (int i = 0; i < lines.size(); i++) {
                hologramLines.get(i).setText(lines.get(i));
            }
        }
        hologramLines.forEach(line -> line.tick(viewers));
    }
}
