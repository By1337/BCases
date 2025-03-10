package dev.by1337.bc.menu;

import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.command.CommandContext;
import dev.by1337.bc.command.CommandRegistry;
import org.bukkit.entity.Player;
import org.by1337.blib.command.CommandException;
import org.by1337.blib.command.StringReader;
import org.by1337.bmenu.Menu;
import org.by1337.bmenu.MenuConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CaseDefaultMenu extends Menu implements CaseMenu{
    private CaseBlock caseBlock;

    public CaseDefaultMenu(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
    }

    @Override
    public void setCaseBlock(CaseBlock caseBlock) {
        this.caseBlock = caseBlock;
    }

    @Override
    public void open() {
        Objects.requireNonNull(caseBlock, "case block cannot be null");
        super.open();
    }

    @Override
    protected void generate() {

    }

    @Override
    protected boolean runCommand(String s) throws CommandException {
        StringReader reader = new StringReader(s);
        String cmd = reader.readToSpace();
        if (CommandRegistry.COMMAND.getSubcommands().containsKey(cmd)) {
            reader.setCursor(0);
            CommandRegistry.COMMAND.process(new CommandContext(caseBlock, viewer), reader);
            return true;
        }
        return false;
    }

    @Override
    public String replace(String string) {
        return caseBlock == null ? super.replace(string) : caseBlock.replace(super.replace(string));
    }
}
