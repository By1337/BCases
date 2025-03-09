package dev.by1337.bc.animation.idle.impl;

import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.animation.idle.IdleAnimation;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.by1337.blib.geom.Vec3d;

public class FireworksSparkIdleAnim implements IdleAnimation {
    private final CaseBlock caseBlock;

    private Task task;

    public FireworksSparkIdleAnim(CaseBlock caseBlock) {
        this.caseBlock = caseBlock;
    }

    @Override
    public void play() {
        if (task != null) return;
        task = new Task();
        task.runTaskTimerAsynchronously(caseBlock.plugin(), 0, 3);
    }

    @Override
    public void pause() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    private class Task extends BukkitRunnable {
        private final Vec3d center = new Vec3d(caseBlock.pos()).add(0.5, 1.05, 0.5);
        private int i;

        final int particleCount = 25;
        final double radius = 0.2;

        @Override
        public void run() {

            for (int t = 0; t < 1; t++) {
                double angle = 2 * Math.PI * i / particleCount;
                double angleCos = Math.cos(angle);
                double angleSin = Math.sin(angle);
                double x1 = center.x + radius * angleCos;
                double z1 = center.z + radius * angleSin;
                double x2 = center.x - radius * angleCos;
                double z2 = center.z - radius * angleSin;
                Vec3d pos1 = new Vec3d(x1, center.y, z1);
                Vec3d pos2 = new Vec3d(x2, center.y, z2);
                Vec3d dir1 = pos1.sub(center).normalize();
                Vec3d dir2 = pos2.sub(center).normalize();

                World world = caseBlock.worldGetter().world();

                world.spawnParticle(Particle.FIREWORKS_SPARK, pos1.x, pos1.y, pos1.z, 0, dir1.x, dir1.y, dir1.z, 0.3);
                world.spawnParticle(Particle.FIREWORKS_SPARK, pos2.x, pos2.y, pos2.z, 0, dir2.x, dir2.y, dir2.z, 0.3);
                i++;
                if (i > particleCount) {
                    i = 0;
                }
            }
        }
    }
}
