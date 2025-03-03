package dev.by1337.bc;

import dev.by1337.bc.animation.AnimationContext;
import dev.by1337.bc.animation.AnimationContextImpl;
import dev.by1337.bc.animation.AnimationLoader;
import dev.by1337.bc.bd.CaseKey;
import dev.by1337.bc.bd.Database;
import dev.by1337.bc.db.MemoryDatabase;
import dev.by1337.bc.hook.papi.PapiHook;
import dev.by1337.bc.menu.CaseDefaultMenu;
import dev.by1337.bc.prize.PrizeMap;
import dev.by1337.bc.util.TimeParser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.chat.util.Message;
import org.by1337.blib.command.Command;
import org.by1337.blib.command.CommandWrapper;
import org.by1337.blib.command.argument.ArgumentInteger;
import org.by1337.blib.command.argument.ArgumentPlayer;
import org.by1337.blib.command.argument.ArgumentString;
import org.by1337.blib.command.argument.MultiArgument;
import org.by1337.blib.command.requires.RequiresPermission;
import org.by1337.blib.util.ResourceUtil;
import org.by1337.blib.util.SpacedNameKey;
import org.by1337.blib.util.invoke.LambdaMetafactoryUtil;
import org.by1337.bmenu.MenuLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiConsumer;

public class BCase extends JavaPlugin {
    private static final BiConsumer<JavaPlugin, Boolean> IS_ENABLED_SETTER;
    private PrizeMap prizeMap;
    private BlockManager blockManager;
    private Database database;
    private AnimationContext animationContext;
    private AnimationLoader animationLoader;
    private PapiHook papiHook;
    private CommandWrapper commandWrapper;
    private Message message;
    private MenuLoader menuLoader;

    @Override
    public void onLoad() {
        if (!new File(getDataFolder(), "animations").exists()) {
            ResourceUtil.saveIfNotExist("animations/randMobs.yml", this);
            ResourceUtil.saveIfNotExist("animations/swords.yml", this);
        }
        if (!new File(getDataFolder(), "menu").exists()) {
            ResourceUtil.saveIfNotExist("menu/default.yml", this);
        }
        message = new Message(getLogger());
        menuLoader = new MenuLoader(new File(getDataFolder(), "menu"), this);
        menuLoader.getRegistry().register(new SpacedNameKey("bcase:case"), CaseDefaultMenu::new);

    }

    @Override
    public void onEnable() {
        database = new MemoryDatabase();
        papiHook = new PapiHook(this, database);
        papiHook.register();

        prizeMap = ResourceUtil.load("prizes.yml", this).get().decode(PrizeMap.CODEC).getOrThrow().getFirst();
        blockManager = new BlockManager(this);

        animationContext = new AnimationContextImpl(this);

        commandWrapper = new CommandWrapper(createCommand(), this);
        commandWrapper.setPermission("bcase.admin");
        commandWrapper.register();

        //todo register custom animations

        menuLoader.loadMenus();
        menuLoader.registerListeners();

        animationLoader = new AnimationLoader(this);
        animationLoader.load();

    }

    @Override
    public void onDisable() {
        IS_ENABLED_SETTER.accept(this, true);
        try {
            onDisable0();
        } finally {
            IS_ENABLED_SETTER.accept(this, false);
        }
    }

    private void onDisable0() {
        blockManager.close();
        papiHook.unregister();
        commandWrapper.close();
        try {
            database.close();
        } catch (IOException e) {
            getSLF4JLogger().error("Failed to close database", e);
        }
    }

    private Command<CommandSender> createCommand() {
        return new Command<CommandSender>("bcase")
                .aliases("case")
                .requires(new RequiresPermission<>("bcase.admin"))
                .addSubCommand(new Command<CommandSender>("give")
                        .argument(new MultiArgument<>("player",
                                new ArgumentPlayer<>("player"),
                                new ArgumentString<>("player")
                        ))
                        .argument(new ArgumentString<>("id", List.of("default")))
                        .argument(new ArgumentInteger<>("count", List.of("1", "10", "25", "50")))
                        .argument(new ArgumentString<>("duration", List.of("7d", "14d", "31d")))
                        .executor(((sender, args) -> {
                            Object player = args.getOrThrow("player", "Use: /bc give <player> <key id> <?count> <?duration>");
                            String keyId = (String) args.getOrThrow("id", "Use: /bc give <player> <key id> <?count> <?duration>");
                            int count = (int) args.getOrDefault("count", 1);
                            long duration = TimeParser.parse((String) args.getOrDefault("duration", "10y"));

                            for (int i = 0; i < count; i++) {
                                CaseKey key = new CaseKey(keyId, System.currentTimeMillis(), System.currentTimeMillis() + duration);
                                if (player instanceof Player pl) {
                                    database.getUser(pl).addKey(key);
                                } else {
                                    database.addKeyIntoDb(key, String.valueOf(player), null);
                                }
                            }
                            if (sender instanceof Player) {
                                message.sendMsg(sender, "&aУспешно выдал игроку {} {} ключей от кейса {}", player, count, keyId);
                            }
                        }))
                )
                ;
    }

    public PrizeMap prizeMap() {
        return prizeMap;
    }

    public BlockManager blockManager() {
        return blockManager;
    }

    public Database database() {
        return database;
    }

    public AnimationContext animationContext() {
        return animationContext;
    }

    public AnimationLoader animationLoader() {
        return animationLoader;
    }

    public MenuLoader menuLoader() {
        return menuLoader;
    }

    static {
        try {
            Field field = JavaPlugin.class.getDeclaredField("isEnabled");
            field.setAccessible(true);
            IS_ENABLED_SETTER = LambdaMetafactoryUtil.setterOf(field);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
