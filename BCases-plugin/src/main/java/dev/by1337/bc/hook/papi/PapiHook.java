package dev.by1337.bc.hook.papi;

import com.google.common.base.Joiner;
import dev.by1337.bc.bd.Database;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.hook.papi.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PapiHook extends PlaceholderExpansion {
    private final Plugin plugin;
    private final Placeholder placeholder;
    private final Database database;

    // [bcases_keys_count, bcase_keys_count_of_type]
    public PapiHook(Plugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        placeholder = new Placeholder(getIdentifier());
        placeholder.addSubPlaceholder(
                new Placeholder("keys_count")
                        .executor(((player, args) -> {
                            if (player == null) return "-1";
                            return String.valueOf(this.database.getUser(player).keysCount());
                        }))
        );
        placeholder.addSubPlaceholder(
                new Placeholder("keys_count_of_type")
                        .executor(((player, args) -> {
                            if (player == null || args.length == 0) return "-1";
                            return String.valueOf(this.database.getUser(player).keysCountOfType(Joiner.on("_").join(args)));
                        }))
        );
        placeholder.build();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bcases";
    }

    @Override
    public @NotNull String getAuthor() {
        return "By1337";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return placeholder.process(player, params.split("_"));
    }
}
