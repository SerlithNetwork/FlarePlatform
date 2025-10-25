package co.technove.flareplatform.velocity;

import com.google.common.base.Preconditions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

@NullMarked
public class FlareConfig {

    private final File configFile;
    private final YamlConfigurationLoader loader;
    private @Nullable ConfigurationNode config = null;

    public FlareConfig(String path) {
        File dataFolder = new File(path);
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
            }
            saveConfig();
        } catch (ConfigurateException e) {
            FlarePlatformVelocity.getInstance().getLogger()
                    .log(Level.SEVERE, "Failed to load the config file!", e);
        }
    }

    private void setDefault(String path, Object value) {
        ConfigurationNode node = getNode(path);
        if (node.virtual()) {
            try {
                node.set(value);
                saveConfig();
            } catch (SerializationException e) {
                FlarePlatformVelocity.getInstance().getLogger()
                        .log(Level.SEVERE, "Failed to set default value for " + path, e);
            }
        }
    }

    public void saveConfig() {
        try {
            Preconditions.checkState(config != null, "Config must not be null!");
            loader.save(config);
        } catch (ConfigurateException e) {
            FlarePlatformVelocity.getInstance().getLogger()
                    .log(Level.SEVERE, "Failed to save the config file!", e);
        }
    }

    public boolean getBoolean(String path, boolean def) {
        setDefault(path, def);
        return getNode(path).getBoolean();
    }

    public int getInt(String path, int def) {
        setDefault(path, def);
        return getNode(path).getInt();
    }

    public double getDouble(String path, double def) {
        setDefault(path, def);
        return getNode(path).getDouble();
    }

    public String getString(String path, String def) {
        setDefault(path, def);
        return getNode(path).getString(def);
    }

    public List<String> getList(String path, List<String> def) {
        try {
            setDefault(path, def);
            return getNode(path).getList(String.class, def);
        } catch (SerializationException e) {
            FlarePlatformVelocity.getInstance().getLogger()
                    .log(Level.SEVERE, "Failed to read the config file!", e);
        }
        return List.of();
    }

    public void set(String path, Object value) {
        try {
            getNode(path).set(value);
        } catch (SerializationException e) {
            FlarePlatformVelocity.getInstance().getLogger()
                    .log(Level.SEVERE, "Failed to update the config file!", e);
        }
        saveConfig();
    }

    public ConfigurationNode getNode(String path) {
        Preconditions.checkState(config != null, "Config must not be null!");
        return config.node((Object[]) path.split("\\."));
    }
}
