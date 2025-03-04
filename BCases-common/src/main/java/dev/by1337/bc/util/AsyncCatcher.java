package dev.by1337.bc.util;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class AsyncCatcher {

    public static void catchOp(@NotNull String identifier) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Asynchronous " + identifier + "!");
        }
    }
}
