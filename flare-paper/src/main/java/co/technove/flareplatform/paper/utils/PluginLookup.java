package co.technove.flareplatform.paper.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

public class PluginLookup {
    private final Cache<String, String> pluginNameCache = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .maximumSize(1024)
        .build();
    private final Cache<Boolean, Map<Plugin, ClassLoader>> classLoaderCache = CacheBuilder.newBuilder() // Yes, boolean key, trust
        .expireAfterAccess(2, TimeUnit.MINUTES)
        .build();

    public Optional<String> getPluginForClass(final String name) {
        if (name.endsWith(".so")
            || name.startsWith("net.minecraft.") || name.startsWith("java.") || name.startsWith("com.mojang.")
            || name.startsWith("com.google.") || name.startsWith("it.unimi.") || name.startsWith("sun.") || name.startsWith("javax.")
            || name.startsWith("jdk.") || name.startsWith("io.papermc.") || name.startsWith("gg.pufferfish.") || name.startsWith("co.technove.")
            || name.startsWith("ca.spottedleaf.") || name.startsWith("com.sun.") || name.startsWith("org.jline.") || name.startsWith("org.bukkit.")
            || name.startsWith("org.spigotmc.") || name.startsWith("com.destroystokyo.paper") || name.startsWith("co.aikar.")
            || name.startsWith("io.netty.") || name.startsWith("com.velocitypowered.") || name.startsWith("org.purpurmc.purpur.")
        ) {
            return Optional.empty();
        }

        final String existing = this.pluginNameCache.getIfPresent(name);
        if (existing != null) {
            return Optional.ofNullable(existing.isEmpty() ? null : existing);
        }

        Map<Plugin, ClassLoader> classLoaders;
        try {
            classLoaders = this.classLoaderCache.get(true, PluginLookup::loadClassLoaders);
        } catch (ExecutionException ignore) {
            return Optional.empty();
        }

        Plugin plugin = this.matchPlugin(classLoaders, name);
        if (plugin == null) {
            return Optional.empty();
        }

        String pluginName = plugin.getName();
        this.pluginNameCache.put(name, pluginName);
        return Optional.ofNullable(pluginName.isEmpty() ? null : pluginName);
    }

    @SuppressWarnings("UnstableApiUsage")
    private @Nullable Plugin matchPlugin(Map<Plugin, ClassLoader> classLoaders, String className) {
        for (Map.Entry<Plugin, ClassLoader> entry : classLoaders.entrySet()) {
            boolean matchesPluginClassLoader = false;
            boolean matchesServerClassLoader = true;

            if (entry.getValue() instanceof ConfiguredPluginClassLoader classLoader) {
                try {
                    classLoader.loadClass(className, true, false, true);
                    matchesPluginClassLoader = true;
                } catch (ClassNotFoundException ignore) {}
            }
            try {
                Class.forName(className);
            } catch (ClassNotFoundException ignore) {
                matchesServerClassLoader = false;
            }
            if (matchesPluginClassLoader && !matchesServerClassLoader) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static Map<Plugin, ClassLoader> loadClassLoaders() {
        return Arrays.stream(Bukkit.getPluginManager().getPlugins()).collect(Collectors.toMap(i -> i, i -> i.getClass().getClassLoader()));
    }


}
