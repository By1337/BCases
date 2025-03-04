package dev.by1337.bc.particle;

import org.bukkit.Particle;
import org.bukkit.World;
import org.by1337.blib.geom.Vec3d;

public class ParticleUtil {
    public static void spawnBlockOutlining(Vec3d block, World world, Particle particle, double step) {
        for (double x = -0.5; x <= 0.5; x += step) {
            for (double z = -0.5; z <= 0.5; z += step) {
                if (x == -0.5 || x == 0.5 || z == -0.5 || z == 0.5) {
                    double x1 = block.x + x;
                    double z1 = block.z + z;
                    world.spawnParticle(particle,
                            x1,
                            block.y,
                            z1,
                            0
                    );
                }
            }
        }
    }
}
