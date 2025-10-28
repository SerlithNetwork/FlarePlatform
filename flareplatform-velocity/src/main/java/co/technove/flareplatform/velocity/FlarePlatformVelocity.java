package co.technove.flareplatform.velocity;

import co.technove.flare.FlareInitializer;
import co.technove.flare.internal.profiling.InitializationException;
import co.technove.flareplatform.common.FlarePlatformConfig;
import co.technove.flareplatform.velocity.utils.PluginLookup;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

public class FlarePlatformVelocity {

    @Nullable
    private static FlarePlatformVelocity instance;
    @Nullable
    private static FlarePlatformConfig config;
    private static boolean shouldRegister = true;
    @Inject
    @Nullable
    private PluginContainer container;
    @Inject
    @Nullable
    private Logger logger;
    @Inject
    @Nullable
    private ProxyServer server;
    @Nullable
    private PluginLookup lookup;

    @Inject
    public FlarePlatformVelocity() {
        instance = this;
    }

    public static FlarePlatformVelocity getInstance() {
        Preconditions.checkState(instance != null, "Instance cannot be null!");
        return instance;
    }

    public static FlarePlatformConfig getFlareConfig() {
        Preconditions.checkState(config != null, "Config cannot be null!");
        return config;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // detect unsupported platforms
        final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (!OS_NAME.contains("linux") && !OS_NAME.contains("mac")) {
            this.getLogger().log(Level.WARNING, "Flare does not support running on " + OS_NAME + ", will not enable!");
            shouldRegister = false;
        }

        try {
            if (shouldRegister) {
                final List<String> warnings = FlareInitializer.initialize();
                this.getLogger().log(Level.WARNING, "Warnings while initializing Flare: " + String.join(", ", warnings));
                // register commands
                CommandManager commandManager = this.getServer().getCommandManager();
                CommandMeta commandMeta = commandManager.metaBuilder("flareprofiler")
                    .aliases("flare", "profiler")
                    .plugin(this)
                    .build();
                BrigadierCommand command = FlareCommand.createBrigadierCommand(this.getServer());
                commandManager.register(commandMeta, command);
                lookup = new PluginLookup(this.getServer());
            }
        } catch (InitializationException e) {
            this.getLogger().log(Level.SEVERE, "Failed to initialize Flare", e);
            this.getServer().getEventManager().unregisterListeners(this); // unregister everything
        }

        instance = this;
        config = new FlarePlatformConfig("plugins/" + this.getContainer().getDescription().getName().orElse(
            "FlarePlatform"), this.getLogger());
        // generate defaults at startup - if omitted it'll just generate them the first time those values get -
        // - accessed so no big deal
        getFlareURI();
        getAccessToken();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (ProfilingManager.isProfiling()) {
            ProfilingManager.stop();
        }
    }

    public ProxyServer getServer() {
        Preconditions.checkState(server != null, "Server cannot be null!");
        return server;
    }

    public PluginContainer getContainer() {
        Preconditions.checkState(container != null, "Plugin container cannot be null!");
        return container;
    }

    public String getVersion() {
        return getContainer().getDescription().getVersion().orElse("unknown");
    }

    public Logger getLogger() {
        Preconditions.checkState(logger != null, "Logger cannot be null!");
        return logger;
    }

    public PluginLookup getPluginLookup() {
        Preconditions.checkState(lookup != null, "Plugin lookup cannot be null!");
        return lookup;
    }

    public URI getFlareURI() {
        return URI.create(getFlareConfig().getString("flare.url", "https://flare.serlith.net"));
    }

    public String getAccessToken() {
        return getFlareConfig().getString("flare.token", "");
    }
}
