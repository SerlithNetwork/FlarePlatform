package co.technove.flareplugin.utils;

import co.technove.flareplugin.FlarePlugin;
import com.google.common.io.Files;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

@NullMarked
public class ServerConfigurations {
    public static final List<String> configurationFiles = FlarePlugin.getFlareConfig().getList("flare.server-configs",
        List.of(
            "server.properties",
            "bukkit.yml",
            "spigot.yml",
            "config/paper-global.yml",
            "config/paper-world-defaults.yml"
        ));
    private static final List<World> worldList = new ArrayList<>();
    private static final Map<String, String> configFiles = new HashMap<>(configurationFiles.size());
    public static final List<String> hiddenEntries = FlarePlugin.getFlareConfig().getList("flare.hidden-entries",
        List.of(
            "proxies.velocity.secret",
            "web-services.token",
            "misc.sentry-dsn",
            "database",
            "server-ip",
            "motd",
            "resource-pack",
            "level-seed",
            "rcon.password",
            "rcon.ip",
            "feature-seeds",
            "world-settings.*.feature-seeds",
            "world-settings.*.seed-*",
            "seed-*"
        ));

        private static final List<Pattern> regexPatterns = hiddenEntries.stream()
                .map(s -> Pattern.compile(s.replace(".", "\\.").replace("*", ".*")))
                .toList();

        public static Map<String, String> getCleanCopies() throws IOException {
            for (final String file : configurationFiles) {
                if (configFiles.containsKey(file)) continue;
                configFiles.put(file, getCleanCopy(file));
            }

            for (final World world : Bukkit.getWorlds()) {
                if (worldList.contains(world)) continue;
                final File worldDir = world.getWorldFolder();
                final String paperWorldConfig = new File(worldDir, "paper-world.yml").getPath();
                final String cleanConfig = getCleanCopy(paperWorldConfig);
                worldList.add(world);
                if (!cleanConfig.isEmpty()) {
                    configFiles.put(paperWorldConfig, cleanConfig);
                }
            }
            return configFiles;
        }

        public static boolean matchesRegex(String key) {
            for (final Pattern pattern : regexPatterns) {
                if (pattern.matcher(key).matches()) {
                    return true;
                }
            }
            return false;
        }

        public static String getCleanCopy(String configName) throws IOException {
            final File file = new File(configName);

            switch (Files.getFileExtension(configName)) {
                case "properties": {
                    final Properties properties = new Properties();
                    try (final FileInputStream inputStream = new FileInputStream(file)) {
                        properties.load(inputStream);
                    }
                    for (final String hiddenConfig : properties.stringPropertyNames()) {
                        if (matchesRegex(hiddenConfig)) properties.remove(hiddenConfig);
                    }
                    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    properties.store(outputStream, "");
                    return Arrays.stream(outputStream.toString()
                                    .split("\n"))
                            .filter(line -> !line.startsWith("#"))
                            .collect(Collectors.joining("\n"));
                }
                case "yml": {
                    final YamlConfiguration configuration = new YamlConfiguration();
                    try {
                        configuration.load(file);
                    } catch (final InvalidConfigurationException e) {
                        throw new IOException(e);
                    }
                    configuration.options().setHeader(null);
                    for (final String key : configuration.getKeys(true)) {
                        if (matchesRegex(key)) {
                            configuration.set(key, null);
                        }
                    }
                    if (configuration.getKeys(false).size() == 1) {
                        return "";
                    } else {
                        return configuration.saveToString();
                    }
                }
                default:
                    throw new IllegalArgumentException("Bad file type " + configName);
            }
        }

    }
