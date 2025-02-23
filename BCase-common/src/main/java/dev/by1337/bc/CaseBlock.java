package dev.by1337.bc;

import dev.by1337.bc.annotations.SyncOnly;
import dev.by1337.bc.world.WorldGetter;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.geom.Vec3i;

public interface CaseBlock {
    Vec3i pos();

    WorldGetter worldGetter();

    @SyncOnly
    void hideBlock();

    @SyncOnly
    void showBlock();

    @SyncOnly
    void hideHologram();

    @SyncOnly
    void showHologram();

    Plugin plugin();
}
