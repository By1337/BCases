package dev.by1337.bc.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.by1337.blib.block.replacer.BlockReplaceFlags;
import org.by1337.blib.block.replacer.BlockReplaceStream;
import org.by1337.blib.geom.Vec3i;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

public class WorldEditor implements Closeable {
    private static final int REPLACE_FLAGS = BlockReplaceFlags.NO_PHYSICS + BlockReplaceFlags.UPDATE_SUPPRESS_DROPS;
    private final Map<Vec3i, BlockData> originalState = new HashMap<>();
    private final BlockReplaceStream stream;

    public WorldEditor(World world) {
        stream = new BlockReplaceStream();
        stream.setFlag(REPLACE_FLAGS);
        stream.setBlockPreReplaceCallBack(bl -> {
            var pos = new Vec3i(bl);
            if (!originalState.containsKey(pos)) {
                originalState.put(pos, bl.getBlockData());
            }
        });
        stream.start(world);
    }

    public void setType(Vec3i pos, Material material) {
        stream.addToReplace(pos, material);
    }

    @Override
    public void close() {
        originalState.forEach(stream::addToReplace);
        stream.setCancel(true);
    }
}
