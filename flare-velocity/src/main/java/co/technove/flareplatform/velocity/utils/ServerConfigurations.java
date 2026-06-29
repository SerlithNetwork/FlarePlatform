package co.technove.flareplatform.velocity.utils;

import co.technove.flareplatform.velocity.config.FlareVelocityConfig;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
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

public class ServerConfigurations {

    public static Map<String, String> getCleanCopies() throws IOException {
        Map<String, String> files = new HashMap<>(FlareVelocityConfig.CONFIGURATIONS.CONFIGURATION_FILES.size());
        for (final String file : FlareVelocityConfig.CONFIGURATIONS.CONFIGURATION_FILES) {
            Path path = Path.of(file);
            if (Files.exists(path)) {
                files.put(file, ServerConfigurations.getCleanCopy(path));
            }
        }
        return files;
    }

    public static boolean matchesRegex(String key) {
        for (final Pattern pattern : FlareVelocityConfig.CONFIGURATIONS.HIDDEN_ENTRIES_PATTERNS) {
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
            case "toml": {
                Toml configuration = new Toml();
                try (final BufferedReader reader = Files.newBufferedReader(configPath)) {
                    configuration.read(reader);
                } catch (final IllegalStateException e) {
                    throw new IOException(e);
                }

                Map<String, Object> map = configuration.toMap();
                for (final Map.Entry<String, Object> entry : configuration.entrySet()) {
                    if (ServerConfigurations.matchesRegex(entry.getKey())) {
                        map.put(entry.getKey(), "");
                    }
                }
                if (map.size() <= 1) {
                    return "";
                } else {
                    TomlWriter writer = new TomlWriter();
                    return writer.write(map);
                }
            }
            default:
                return Files.readString(configPath);
        }
    }

}
