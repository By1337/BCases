package dev.by1337.bc.addon;

import dev.by1337.bc.BCasesApi;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

public class AddonClassLoader extends URLClassLoader {
    private final Plugin plugin;
    private final AddonDescription description;
    private final AddonLoader loader;
    private final AbstractAddon addon;
    private final File file;
    private final BCasesApi bCasesApi;

    public AddonClassLoader(@Nullable ClassLoader parent, AddonDescription description, Plugin plugin, File file, AddonLoader loader, BCasesApi bCasesApi) throws MalformedURLException, InvalidAddonException {
        super(new URL[]{file.toURI().toURL()}, parent);

        this.plugin = plugin;
        this.description = description;
        this.loader = loader;
        this.file = file;
        this.bCasesApi = bCasesApi;

        try {
            Class<?> jarClass;
            try {
                jarClass = Class.forName(description.mainClass(), true, this);
            } catch (ClassNotFoundException ex) {
                throw new InvalidAddonException("Cannot find main class '{}'", ex, description.mainClass());
            }
            Class<? extends AbstractAddon> pluginClass;
            try {
                pluginClass = jarClass.asSubclass(AbstractAddon.class);
            } catch (ClassCastException ex) {
                throw new InvalidAddonException("main class `" + description.mainClass() + "' does not extend AbstractAddon", ex);
            }
            addon = pluginClass.newInstance();
        } catch (IllegalAccessException ex) {
            throw new InvalidAddonException("No public constructor", ex);
        } catch (InstantiationException ex) {
            throw new InvalidAddonException("Abnormal service type", ex);
        }
    }

    public AbstractAddon addon() {
        return addon;
    }

    void initialize(AbstractAddon addon) {
        addon.init(LoggerFactory.getLogger("BCases#" + description.name()), description, this, file, plugin, bCasesApi);
    }

    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return findResources(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass0(name, resolve, true);
    }

    Class<?> loadClass0(@NotNull String name, boolean resolve, boolean global) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException ignore) {
        }

        if (global) {
            Class<?> result = loader.getClassByName(name, resolve);
            if (result != null) {
                return result;
            }
        }
        throw new ClassNotFoundException(name);
    }
}
