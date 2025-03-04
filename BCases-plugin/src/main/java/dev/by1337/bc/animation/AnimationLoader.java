package dev.by1337.bc.animation;

import dev.by1337.bc.yaml.CashedYamlContext;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.configuration.YamlConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

public class AnimationLoader {
    private final Plugin plugin;
    private final Random random = new Random();
    private final Map<String, AnimationData> animations = new HashMap<>();
    private final List<AnimationData> animationDataList = new ArrayList<>();

    public AnimationLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    @NotNull
    public AnimationData getAnimation(String animationName) {
        return Objects.requireNonNull(animations.get(animationName), "Unknown animation: " + animationName);
    }
    public AnimationData getRandomAnimation() {
        return animationDataList.get(random.nextInt(animationDataList.size()));
    }

    public void load() {
        List<File> files = findFiles(new File(plugin.getDataFolder(), "animations"), file -> file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"));
        for (File file : files) {
            try {
                YamlConfig config = new YamlConfig(file);
                AnimationRegistry.AnimationCreator creator = AnimationRegistry.INSTANCE.lookup(
                        Objects.requireNonNull(config.getAsString("provider"), "provider is not defined!")
                );
                if (creator == null) {
                    throw new IllegalArgumentException("Unknown provider: " + config.getAsString("provider"));
                }
                String id = Objects.requireNonNull(config.getAsString("id"), "id is not defined!");
                AnimationData data = new AnimationData(creator, id, new CashedYamlContext(config));
                if (animations.containsKey(id)) {
                    throw new IllegalArgumentException("Duplicate id: " + id);
                }
                animations.put(id, data);
                animationDataList.add(data);
            } catch (Throwable t) {
                plugin.getSLF4JLogger().error("Failed to load animation from {}", file.getAbsolutePath(), t);
            }
        }
    }

    private List<File> findFiles(File dir, Predicate<File> filter) {
        List<File> result = new ArrayList<>();
        for (File file : Objects.requireNonNull(dir.listFiles(), dir + " folder isn't exists")) {
            if (file.isDirectory()) {
                result.addAll(this.findFiles(file, filter));
            }

            if (filter.test(file)) {
                result.add(file);
            }
        }

        return result;
    }
}
