package dev.by1337.bc.event;

import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.prize.Prize;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PrizeGiveEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Prize prize;
    private final CaseBlock caseBlock;

    public PrizeGiveEvent(@NotNull Player who, @NotNull Prize prize, @NotNull CaseBlock caseBlock) {
        super(who);
        this.prize = prize;
        this.caseBlock = caseBlock;
    }

    public @NotNull CaseBlock getCaseBlock() {
        return caseBlock;
    }

    public @NotNull Prize getPrize() {
        return prize;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
