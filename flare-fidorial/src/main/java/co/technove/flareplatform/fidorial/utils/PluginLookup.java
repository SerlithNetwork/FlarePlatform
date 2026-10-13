package co.technove.flareplatform.fidorial.utils;

import fr.fidorial.Server;
import fr.fidorial.plugin.PluginMeta;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PluginLookup {

    private static final String LOADER_PREFIX = "fidorial-plugin:";

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final Server server;

    public PluginLookup(Server server) {
        this.server = server;
    }

    public Optional<String> getPluginForClass(final String name) {
        if (name.endsWith(".so")
            || name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")
            || name.startsWith("sun.") || name.startsWith("com.sun.")
            || name.startsWith("io.netty.") || name.startsWith("org.slf4j.")
            || name.startsWith("co.technove.")
            || name.startsWith("fr.fidorial.")
            || name.startsWith("fr.euphyllia.fidorial.")
        ) {
            return Optional.empty();
        }

        final String existing = this.cache.get(name);
        if (existing != null) {
            return existing.isEmpty() ? Optional.empty() : Optional.of(existing);
        }

        final Class<?> loadedClass;
        try {
            loadedClass = Class.forName(name);
        } catch (ClassNotFoundException | LinkageError e) {
            this.cache.put(name, "");
            return Optional.empty();
        }

        ClassLoader loader = loadedClass.getClassLoader();
        String pluginName = "";
        if (loader != null && loader.getName() != null && loader.getName().startsWith(LOADER_PREFIX)) {
            String jarName = loader.getName().substring(LOADER_PREFIX.length());
            pluginName = resolvePluginName(jarName);
        }

        this.cache.put(name, pluginName);
        return pluginName.isEmpty() ? Optional.empty() : Optional.of(pluginName);
    }

    private String resolvePluginName(String jarName) {
        String base = jarName.endsWith(".jar") ? jarName.substring(0, jarName.length() - 4) : jarName;
        for (PluginMeta meta : server.plugins().loaded()) {
            if (base.toLowerCase().contains(meta.id().toLowerCase())) {
                return meta.name();
            }
        }
        return base;
    }
}
