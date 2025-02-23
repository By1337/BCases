package dev.by1337.bc.animation;

import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.prize.PrizeSelector;
import dev.by1337.bc.yaml.CashedYamlContext;
import org.bukkit.entity.Player;

public class AnimationData {
    private final AnimationRegistry.AnimationCreator creator;
    private final String id;
    private final CashedYamlContext context;

    public AnimationData(AnimationRegistry.AnimationCreator creator, String id, CashedYamlContext context) {
        this.creator = creator;
        this.id = id;
        this.context = context;
    }

    public AnimationRegistry.AnimationCreator creator() {
        return creator;
    }

    public String id() {
        return id;
    }

    public CashedYamlContext context() {
        return context;
    }

    public Animation create(CaseBlock caseBlock, AnimationContext context, Runnable onEndCallback, PrizeSelector prizeSelector, Player player) {
        return creator.create(caseBlock, context, onEndCallback, prizeSelector, this.context, player);
    }

}
