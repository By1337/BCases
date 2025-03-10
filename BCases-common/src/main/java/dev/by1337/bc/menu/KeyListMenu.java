package dev.by1337.bc.menu;

import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.bd.CaseKey;
import dev.by1337.bc.bd.User;
import dev.by1337.bc.command.CommandContext;
import dev.by1337.bc.command.CommandRegistry;
import org.bukkit.entity.Player;
import org.by1337.blib.command.CommandException;
import org.by1337.blib.command.StringReader;
import org.by1337.blib.configuration.YamlValue;
import org.by1337.bmenu.Menu;
import org.by1337.bmenu.MenuConfig;
import org.by1337.bmenu.MenuItem;
import org.by1337.bmenu.MenuItemBuilder;
import org.by1337.bmenu.animation.util.AnimationUtil;
import org.by1337.bmenu.impl.MultiPageMenu;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class KeyListMenu extends MultiPageMenu<CaseKey> implements CaseMenu {

    private final Map<String, MenuItemBuilder> keyItems;

    private CaseBlock caseBlock;
    private User user;

    public KeyListMenu(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        setItemSlots(config.getCashedContext().get("data-slots", v -> AnimationUtil.readSlots(v.getAsString())));
        keyItems = config.getCashedContext().get("crates", v -> v.getAsMap(YamlValue::getAsString, v1 -> MenuItemBuilder.read(v1.getAsYamlContext(), loader)));
    }

    @Override
    public void setCaseBlock(CaseBlock caseBlock) {
        this.caseBlock = caseBlock;
        user = caseBlock.getDatabase().getUser(viewer);
    }

    @Override
    protected MenuItem map(CaseKey input) {
        input.initPlaceholders(caseBlock.getBCasesApi());
        MenuItemBuilder builder = keyItems.get(input.id());
        var item = builder.build(this, null, input);
        if (item == null) return null;
        item.setData(input);
        return item;
    }

    @Override
    protected List<CaseKey> getItems() {
        var list = user.getAllKeys();
        list.removeIf(k -> !keyItems.containsKey(k.id()));
        return list;
    }

    @Override
    protected boolean runCommand(String s) throws CommandException {
        if (s.equalsIgnoreCase("[TAKE_THIS_KEY]")) {
            if (lastClickedItem != null && lastClickedItem.getData() instanceof CaseKey caseKey) {
                user.removeKey(caseKey);
            }
            return true;
        }
        StringReader reader = new StringReader(s);
        String cmd = reader.readToSpace();
        if (CommandRegistry.COMMAND.getSubcommands().containsKey(cmd)) {
            reader.setCursor(0);
            CommandRegistry.COMMAND.process(new CommandContext(caseBlock, viewer), reader);
            return true;
        }
        return super.runCommand(cmd);
    }
    @Override
    public String replace(String string) {
        return caseBlock == null ? super.replace(string) : caseBlock.replace(super.replace(string));
    }
}
