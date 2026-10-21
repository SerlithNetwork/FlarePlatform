package co.technove.flareplatform.fidorial.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fr.fidorial.Server;
import fr.fidorial.plugin.PluginMeta;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PluginLookup {
    private final Cache<String, String> cache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(1))
        .maximumSize(1024)
        .build();

    private final Map<ClassLoader, String> pluginByLoader;

    public PluginLookup(final Server server) {
        final Map<ClassLoader, String> pluginByLoader = new HashMap<>();
        for (final PluginMeta meta : server.plugins().loaded()) {
            server.plugins().plugin(meta.id())
                .ifPresent(plugin -> pluginByLoader.put(plugin.getClass().getClassLoader(), meta.name()));
        }
        this.pluginByLoader = Map.copyOf(pluginByLoader);
    }

    public Optional<String> getPluginForClass(final String name) {
        final int lambda = name.indexOf("$$Lambda");
        final String owner = lambda == -1 ? name : name.substring(0, lambda);

        if (owner.endsWith(".so")
            || owner.startsWith("java.") || owner.startsWith("javax.") || owner.startsWith("jdk.")
            || owner.startsWith("sun.") || owner.startsWith("com.sun.")
            || owner.startsWith("io.netty.") || owner.startsWith("org.slf4j.")
            || owner.startsWith("co.technove.")
            || owner.startsWith("fr.fidorial.")
            || owner.startsWith("fr.euphyllia.fidorial.server.")
        ) {
            return Optional.empty();
        }

        final String existing = this.cache.getIfPresent(owner);
        if (existing != null) {
            return existing.isEmpty() ? Optional.empty() : Optional.of(existing);
        }

        String pluginName = "";
        for (final Map.Entry<ClassLoader, String> entry : this.pluginByLoader.entrySet()) {
            if (definedBy(owner, entry.getKey())) {
                pluginName = entry.getValue();
                break;
            }
        }

        this.cache.put(owner, pluginName);
        return pluginName.isEmpty() ? Optional.empty() : Optional.of(pluginName);
    }

    private static boolean definedBy(final String name, final ClassLoader loader) {
        try {
            return Class.forName(name, true, loader).getClassLoader() == loader;
        } catch (final ClassNotFoundException | LinkageError _) {
            return false;
        }
    }
}
