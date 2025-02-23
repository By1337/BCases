package dev.by1337.bc.world;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.PrimitiveCodec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class WorldGetter {
    public static final Codec<WorldGetter> CODEC = Codec.STRING.xmap(WorldGetter::new, WorldGetter::worldName);

    private final String worldName;
    private transient World world;

    public WorldGetter(String worldName) {
        this.worldName = worldName;
    }

    public World world() {
        return world == null ? world = Bukkit.getWorld(worldName) : world;
    }

    public String worldName() {
        return worldName;
    }
}
