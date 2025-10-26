package co.technove.flareplatform;

import com.google.common.base.Preconditions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@NullMarked
public class FlarePlatformConfig {

    private final File configFile;
    private final YamlConfigurationLoader loader;
    private final Logger logger;
    private @Nullable ConfigurationNode config = null;

    public FlarePlatformConfig(String path, Logger logger) {
        File dataFolder = new File(path);
        this.logger = logger;
        this.configFile = new File(dataFolder, "config.yml");
        this.loader = YamlConfigurationLoader.builder()
                .file(configFile)
                .build();
        reloadConfig();
    }

    public void reloadConfig() {
        try {
            if (configFile.exists()) {
                this.config = loader.load();
            } else {
                this.config = loader.createNode();
                saveConfig();
            }
        } catch (ConfigurateException e) {
            logger.log(Level.SEVERE, "Failed to load the config file!", e);
        }
    }

    private void setDefault(String path, Object value) {
        ConfigurationNode node = getNode(path);
        try {
            node.set(value);
            saveConfig();
        } catch (SerializationException e) {
            logger.log(Level.SEVERE, "Failed to set default value for " + path, e);
        }
    }

    public void saveConfig() {
        try {
            Preconditions.checkState(config != null, "Config must not be null!");
            loader.save(config);
        } catch (ConfigurateException e) {
            logger.log(Level.SEVERE, "Failed to save the config file!", e);
        }
    }

    public boolean getBoolean(String path, boolean def) {
        if (!getNode(path).isNull()) {
            return getNode(path).getBoolean();
        }
        setDefault(path, def);
        return getNode(path).getBoolean(def);
    }

    public int getInt(String path, int def) {
        if (!getNode(path).isNull()) {
            return getNode(path).getInt();
        }
        setDefault(path, def);
        return getNode(path).getInt(def);
    }

    public double getDouble(String path, double def) {
        if (!getNode(path).isNull()) {
            return getNode(path).getDouble();
        }
        setDefault(path, def);
        return getNode(path).getDouble(def);
    }

    public String getString(String path, String def) {
        if (!getNode(path).isNull()) {
            return Objects.requireNonNullElse(getNode(path).getString(), "");
        }
        setDefault(path, def);
        return getNode(path).getString(def);
    }

    public List<String> getList(String path, List<String> def) {
        try {
            if (!getNode(path).isNull()) {
                return Objects.requireNonNullElse(getNode(path).getList(String.class), List.of());
            }
            setDefault(path, def);
            return getNode(path).getList(String.class, def);
        } catch (SerializationException e) {
            logger.log(Level.WARNING, "Failed to retrieve a list from the config file!");
            return List.of();
        }
    }

    public ConfigurationNode getNode(String path) {
        Preconditions.checkState(config != null, "Config must not be null!");
        return config.node((Object[]) path.split("\\."));
    }
}
