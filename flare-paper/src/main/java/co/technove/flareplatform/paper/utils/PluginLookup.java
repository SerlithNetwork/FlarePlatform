package co.technove.flareplatform.paper.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

public final class PluginLookup {
    private final Cache<String, String> pluginNameCache = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(1))
        .maximumSize(1024)
        .build();
    private final Cache<Boolean, Map<Plugin, ClassLoader>> classLoaderCache = CacheBuilder.newBuilder() // Yes, boolean key, trust
        .expireAfterAccess(Duration.ofMinutes(2))
        .build();

    public Optional<String> getPluginForClass(final String name) {
        final int lambda = name.indexOf("$$Lambda");
        final String owner = lambda == -1 ? name : name.substring(0, lambda);

        if (owner.endsWith(".so")
            || owner.startsWith("net.minecraft.") || owner.startsWith("java.") || owner.startsWith("com.mojang.")
            || owner.startsWith("com.google.") || owner.startsWith("it.unimi.") || owner.startsWith("sun.") || owner.startsWith("javax.")
            || owner.startsWith("jdk.") || owner.startsWith("io.papermc.") || owner.startsWith("gg.pufferfish.") || owner.startsWith("co.technove.")
            || owner.startsWith("ca.spottedleaf.") || owner.startsWith("com.sun.") || owner.startsWith("org.jline.") || owner.startsWith("org.bukkit.")
            || owner.startsWith("org.spigotmc.") || owner.startsWith("com.destroystokyo.paper") || owner.startsWith("co.aikar.")
            || owner.startsWith("io.netty.") || owner.startsWith("com.velocitypowered.") || owner.startsWith("org.purpurmc.purpur.")
        ) {
            return Optional.empty();
        }

        final String existing = this.pluginNameCache.getIfPresent(owner);
        if (existing != null) {
            return Optional.ofNullable(existing.isEmpty() ? null : existing);
        }

        final Map<Plugin, ClassLoader> classLoaders;
        try {
            classLoaders = this.classLoaderCache.get(true, PluginLookup::loadClassLoaders);
        } catch (final ExecutionException ignored) {
            return Optional.empty();
        }

        final Plugin plugin = this.matchPlugin(classLoaders, owner);
        if (plugin == null) {
            this.pluginNameCache.put(owner, "");
            return Optional.empty();
        }

        final String pluginName = plugin.getName();
        this.pluginNameCache.put(owner, pluginName);
        return Optional.ofNullable(pluginName.isEmpty() ? null : pluginName);
    }

    @SuppressWarnings("UnstableApiUsage")
    private @Nullable Plugin matchPlugin(final Map<Plugin, ClassLoader> classLoaders, final String className) {
        for (final Map.Entry<Plugin, ClassLoader> entry : classLoaders.entrySet()) {
            boolean matchesPluginClassLoader = false;
            boolean matchesServerClassLoader = true;

            if (entry.getValue() instanceof final ConfiguredPluginClassLoader classLoader) {
                try {
                    classLoader.loadClass(className, true, false, true);
                    matchesPluginClassLoader = true;
                } catch (final ClassNotFoundException ignored) {}
            }
            try {
                Class.forName(className);
            } catch (final ClassNotFoundException ignored) {
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
