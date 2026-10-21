package co.technove.flareplatform.velocity.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PluginLookup {
    private final Cache<String, String> pluginNameCache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(1))
        .maximumSize(1024)
        .build();

    private final Map<ClassLoader, PluginContainer> classLoaderToPlugin = new ConcurrentHashMap<>();

    public PluginLookup(final ProxyServer server) {
        for (final PluginContainer plugin : server.getPluginManager().getPlugins()) {
            classLoaderToPlugin.put(plugin.getClass().getClassLoader(), plugin);
        }
    }

    public Optional<String> getPluginForClass(final String name) {
        final int lambda = name.indexOf("$$Lambda");
        final String owner = lambda == -1 ? name : name.substring(0, lambda);

        if (owner.endsWith(".so")
            || owner.startsWith("net.minecraft.") || owner.startsWith("java.") || owner.startsWith("com.mojang.")
            || owner.startsWith("com.google.") || owner.startsWith("it.unimi.") || owner.startsWith("sun.") || owner.startsWith("javax.")
            || owner.startsWith("jdk.") || owner.startsWith("io.papermc.") || owner.startsWith("co.technove.")
            || owner.startsWith("ca.spottedleaf.") || owner.startsWith("com.sun.") || owner.startsWith("org.jline.")
            || owner.startsWith("com.destroystokyo.paper") || owner.startsWith("co.aikar.")
            || owner.startsWith("io.netty.") || owner.startsWith("com.velocitypowered.")
        ) {
            return Optional.empty();
        }

        final String existing = this.pluginNameCache.getIfPresent(owner);
        if (existing != null) {
            return Optional.ofNullable(existing.isEmpty() ? null : existing);
        }


        final Class<?> loadedClass;
        try {
            loadedClass = Class.forName(owner);
        } catch (final ClassNotFoundException ignored) {
            this.pluginNameCache.put(owner, "");
            return Optional.empty();
        }

        if (loadedClass.getClassLoader() == null) {
            this.pluginNameCache.put(owner, "");
            return Optional.empty();
        }

        final PluginContainer plugin = this.classLoaderToPlugin.get(loadedClass.getClassLoader());
        final String pluginName = (plugin == null) ? "" : plugin.getDescription().getName().orElse("");

        this.pluginNameCache.put(owner, pluginName);
        return Optional.ofNullable(pluginName.isEmpty() ? null : pluginName);
    }
}
