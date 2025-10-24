package co.technove.flareplugin;

import co.technove.flare.FlareInitializer;
import co.technove.flare.internal.profiling.InitializationException;
import co.technove.flareplugin.utils.PluginLookup;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;

public class FlarePlugin extends JavaPlugin {

    private ProfilingManager profilingManager;
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

        /*
        this.getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS, commands -> {
                    commands.registrar().register(FlareCommand.createCommand());
                }
        );
        */

        this.profilingManager = new ProfilingManager(this);

        this.pluginLookup = new PluginLookup();
        this.getServer().getPluginManager().registerEvents(this.pluginLookup, this);

        //this.getCommand("flare").setExecutor(new FlareCommand(this)); - migrating to brigadier
        config.saveConfig();
    }

    @Override
    public void onDisable() {
        if (this.profilingManager.isProfiling()) {
            this.profilingManager.stop();
        }
    }

    public static FlareConfig getFlareConfig() {
        return config;
    }

    public static FlarePlugin getInstance() {
        return instance;
    }

    public URI getFlareURI() {
        return URI.create(getFlareConfig().getString("flare.url", "flare.serlith.net"));
    }

    public String getAccessToken() {
        return getFlareConfig().getString("flare.token", "");
    }

    public ProfilingManager getProfilingManager() {
        return profilingManager;
    }

    public PluginLookup getPluginLookup() {
        return pluginLookup;
    }
}
