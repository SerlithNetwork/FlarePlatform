package co.technove.flareplatform.velocity.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PluginLookup {
    private final Cache<String, String> pluginNameCache = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .maximumSize(1024)
        .build();

    private final Map<ClassLoader, PluginContainer> classLoaderToPlugin = new ConcurrentHashMap<>();

    public PluginLookup(final ProxyServer server) {
        for (PluginContainer plugin : server.getPluginManager().getPlugins()) {
            classLoaderToPlugin.put(plugin.getClass().getClassLoader(), plugin);
        }
    }

    public Optional<String> getPluginForClass(final String name) {
        if (name.startsWith("com.google") || name.startsWith("it.unimi") || name.startsWith("sun")) {
            return Optional.empty();
        }

        final String existing = this.pluginNameCache.getIfPresent(name);
        if (existing != null) {
            return Optional.ofNullable(existing.isEmpty() ? null : existing);
        }


        final Class<?> loadedClass;
        try {
            loadedClass = Class.forName(name);
        } catch (ClassNotFoundException e) {
            this.pluginNameCache.put(name, "");
            return Optional.empty();
        }

        if (loadedClass.getClassLoader() == null) {
            this.pluginNameCache.put(name, "");
            return Optional.empty();
        }

        final PluginContainer plugin = this.classLoaderToPlugin.get(loadedClass.getClassLoader());
        final String pluginName = (plugin == null) ? "" : plugin.getDescription().getName().orElse("");

        this.pluginNameCache.put(name, pluginName);
        return Optional.ofNullable(pluginName.isEmpty() ? null : pluginName);
    }
}
