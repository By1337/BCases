package dev.by1337.bc;

import dev.by1337.bc.addon.AddonLoader;
import dev.by1337.bc.animation.AnimationContext;
import dev.by1337.bc.animation.AnimationContextImpl;
import dev.by1337.bc.animation.AnimationLoader;
import dev.by1337.bc.bd.CaseKey;
import dev.by1337.bc.bd.Database;
import dev.by1337.bc.bd.User;
import dev.by1337.bc.db.MemoryDatabase;
import dev.by1337.bc.db.SqlDatabase;
import dev.by1337.bc.hologram.HologramManager;
import dev.by1337.bc.hook.papi.PapiHook;
import dev.by1337.bc.menu.CaseDefaultMenu;
import dev.by1337.bc.prize.PrizeMap;
import dev.by1337.bc.util.LookingAtCaseBlockUtil;
import dev.by1337.bc.util.TimeParser;
import dev.by1337.bc.world.WorldGetter;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.BLib;
import org.by1337.blib.chat.util.Message;
import org.by1337.blib.command.Command;
import org.by1337.blib.command.CommandException;
import org.by1337.blib.command.CommandWrapper;
import org.by1337.blib.command.argument.*;
import org.by1337.blib.command.requires.RequiresPermission;
import org.by1337.blib.configuration.YamlConfig;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.geom.Vec3i;
import org.by1337.blib.net.RepositoryUtil;
import org.by1337.blib.util.ResourceUtil;
import org.by1337.blib.util.SpacedNameKey;
import org.by1337.blib.util.invoke.LambdaMetafactoryUtil;
import org.by1337.bmenu.MenuLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class BCases extends JavaPlugin implements BCasesApi {
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
    private LookingAtCaseBlockUtil lookingAtCaseUtil;
    private AddonLoader addonLoader;

    @Override
    public void onLoad() {
        if (!new File(getDataFolder(), "animations").exists()) {
            ResourceUtil.saveIfNotExist("animations/randMobs.yml", this);
            ResourceUtil.saveIfNotExist("animations/swords.yml", this);
            ResourceUtil.saveIfNotExist("animations/creepers.yml", this);
        }
        if (!new File(getDataFolder(), "menu").exists()) {
            ResourceUtil.saveIfNotExist("menu/default.yml", this);
        }

        try {
            Class.forName("com.zaxxer.hikari.HikariConfig");
        } catch (Throwable t) {
            Path cp = RepositoryUtil.downloadIfNotExist("https://repo1.maven.org/maven2", "com.zaxxer", "HikariCP", "5.1.0", new File(getDataFolder(), "libraries").toPath());
            BLib.getApi().getUnsafe().getPluginClasspathUtil().addUrl(this, cp.toFile());
        }

        message = new Message(getLogger());
        menuLoader = new MenuLoader(new File(getDataFolder(), "menu"), this);
        menuLoader.getRegistry().register(new SpacedNameKey("bcases:case"), CaseDefaultMenu::new);

    }


    @Override
    public void onEnable() {
        loadDb();

        papiHook = new PapiHook(this, database);
        papiHook.register();

        prizeMap = ResourceUtil.load("prizes.yml", this).get().decode(PrizeMap.CODEC).getOrThrow().getFirst();
        blockManager = new BlockManager(this);
        lookingAtCaseUtil = new LookingAtCaseBlockUtil(blockManager);

        animationContext = new AnimationContextImpl(this);


        addonLoader = new AddonLoader(this, new File(getDataFolder(), "addons"), this);
        addonLoader.findAddons();
        addonLoader.enableAll();

        menuLoader.loadMenus();
        menuLoader.registerListeners();

        animationLoader = new AnimationLoader(this);
        animationLoader.load();

        commandWrapper = new CommandWrapper(createCommand(), this); // обязательно после загрузки анимаций
        commandWrapper.setPermission("bcases.admin");
        commandWrapper.register();

    }

    private void loadDb() {
        YamlConfig config = ResourceUtil.load("config.yml", this);
        String dbType = config.getAsString("database_type");
        if (dbType.equalsIgnoreCase("mariadb")) {
            loadMariaDbDriver();
            database = new SqlDatabase(config.get("database").decode(SqlDatabase.Config.CODEC).getOrThrow().getFirst(), this);
        } else if (dbType.equalsIgnoreCase("mysql")) {
            database = new SqlDatabase(config.get("database").decode(SqlDatabase.Config.CODEC).getOrThrow().getFirst(), this);
        } else if (dbType.equalsIgnoreCase("memory")) {
            database = new MemoryDatabase();
        } else {
            getSLF4JLogger().error("Unknown db type {}", dbType);
            database = new MemoryDatabase();
        }
    }

    private void loadMariaDbDriver() {
        Path cp = RepositoryUtil.downloadIfNotExist("https://repo1.maven.org/maven2", "org.mariadb.jdbc", "mariadb-java-client", "3.5.2", new File(getDataFolder(), "libraries").toPath());
        BLib.getApi().getUnsafe().getPluginClasspathUtil().addUrl(this, cp.toFile());
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
        addonLoader.disableAll();
        papiHook.unregister();
        commandWrapper.close();
        try {
            database.close();
        } catch (IOException e) {
            getSLF4JLogger().error("Failed to close database", e);
        }
        try {
            addonLoader.close();
        } catch (IOException e) {
            getSLF4JLogger().error("Failed to close addons loader", e);
        }
    }

    private Command<CommandSender> createCommand() {
        return new Command<CommandSender>("bcases")
                .aliases("case")
                .requires(new RequiresPermission<>("bcases.admin"))
                .addSubCommand(new Command<CommandSender>("give")
                        .argument(new MultiArgument<>("player",
                                new ArgumentPlayer<>("player"),
                                new ArgumentString<>("player")
                        ))
                        .argument(new ArgumentString<>("id", new ArrayList<>(prizeMap.keySet())))
                        .argument(new ArgumentInteger<>("count", List.of("1", "10", "25", "50")))
                        .argument(new ArgumentString<>("duration", List.of("7d", "14d", "31d")))
                        .executor(((sender, args) -> {
                            Object player = args.getOrThrow("player", "Use: /bcases give <player> <key id> <?count> <?duration>");
                            String keyId = (String) args.getOrThrow("id", "Use: /bcases give <player> <key id> <?count> <?duration>");
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
                                if (player instanceof Player pl) {
                                    message.sendMsg(sender, "&a[✔] &fУспешно выдал игроку {} {} ключей от кейса {}", pl.getName(), count, keyId);
                                } else {
                                    message.sendMsg(sender, "&a[✔] &fУспешно выдал &cофлайн&f игроку {} {} ключей от кейса {}", player, count, keyId);
                                }

                            }
                        }))
                )
                .addSubCommand(new Command<CommandSender>("take")
                        .argument(new MultiArgument<>("player",
                                new ArgumentPlayer<>("player"),
                                new ArgumentString<>("player")
                        ))
                        .argument(new ArgumentString<>("id", new ArrayList<>(prizeMap.keySet())))
                        .argument(new ArgumentInteger<>("count", List.of("1", "10", "25", "50")))
                        .executor(((sender, args) -> {
                            Object player = args.getOrThrow("player", "Use: /bcases take <player> <key id> <?count> <?duration>");
                            String keyId = (String) args.getOrThrow("id", "Use: /bcases take <player> <key id> <?count> <?duration>");
                            int count = (int) args.getOrDefault("count", 1);


                            if (player instanceof Player pl) {
                                User user = database.getUser(pl);
                                List<CaseKey> keys = new ArrayList<>(user.getKeysOfType(keyId));

                                int removed = Math.min(count, keys.size());
                                for (int i = 0; i < removed; i++) {
                                    user.removeKey(keys.get(i));
                                }
                                if (sender instanceof Player) {
                                    message.sendMsg(sender, "&a[✔] &fУспешно удалил ключи {} в количестве {} у игрока {}", keyId, removed, pl.getName());
                                }
                            } else {
                                message.sendMsg(sender, "&c[❌] &fНевозможно удалить ключи у оффлайн игрока!");
                            }

                        }))
                )
                .addSubCommand(new Command<CommandSender>("remove")
                        .requires(sender -> sender instanceof Player)
                        .requires(sender -> lookingAtCaseUtil.getLookingAtCaseBlock((Player) sender) != null)
                        .executor(((sender, args) -> {
                            CaseBlockImpl block = lookingAtCaseUtil.getLookingAtCaseBlock((Player) sender);
                            if (block == null) throw new CommandException("Block not found!");
                            blockManager.removeBlock(block);
                            blockManager.saveConfig();
                            message.sendMsg(sender, "&a[✔] &fУспешно удалил блок на координатах {} {} {}", block.pos().x, block.pos().y, block.pos().z);
                        }))
                )
                .addSubCommand(new Command<CommandSender>("set")
                        .requires(sender -> sender instanceof Player)
                        .argument(new ArgumentLookingAtBlock<>("pos"))
                        .executor(((sender, args) -> {
                            Player player = (Player) sender;

                            Vec3i pos = (Vec3i) args.getOrThrow("pos", "Use: /bcases <x> <y> <z>");
                            CaseBlockImpl caseBlock = new CaseBlockImpl(
                                    Material.END_PORTAL_FRAME.createBlockData(),
                                    new WorldGetter(player.getWorld().getName()),
                                    pos,
                                    menuLoader.getMenus().iterator().next().toString(),
                                    new HologramManager.Config(
                                            new Vec3d(0.5, 1.5, 0.5),
                                            List.of("&dКейсы", "&fНажми, чтобы открыть", "&bКлючей: %bcases_keys_count_of_type_default%")
                                    ),
                                    "default:none"
                            );
                            blockManager.addBlock(caseBlock);
                            blockManager.saveConfig();
                            message.sendMsg(sender, "&a[✔] &fУспешно установил кейс");
                        }))
                )

                .addSubCommand(new Command<CommandSender>("play")
                        .requires(sender -> sender instanceof Player)
                        .requires(sender -> lookingAtCaseUtil.getLookingAtCaseBlock((Player) sender) != null)
                        .argument(new ArgumentChoice<>("animation", new ArrayList<>(animationLoader.keySet())))
                        .argument(new ArgumentChoice<>("prizes", new ArrayList<>(prizeMap.keySet())))
                        .executor(((sender, args) -> {
                            CaseBlockImpl block = lookingAtCaseUtil.getLookingAtCaseBlock((Player) sender);
                            if (block == null) throw new CommandException("Block not found!");
                            String animation = (String) args.getOrThrow("animation", "Use: /bcases play <animation> <prizes>");
                            String prizes = (String) args.getOrThrow("prizes", "Use: /bcases play <animation> <prizes>");

                            block.playAnimation((Player) sender, animation, prizes);
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

    @Override
    public MenuLoader getMenuLoader() {
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
