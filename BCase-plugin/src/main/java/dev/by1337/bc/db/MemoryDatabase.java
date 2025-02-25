package dev.by1337.bc.db;

import dev.by1337.bc.bd.CaseKey;
import dev.by1337.bc.bd.Database;
import dev.by1337.bc.bd.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MemoryDatabase implements Database {
    private final Map<UUID, User> users = new HashMap<>();

    @Override
    public User getUser(@NotNull Player player) {
        return users.computeIfAbsent(player.getUniqueId(), k -> new User(
                player.getName(), k, new HashMap<>()
        ));
    }

    @Override
    public User getUser(@NotNull UUID uuid) {
        return getUser(Objects.requireNonNull(Bukkit.getPlayer(uuid), "Player is offline"));
    }

    @Override
    public void remKeyFromDb(CaseKey key, @Nullable String name, @Nullable UUID uuid) {
        if (name == null && uuid == null) {
            throw new NullPointerException("name and uuid cannot be null");
        }

    }

    @Override
    public void addKeyIntoDb(CaseKey key, @Nullable String name, @Nullable UUID uuid) {
        if (name == null && uuid == null) {
            throw new NullPointerException("name and uuid cannot be null");
        }
    }

    @Override
    public void close() throws IOException {

    }
}
