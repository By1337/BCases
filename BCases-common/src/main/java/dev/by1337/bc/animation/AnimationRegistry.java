package dev.by1337.bc.animation;

import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.animation.impl.CreepersAnim;
import dev.by1337.bc.animation.impl.RandMobs;
import dev.by1337.bc.animation.impl.SwordsAnim;
import dev.by1337.bc.prize.PrizeSelector;
import dev.by1337.bc.yaml.CashedYamlContext;
import org.bukkit.entity.Player;
import org.by1337.blib.util.SpacedNameKey;
import org.by1337.blib.util.collection.SpacedNameRegistry;
import org.jetbrains.annotations.Nullable;

public class AnimationRegistry extends SpacedNameRegistry<AnimationRegistry.AnimationCreator> {
    public static final AnimationRegistry INSTANCE = new AnimationRegistry();
    public static AnimationCreator RAND_MOBS = INSTANCE.register("default:rand_mobs", RandMobs::new);
    public static AnimationCreator SWORDS = INSTANCE.register("default:swords", SwordsAnim::new);
    public static AnimationCreator CREEPERS = INSTANCE.register("default:creepers", CreepersAnim::new);

    private AnimationRegistry() {
    }

    public AnimationCreator register(String id, AnimationCreator creator) {
        put(new SpacedNameKey(id), creator);
        return creator;
    }

    @Nullable
    public AnimationCreator unregister(SpacedNameKey id) {
        return remove(id);
    }

    public AnimationCreator lookup(String id) {
        return find(SpacedNameKey.fromString(id, "default"));
    }

    @FunctionalInterface
    public interface AnimationCreator {
        Animation create(CaseBlock caseBlock, AnimationContext context, Runnable onEndCallback, PrizeSelector prizeSelector, CashedYamlContext config, Player player);
    }
}
