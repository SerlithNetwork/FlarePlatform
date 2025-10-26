package co.technove.flareplatform.paper;

import co.technove.flare.FlareInitializer;
import co.technove.flare.internal.profiling.InitializationException;
import co.technove.flareplatform.common.FlarePlatformConfig;
import co.technove.flareplatform.paper.utils.PluginLookup;
import co.technove.flareplatform.paper.utils.ServerConfigurations;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public class FlarePlatform extends JavaPlugin {

    private PluginLookup pluginLookup;
    private static FlarePlatformConfig config;
    private static FlarePlatform instance;
    private static boolean shouldRegister = true;
    public static final boolean IS_FOLIA = detectFolia();

    @Override
    public void onEnable() {
        // detect unsupported platforms
        final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (!OS_NAME.contains("linux") && !OS_NAME.contains("mac")) {
            this.getLogger().log(Level.WARNING, "Flare does not support running on " + OS_NAME + ", will not enable!");
            shouldRegister = false;
        }

        try {
            Class.forName("co.technove.flare.Flare", false, ClassLoader.getSystemClassLoader());
            this.getLogger().log(Level.WARNING, "Your platform already bundles flare on its classpath!");
        } catch (ClassNotFoundException ignored) {}

        try {
            if (shouldRegister) {
                if (IS_FOLIA) this.getLogger().log(Level.INFO, "You're running a Folia based platform. TPS information won't be reported");
                final List<String> warnings = FlareInitializer.initialize();
                this.getLogger().log(Level.WARNING, "Warnings while initializing Flare: " + String.join(", ", warnings));
                this.getLifecycleManager().registerEventHandler(
                        LifecycleEvents.COMMANDS, commands -> {
                            commands.registrar().register(FlareCommand.createCommand(), "Flare profiling commands",
                                    List.of("flare", "profiler"));
                        }
                );
                this.pluginLookup = new PluginLookup();
            }
        } catch (InitializationException e) {
            this.getLogger().log(Level.SEVERE, "Failed to initialize Flare", e);
        }

        instance = this;
        config = new FlarePlatformConfig("plugins/" + this.getPluginMeta().getName(), this.getLogger());

        // dirty hack so those get saved to the config file without starting the profiler
        this.getFlareURI();
        this.getAccessToken();
        this.getServerConfigurations();
        this.getHiddenEntries();
        // dirty hack end
    }

    @Override
    public void onDisable() {
        if (ProfilingManager.isProfiling()) {
            ProfilingManager.stop();
        }
    }

    public static FlarePlatformConfig getFlareConfig() {
        return config;
    }

    public static FlarePlatform getInstance() {
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

    /** internal */
    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    public static boolean isFolia() {
        return IS_FOLIA;
    }
}
