package dev.by1337.bc.command;

import dev.by1337.bc.CaseBlock;
import org.bukkit.entity.Player;

public record CommandContext(CaseBlock caseBlock, Player player) {
}
