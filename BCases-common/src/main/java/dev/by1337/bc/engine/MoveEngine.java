package dev.by1337.bc.engine;

import dev.by1337.bc.task.AsyncTask;
import dev.by1337.virtualentity.api.virtual.VirtualEntity;
import org.by1337.blib.geom.Vec3d;

public class MoveEngine {

    public static AsyncTask goTo(VirtualEntity entity, Vec3d dest, double speed) {
        return new AsyncTask() {
            final Vec3d startPos = entity.getPos();
            final double distance = distanceBlocks(startPos, dest);
            final double steps = (distance / speed) * 20;
            final Vec3d delta = dest.sub(startPos).divide(steps);
            int step = 0;

            @Override
            public void run() {
                if (step++ < steps) {
                    entity.setPos(startPos.add(delta.mul(step)));
                } else {
                    cancel();
                }
            }
        }.timer().delay(1);

    }


    private static double distanceBlocks(Vec3d start, Vec3d end) {
        return Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2) + Math.pow(end.z - start.z, 2));
    }

    public static AsyncTask goToParabola(VirtualEntity entity, Vec3d dest, double speed, double height) {
        return new AsyncTask() {
            final Vec3d startPos = entity.getPos();
            final double distance = distanceBlocks(startPos, dest);
            final double steps = (distance / speed) * 20;
            final Vec3d delta = dest.sub(startPos).divide(steps);
            int step = 0;

            @Override
            public void run() {
                if (step++ < steps) {
                    double t = (double) step / steps;
                    entity.setPos(startPos.add(delta.mul(step)).add(0, (height * (1 - Math.pow(t - 0.5, 2) * 4)), 0));
                } else {
                    entity.setPos(startPos.add(delta.mul(step)));
                    cancel();
                }
            }
        }.timer().delay(1);
    }
}
