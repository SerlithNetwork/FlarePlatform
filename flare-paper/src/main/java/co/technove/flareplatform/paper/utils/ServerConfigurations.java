package co.technove.flareplatform.paper.utils;

import co.technove.flareplatform.paper.config.FlarePaperConfig;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class ServerConfigurations {

    public static Map<String, String> getCleanCopies() throws IOException {
        Map<String, String> files = new HashMap<>(FlarePaperConfig.CONFIGURATIONS.CONFIGURATION_FILES.size());
        for (final String file : FlarePaperConfig.CONFIGURATIONS.CONFIGURATION_FILES) {
            Path path = Path.of(file);
            if (Files.exists(path)) {
                files.put(file, ServerConfigurations.getCleanCopy(path));
            }
        }
        for (final World world : Bukkit.getWorlds()) {
            final Path worldPath = world.getWorldPath();
            for (String configName : FlarePaperConfig.CONFIGURATIONS.WORLD_CONFIGURATION_FILES) {
                final Path config = worldPath.resolve(configName);
                if (Files.exists(config)) {
                    final String cleanConfig = ServerConfigurations.getCleanCopy(config);
                    if (!cleanConfig.isEmpty()) {
                        files.put(config.toString(), cleanConfig);
                    }
                }
            }
        }
        return files;
    }

    public static boolean matchesRegex(String key) {
        for (final Pattern pattern : FlarePaperConfig.CONFIGURATIONS.HIDDEN_ENTRIES_PATTERNS) {
            if (pattern.matcher(key).matches()) {
                return true;
            }
        }
        return false;
    }

    public static String getCleanCopy(Path configPath) throws IOException {
        switch (com.google.common.io.Files.getFileExtension(configPath.getFileName().toString())) {
            case "properties": {
                final Properties properties = new Properties();
                try (final InputStream inputStream = Files.newInputStream(configPath)) {
                    properties.load(inputStream);
                }
                for (final String hiddenConfig : properties.stringPropertyNames()) {
                    if (ServerConfigurations.matchesRegex(hiddenConfig)) {
                        properties.remove(hiddenConfig);
                    }
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
                try (final BufferedReader reader = Files.newBufferedReader(configPath)) {
                    configuration.load(reader);
                } catch (final InvalidConfigurationException e) {
                    throw new IOException(e);
                }
                configuration.options().setHeader(null);
                for (final String key : configuration.getKeys(true)) {
                    if (ServerConfigurations.matchesRegex(key)) {
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
                throw new IllegalArgumentException("Bad file type " + configPath);
        }
    }

}
