package co.technove.flareplatform.paper;

import co.technove.flare.FlareInitializer;
import co.technove.flare.internal.profiling.InitializationException;
import co.technove.flareplatform.paper.utils.PluginLookup;
import co.technove.flareplatform.paper.utils.ServerConfigurations;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;

public class FlarePlugin extends JavaPlugin {

    private PluginLookup pluginLookup;
    private static FlareConfig config;
    private static FlarePlugin instance;

    @Override
    public void onEnable() {
        try {
            Class.forName("co.technove.flare.Flare", false, ClassLoader.getSystemClassLoader());
            this.getLogger().log(Level.WARNING, "Your platform already bundles flare on its classpath!");
        } catch (ReflectiveOperationException ignored) {}

        try {
            final List<String> warnings = FlareInitializer.initialize();
            this.getLogger().log(Level.WARNING, "Warnings while initializing Flare: " + String.join(", ", warnings));
        } catch (InitializationException e) {
            this.getLogger().log(Level.SEVERE, "Failed to initialize Flare", e);
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        instance = this;
        config = new FlareConfig();

        this.getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS, commands -> {
                    commands.registrar().register(FlareCommand.createCommand(), "Flare profiling commands");
                }
        );

        this.pluginLookup = new PluginLookup();
        this.getServer().getPluginManager().registerEvents(this.pluginLookup, this);

        // dirty hack so those get saved to the config file without starting the profiler
        this.getServerConfigurations();
        this.getHiddenEntries();
        this.getFlareURI();
        this.getAccessToken();
        // dirty hack end

        config.saveConfig();
    }

    @Override
    public void onDisable() {
        if (ProfilingManager.isProfiling()) {
            ProfilingManager.stop();
        }
    }

    public static FlareConfig getFlareConfig() {
        return config;
    }

    public static FlarePlugin getInstance() {
        return instance;
    }

    public URI getFlareURI() {
        return URI.create(getFlareConfig().getString("flare.url", "https://flare.serlith.net"));
    }

    public String getAccessToken() {
        return getFlareConfig().getString("flare.token", "");
    }

    private List<String> getServerConfigurations() {
        return ServerConfigurations.configurationFiles;
    }

    private List<String> getHiddenEntries() {
        return ServerConfigurations.hiddenEntries;
    }

    public PluginLookup getPluginLookup() {
        return pluginLookup;
    }
}
