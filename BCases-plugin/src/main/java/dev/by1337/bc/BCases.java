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
import dev.by1337.bc.menu.KeyListMenu;
import dev.by1337.bc.prize.PrizeMap;
import dev.by1337.bc.util.LookingAtCaseBlockUtil;
import dev.by1337.bc.util.TimeFormatter;
import dev.by1337.bc.util.TimeParser;
import dev.by1337.bc.world.WorldGetter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.BLib;
import org.by1337.blib.chat.util.Message;
import org.by1337.blib.command.Command;
import org.by1337.blib.command.CommandException;
import org.by1337.blib.command.CommandWrapper;
import org.by1337.blib.command.argument.*;
import org.by1337.blib.command.requires.RequiresPermission;
import org.by1337.blib.configuration.YamlConfig;
import org.by1337.blib.configuration.YamlContext;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.geom.Vec3i;
import org.by1337.blib.net.RepositoryUtil;
import org.by1337.blib.util.ResourceUtil;
import org.by1337.blib.util.SpacedNameKey;
import org.by1337.blib.util.invoke.LambdaMetafactoryUtil;
import org.by1337.bmenu.MenuLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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
    private TimeFormatter timeFormatter;
    private YamlConfig config;

    @Override
    public void onLoad() {
        updateConfig();
        if (!new File(getDataFolder(), "animations").exists()) {
            ResourceUtil.saveIfNotExist("animations/randMobs.yml", this);
            ResourceUtil.saveIfNotExist("animations/swords.yml", this);
            ResourceUtil.saveIfNotExist("animations/creepers.yml", this);
            ResourceUtil.saveIfNotExist("animations/wheel.yml", this);
        }
        if (!new File(getDataFolder(), "menu").exists()) {
            ResourceUtil.saveIfNotExist("menu/default.yml", this);
            ResourceUtil.saveIfNotExist("menu/keylist.yml", this);
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
        menuLoader.getRegistry().register(new SpacedNameKey("bcases:key_list"), KeyListMenu::new);

    }


    @Override
    public void onEnable() {
        config = ResourceUtil.load("config.yml", this);
        timeFormatter = config.get("time-format").decode(TimeFormatter.CODEC).getOrThrow().getFirst();
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

    private void updateConfig() {
        var cfg = ResourceUtil.load("config.yml", this);
        YamlContext original;
        try (var in = Objects.requireNonNull(getResource("config.yml"))) {
            original = new YamlContext(YamlConfiguration.loadConfiguration(new InputStreamReader(in)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var map = original.get().getAsMap(String.class, Object.class);

        boolean updated = false;
        for (String s : map.keySet()) {
            if (!cfg.has(s)) {
                updated = true;
                cfg.set(s, map.get(s));
            }
        }
        if (updated) {
            cfg.trySave();
        }
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
                        .requires(new RequiresPermission<>("bcases.admin.give"))
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
                        .requires(new RequiresPermission<>("bcases.admin.take"))
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
                        .requires(new RequiresPermission<>("bcases.admin.remove"))
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
                        .requires(new RequiresPermission<>("bcases.admin.set"))
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
                        .requires(new RequiresPermission<>("bcases.admin.play"))
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
                .addSubCommand(new Command<CommandSender>("dump")
                        .requires(new RequiresPermission<>("bcases.admin.dump"))
                        .requires(sender -> sender instanceof Player)
                        .executor(((sender, args) -> {
                            Player player = (Player) sender;
                            ItemStack itemStack = player.getInventory().getItemInMainHand();
                            if (itemStack.getType().isAir()) {
                                message.sendMsg(sender, "&c[❌] &fУ Вас в руке должен быть предмет!");
                            }
                            String item = BLib.getApi().getItemStackSerialize().serialize(itemStack);
                            player.sendMessage(
                                    Component.text(item)
                                            .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Нажмите, чтобы скопировать")))
                                            .clickEvent(ClickEvent.copyToClipboard(item))
                            );
                        }))
                )
                .addSubCommand(new Command<CommandSender>("reload")
                        .requires(new RequiresPermission<>("bcases.admin.reload"))
                        .executor(((sender, args) -> {
                            long nanos = System.nanoTime();
                            onDisable0();
                            HandlerList.unregisterAll(this);
                            Bukkit.getScheduler().cancelTasks(this);
                            onLoad();
                            onEnable();
                            message.sendMsg(sender, "&a[✔] &fПлагин успешно перезагружен за {} ms.", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanos));
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

    @Override
    public TimeFormatter getTimeFormatter() {
        return timeFormatter;
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
