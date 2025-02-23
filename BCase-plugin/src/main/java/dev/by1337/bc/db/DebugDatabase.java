package dev.by1337.bc.db;

import dev.by1337.bc.bd.Database;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

public class DebugDatabase implements Database {

    @Override
    public int getKeysCount(@NotNull String keyId, @Nullable String name, @Nullable UUID uuid) {
        if (name == null && uuid == null) throw new IllegalArgumentException("name and uuid can't be null");
        return Integer.MAX_VALUE;
    }

    @Override
    public void giveKeys(@NotNull String keyId, int count, @Nullable String name, @Nullable UUID uuid) {
        if (name == null && uuid == null) throw new IllegalArgumentException("name and uuid can't be null");

    }

    @Override
    public void takeKeys(@NotNull String keyId, int count, @Nullable String name, @Nullable UUID uuid) {
        if (name == null && uuid == null) throw new IllegalArgumentException("name and uuid can't be null");
    }

    @Override
    public void close() throws IOException {

    }
}
