package dev.by1337.bc.animation;

import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.prize.PrizeSelector;
import dev.by1337.bc.yaml.CashedYamlContext;
import org.bukkit.entity.Player;
import org.by1337.blib.util.SpacedNameKey;
import org.by1337.blib.util.collection.SpacedNameRegistry;

public class AnimationRegistry extends SpacedNameRegistry<AnimationRegistry.AnimationCreator> {
    public static final AnimationRegistry INSTANCE = new AnimationRegistry();


    private AnimationRegistry() {
    }

    public AnimationCreator register(String id, AnimationCreator creator) {
        put(new SpacedNameKey(id), creator);
        return creator;
    }

    public AnimationCreator lookup(String id) {
        return find(SpacedNameKey.fromString(id, "default"));
    }

    @FunctionalInterface
    public interface AnimationCreator {
        Animation create(CaseBlock caseBlock, AnimationContext context, Runnable onEndCallback, PrizeSelector prizeSelector, CashedYamlContext config, Player player);
    }
}
