package dev.by1337.bc.db;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

public class SqlDatabase implements Database, Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger("BCases#DB");
    private final Config config;
    private final HikariDataSource dataSource;
    private final Queue<Operation> operations = new LinkedBlockingDeque<>();
    private BukkitTask flushTask;
    private final Map<UUID, User> users = new HashMap<>();

    public SqlDatabase(Config config, Plugin plugin) {
        this.config = config;

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setUsername(config.user);
        hikariConfig.setPassword(config.password);
        hikariConfig.setMaximumPoolSize(config.maxPoolSize);
        hikariConfig.setJdbcUrl(config.url);
        if (config.url.contains("mariadb")) {
            hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
        }

        dataSource = new HikariDataSource(hikariConfig);
        initTable();
        flushTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::flush, 40, 40);

        for (Player player : Bukkit.getOnlinePlayers()) {
            loadUser(player.getName(), player.getUniqueId()).thenAccept(user -> {
                users.put(player.getUniqueId(), user);
            });
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void flush() {
        if (operations.isEmpty()) return;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO bcases_users (name, uuid, id, issue_date, removal_date) VALUES (?, ?, ?, ?, ?)");
                 PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM bcases_users WHERE (name = ? OR uuid = ?) AND id = ? AND issue_date = ? AND removal_date = ? LIMIT 1")) {

                Operation operation;
                while ((operation = operations.poll()) != null) {
                    PreparedStatement preparedStatement = (operation.type == OperationType.ADD)
                            ? insertStatement
                            : deleteStatement;

                    if (operation.name == null) {
                        preparedStatement.setNull(1, Types.VARCHAR);
                    } else {
                        preparedStatement.setString(1, operation.name);
                    }
                    if (operation.uuid == null) {
                        preparedStatement.setNull(2, Types.VARCHAR);
                    } else {
                        preparedStatement.setString(2, operation.uuid.toString());
                    }
                    preparedStatement.setString(3, operation.key.id());
                    preparedStatement.setLong(4, operation.key.issueDate());
                    preparedStatement.setLong(5, operation.key.removalDate());

                    preparedStatement.addBatch();
                }

                insertStatement.executeBatch();
                deleteStatement.executeBatch();

                connection.commit();

            } catch (Throwable e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to flush db!", e);
        }
    }


    private void initTable() {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS bcases_users (
                        name TEXT NULL,
                        uuid CHAR(36) NULL,
                        id VARCHAR(255) NOT NULL,
                        issue_date BIGINT NOT NULL,
                        removal_date BIGINT NOT NULL
                    );
                    """)) {
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            LOGGER.error("Не удалось создать таблицу!", e);
            throw new RuntimeException(e);
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
    public CompletableFuture<User> loadUser(@Nullable String name0, @Nullable UUID uuid) {
        if (name0 == null && uuid == null) throw new NullPointerException("name and uuid cannot be null");
        final String name;
        if (name0 != null) name = name0.toLowerCase();
        else name = null;
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(
                        "SELECT * FROM bcases_users WHERE name = ? OR uuid = ?;")) {
                    if (name != null) {
                        preparedStatement.setString(1, name);
                    } else {
                        preparedStatement.setNull(1, Types.VARCHAR);
                    }
                    if (uuid != null) {
                        preparedStatement.setString(2, uuid.toString());
                    } else {
                        preparedStatement.setNull(2, Types.VARCHAR);
                    }

                    Map<String, List<CaseKey>> keys = new HashMap<>();
                    UUID playerUUID = uuid;
                    String playerName = name;
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            String pName = resultSet.getString("name");
                            String pUuid = resultSet.getString("uuid");
                            long issue_date = resultSet.getLong("issue_date");
                            long removal_date = resultSet.getLong("removal_date");
                            String id = resultSet.getString("id");
                            if (pUuid != null && playerUUID == null) {
                                playerUUID = UUID.fromString(pUuid);
                            }
                            if (pName != null && playerName == null) {
                                playerName = pName;
                            }
                            keys.computeIfAbsent(id, k -> new ArrayList<>()).add(new CaseKey(id, issue_date, removal_date));
                        }
                    }
                    return new User(playerName, playerUUID, SqlDatabase.this, keys);
                }
            } catch (Throwable e) {
                LOGGER.error("Failed to load user! name={} uuid={}", name, uuid, e);
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void remKeyFromDb(CaseKey key, @Nullable String name, @Nullable UUID uuid) {
        if (name == null && uuid == null) throw new NullPointerException("name and uuid cannot be null");
        operations.offer(new Operation(
                key, name == null ? null : name.toLowerCase(),
                uuid,
                OperationType.REMOVE
        ));
    }

    @Override
    public void addKeyIntoDb(CaseKey key, @Nullable String name, @Nullable UUID uuid) {
        if (name == null && uuid == null) throw new NullPointerException("name and uuid cannot be null");
        operations.offer(new Operation(
                key, name == null ? null : name.toLowerCase(),
                uuid,
                OperationType.ADD
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        loadUser(player.getName(), player.getUniqueId()).thenAccept(user -> {
            users.put(player.getUniqueId(), user);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event){
        users.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void close() throws IOException {
        HandlerList.unregisterAll(this);
        flushTask.cancel();
        flush();
        dataSource.close();
    }

    public record Config(String user, String password, int maxPoolSize, String url) {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("user").forGetter(Config::user),
                Codec.STRING.fieldOf("password").forGetter(Config::password),
                Codec.INT.fieldOf("maxPoolSize").forGetter(Config::maxPoolSize),
                Codec.STRING.fieldOf("url").forGetter(Config::url)
        ).apply(instance, Config::new));
    }

    private enum OperationType {
        ADD,
        REMOVE
    }

    private record Operation(@NotNull CaseKey key, @Nullable String name, @Nullable UUID uuid, OperationType type) {
        @Override
        public String toString() {
            return "Operation{" +
                    "key=" + key +
                    ", name='" + name + '\'' +
                    ", uuid=" + uuid +
                    ", type=" + type +
                    '}';
        }
    }
}
