package dev.by1337.bc.db;

import dev.by1337.bc.bd.CaseKey;
import dev.by1337.bc.bd.Database;
import dev.by1337.bc.bd.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.nbt.DefaultNbtByteBuffer;
import org.by1337.blib.nbt.NBT;
import org.by1337.blib.nbt.NbtOps;
import org.by1337.blib.nbt.NbtType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FileDatabase implements Database, Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger("BCases#DB");

    private final Plugin plugin;
    private final Map<UUID, User> users = new HashMap<>();
    private final File dataFolder;

    public FileDatabase(Plugin plugin) {
        this.plugin = plugin;
        dataFolder = new File(plugin.getDataFolder(), "database");
        dataFolder.mkdirs();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadUser(player.getName(), player.getUniqueId()).thenAccept(user -> {
                users.put(player.getUniqueId(), user);
            }).exceptionally(e -> {
                LOGGER.error("Failed to load user", e);
                return null;
            });
        }
    }

    @Override
    public User getUser(@NotNull Player player) {
        return users.computeIfAbsent(player.getUniqueId(), k -> new User(
                player.getName(), k, this, new HashMap<>()
        ));
    }

    @Override
    public User getUser(@NotNull UUID player) {
        return getUser(Objects.requireNonNull(Bukkit.getPlayer(player), "Player is offline"));
    }

    @Override
    public CompletableFuture<User> loadUser(@Nullable String name, @Nullable UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, List<CaseKey>> keys = new HashMap<>();
            if (name != null) {
                for (CaseKey key : loadByName(name)) {
                    keys.computeIfAbsent(key.id(), k -> new ArrayList<>()).add(key);
                }
            }
            if (uuid != null) {
                for (CaseKey key : loadByUUID(uuid)) {
                    keys.computeIfAbsent(key.id(), k -> new ArrayList<>()).add(key);
                }
            }
            return new User(name, uuid, FileDatabase.this, keys);
        });
    }

    @Override
    public void remKeyFromDb(CaseKey key, @Nullable String name, @Nullable UUID uuid) {
        if (name == null && uuid == null) throw new NullPointerException("name and uuid cannot be null");
        // NOP
    }

    @Override
    public void addKeyIntoDb(CaseKey key, @Nullable String name, @Nullable UUID uuid) {
        if (name == null && uuid == null) throw new NullPointerException("name and uuid cannot be null");
        if (!isOffline(name, uuid)) return;
        if (uuid == null) {
            List<CaseKey> keys = loadByName(name);
            keys.add(key);
            write(keys, new File(dataFolder, name.toLowerCase(Locale.ENGLISH) + ".bnbt"));
        } else {
            List<CaseKey> keys = loadByUUID(uuid);
            keys.add(key);
            write(keys, new File(dataFolder, uuid + ".bnbt"));
        }
    }

    @Contract("null, null -> fail")
    private boolean isOffline(@Nullable String name, @Nullable UUID uuid) {
        if (name == null && uuid == null) throw new NullPointerException("name and uuid cannot be null");
        if (uuid != null) return Bukkit.getPlayer(uuid) == null;
        return Bukkit.getPlayerExact(name) == null;
    }

    @Override
    public void close() throws IOException {
        HandlerList.unregisterAll(this);
        users.values().forEach(this::flush);
        users.clear();
    }

    private void flush(User user) {
        File byName = new File(dataFolder, user.lastKnownName().toLowerCase(Locale.ENGLISH) + ".bnbt");
        File byUUID = new File(dataFolder, user.uuid() + ".bnbt");
        if (byUUID.exists()) byUUID.delete();
        if (byName.exists()) byName.delete();
        write(user.getAllKeys(), byUUID);
    }

    private void write(List<CaseKey> keys, File to) {
        try {
            if (keys.isEmpty()) {
                to.delete();
            } else {
                NBT data = CaseKey.LIST_CODEC.encodeStart(NbtOps.INSTANCE, keys).getOrThrow();
                DefaultNbtByteBuffer buffer = new DefaultNbtByteBuffer();
                data.write(buffer);
                Files.write(to.toPath(), buffer.toByteArray());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write NBT bytes", e);
        }
    }

    private List<CaseKey> loadByUUID(UUID uuid) {
        File byUUID = new File(dataFolder, uuid + ".bnbt");
        if (!byUUID.exists()) return new ArrayList<>();
        return load(byUUID);
    }

    private List<CaseKey> loadByName(String name) {
        File byName = new File(dataFolder, name.toLowerCase(Locale.ENGLISH) + ".bnbt");
        if (!byName.exists()) return new ArrayList<>();
        return load(byName);
    }

    private List<CaseKey> load(File file) {
        try {
            DefaultNbtByteBuffer buffer = new DefaultNbtByteBuffer(
                    Files.readAllBytes(file.toPath())
            );
            return CaseKey.LIST_CODEC.decode(NbtOps.INSTANCE, NbtType.LIST.read(buffer)).getOrThrow().getFirst();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loadUser(player.getName(), player.getUniqueId()).thenAccept(user -> {
            users.put(player.getUniqueId(), user);
        }).exceptionally(e -> {
            LOGGER.error("Failed to load user", e);
            return null;
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        var v = users.remove(event.getPlayer().getUniqueId());
        if (v != null) {
            CompletableFuture.runAsync(() -> flush(v))
                    .exceptionally(e -> {
                        LOGGER.error("Failed to flush user", e);
                        return null;
                    });
        }
    }
}
