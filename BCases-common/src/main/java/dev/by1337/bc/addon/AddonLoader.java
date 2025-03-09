package dev.by1337.bc.addon;

import dev.by1337.bc.BCasesApi;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.configuration.YamlContext;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AddonLoader implements Closeable{

    private static final Logger LOGGER = LoggerFactory.getLogger("BCases#AddonLoader");
    private final Plugin plugin;
    private final Map<String, AbstractAddon> addons = new ConcurrentHashMap<>();
    private final List<AddonClassLoader> loaders = new CopyOnWriteArrayList<>();
    private final File folder;
    private final BCasesApi bCasesApi;

    public AddonLoader(Plugin plugin, File folder, BCasesApi bCasesApi) {
        this.folder = folder;
        this.plugin = plugin;
        this.bCasesApi = bCasesApi;
        folder.mkdirs();
    }

    public void findAddons() {
        for (File file : folder.listFiles()) {
            if (file.getName().endsWith(".jar")) {
                try {
                    AddonDescription description = new AddonDescription(readFileContentFromJar(file));
                    AddonClassLoader addonClassLoader = new AddonClassLoader(this.getClass().getClassLoader(), description, plugin, file, this, bCasesApi);
                    loaders.add(addonClassLoader);
                    addons.put(description.name(), addonClassLoader.addon());
                } catch (Throwable e) {
                    LOGGER.error("Failed to load addon {}", file.getName(), e);
                }
            }
        }
    }

    public void enableAll() {
        for (AbstractAddon value : addons.values()) {
            try {
                value.setEnabled(true);
            } catch (Throwable e) {
                LOGGER.error("Failed to enable addon {}", value.getDescription().name(), e);
            }
        }
    }

    public void disableAll() {
        for (AbstractAddon value : addons.values()) {
            try {
                value.setEnabled(false);
            } catch (Throwable e) {
                LOGGER.error("Failed to disable addon {}", value.getDescription().name(), e);
            }
        }
    }


    public static YamlContext readFileContentFromJar(File jar) throws IOException {
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry("addon.yml");
            if (entry != null) {
                try (InputStream inputStream = jarFile.getInputStream(entry);
                     InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                     BufferedReader reader = new BufferedReader(inputStreamReader)) {
                    return new YamlContext(YamlConfiguration.loadConfiguration(reader));
                }
            } else {
                throw new IOException("Jar does not contain addon.yml");
            }
        }
    }

    @Nullable
    public Class<?> getClassByName(String name, boolean resolve) {
        for (AddonClassLoader loader1 : loaders) {
            try {
                return loader1.loadClass0(name, resolve, false);
            } catch (ClassNotFoundException ignore) {
            }
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        addons.clear();
        for (AddonClassLoader loader : loaders) {
            loader.close();
        }
        loaders.clear();
    }
}
