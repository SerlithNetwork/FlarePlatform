package co.technove.flareplatform.fidorial.utils;

import co.technove.flareplatform.fidorial.FlarePlatformFidorial;
import co.technove.flareplatform.fidorial.config.FlareFidorialConfig;
import com.google.common.base.Preconditions;
import fr.fidorial.world.World;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ServerConfigurations {

    private ServerConfigurations() {
    }

    public static Map<String, String> getCleanCopies() throws IOException {
        final FlarePlatformFidorial platform = FlarePlatformFidorial.getInstance();
        Preconditions.checkState(platform != null, "The plugin has not yet been enabled");

        final Map<String, String> files = new HashMap<>(FlareFidorialConfig.CONFIGURATIONS.CONFIGURATION_FILES.size());
        for (final String file : FlareFidorialConfig.CONFIGURATIONS.CONFIGURATION_FILES) {
            final Path path = Path.of(file);
            if (Files.exists(path)) {
                files.put(file, getCleanCopy(path));
            }
        }
        for (final World world : platform.context().server().worlds()) {
            final Path worldPath = world.paths().directory();
            for (final String configName : FlareFidorialConfig.CONFIGURATIONS.WORLD_CONFIGURATION_FILES) {
                final Path config = worldPath.resolve(configName);
                if (Files.exists(config)) {
                    final String cleanConfig = getCleanCopy(config);
                    if (!cleanConfig.isEmpty()) {
                        files.put(config.toString(), cleanConfig);
                    }
                }
            }
        }
        return files;
    }

    public static boolean matchesRegex(final String key) {
        return FlareFidorialConfig.CONFIGURATIONS.HIDDEN_ENTRIES_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(key).matches());
    }

    public static String getCleanCopy(final Path configPath) throws IOException {
        return switch (com.google.common.io.Files.getFileExtension(configPath.getFileName().toString())) {
            case "yml", "yaml" -> cleanYaml(configPath);
            default -> Files.readString(configPath);
        };
    }

    private static String cleanYaml(final Path configPath) throws IOException {
        try {
            final ConfigurationNode root = YamlConfigurationLoader.builder()
                .path(configPath)
                .build()
                .load();
            removeHidden(root, "");
            if (root.childrenMap().isEmpty()) {
                return "";
            }

            final StringWriter writer = new StringWriter();
            YamlConfigurationLoader.builder()
                .sink(() -> new BufferedWriter(writer))
                .headerMode(HeaderMode.NONE)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build()
                .save(root);
            return writer.toString();
        } catch (final ConfigurateException e) {
            throw new IOException(e);
        }
    }

    private static void removeHidden(final ConfigurationNode node, final String prefix) {
        for (final Map.Entry<Object, ? extends ConfigurationNode> entry : List.copyOf(node.childrenMap().entrySet())) {
            final String key = prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + '.' + entry.getKey();
            if (matchesRegex(key)) {
                node.removeChild(entry.getKey());
            } else if (entry.getValue().isMap()) {
                removeHidden(entry.getValue(), key);
            }
        }
    }
}
