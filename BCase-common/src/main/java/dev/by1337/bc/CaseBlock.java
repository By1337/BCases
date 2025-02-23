package dev.by1337.bc;

import dev.by1337.bc.world.WorldGetter;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.geom.Vec3i;

public interface CaseBlock {
    Vec3i pos();
    WorldGetter worldGetter();
    void hideBlock();
    void showBlock();
    void hideHologram();
    void showHologram();
    Plugin plugin();
}
