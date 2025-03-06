package dev.by1337.bc.bd;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Database extends Closeable {


    User getUser(@NotNull Player player);
    // offline player -> fail
    User getUser(@NotNull UUID player);

    @Contract("null, null -> fail")
    CompletableFuture<User> loadUser(@Nullable String name, @Nullable UUID uuid);

    @Contract("_,null, null -> fail")
    void remKeyFromDb(CaseKey key, @Nullable String name, @Nullable UUID uuid);

    @Contract("_,null, null -> fail")
    void addKeyIntoDb(CaseKey key, @Nullable String name, @Nullable UUID uuid);
}
