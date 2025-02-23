package dev.by1337.bc.bd;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.UUID;

public interface Database extends Closeable {
    @Contract("_, null, null -> fail")
    int getKeysCount(@NotNull String keyId, @Nullable String name, @Nullable UUID uuid);

    @Contract("_, _, null, null -> fail")
    void giveKeys(@NotNull String keyId, int count, @Nullable String name, @Nullable UUID uuid);

    @Contract("_, _, null, null -> fail")
    void takeKeys(@NotNull String keyId, int count, @Nullable String name, @Nullable UUID uuid);
}
