package dev.by1337.bc.addon;

import dev.by1337.bc.BCasesApi;
import org.bukkit.Bukkit;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AddonClassLoader extends URLClassLoader {
    private final Plugin plugin;
    private final AddonDescription description;
    private final AddonLoader loader;
    private final AbstractAddon addon;
    private final File file;
    private final BCasesApi bCasesApi;
    private final JarFile jar;
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();

    public AddonClassLoader(@Nullable ClassLoader parent, AddonDescription description, Plugin plugin, File file, AddonLoader loader, BCasesApi bCasesApi) throws MalformedURLException, InvalidAddonException {
        super(new URL[]{file.toURI().toURL()}, parent);

        this.plugin = plugin;
        this.description = description;
        this.loader = loader;
        this.file = file;
        this.bCasesApi = bCasesApi;
        try {
            jar = new JarFile(file);
        } catch (IOException e) {
            throw new InvalidAddonException("Failed to open jar file: " + file.getPath(), e);
        }
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

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> result = this.classes.get(name);
        if (result != null) return result;
        String path = name.replace('.', '/').concat(".class");
        JarEntry entry = this.jar.getJarEntry(path);
        if (entry != null) {
            byte[] classBytes;
            try (var in = jar.getInputStream(entry)) {
                classBytes = in.readAllBytes();
            } catch (IOException ex) {
                throw new ClassNotFoundException(name, ex);
            }
            classBytes = Bukkit.getServer().getUnsafe().processClass(plugin.getDescription(), path, classBytes);
            result = defineClass(name, classBytes, 0, classBytes.length);
            classes.put(name, result);
            return result;
        }
        return super.findClass(name);
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            jar.close();
        }
    }
}
