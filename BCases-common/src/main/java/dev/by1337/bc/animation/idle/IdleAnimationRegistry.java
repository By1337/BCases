package dev.by1337.bc.animation.idle;

import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.animation.idle.impl.FireworksSparkIdleAnim;
import org.by1337.blib.util.SpacedNameKey;
import org.by1337.blib.util.collection.SpacedNameRegistry;
import org.jetbrains.annotations.Nullable;

public class IdleAnimationRegistry extends SpacedNameRegistry<IdleAnimationRegistry.IdleAnimationCreator> {
    public static final IdleAnimationRegistry INSTANCE = new IdleAnimationRegistry();
    public static final IdleAnimationCreator NONE = INSTANCE.register("default:none", EmptyIdleAnimation::new);
    public static final IdleAnimationCreator FIREWORKS = INSTANCE.register("default:fireworks", FireworksSparkIdleAnim::new);


    public IdleAnimationCreator register(String id, IdleAnimationCreator creator) {
        put(new SpacedNameKey(id), creator);
        return creator;
    }

    @Nullable
    public IdleAnimationCreator unregister(SpacedNameKey id) {
        return remove(id);
    }

    public IdleAnimationCreator lookup(String id) {
        return find(SpacedNameKey.fromString(id, "default"));
    }

    @FunctionalInterface
    public interface IdleAnimationCreator {
        IdleAnimation create(CaseBlock caseBlock);
    }
}
