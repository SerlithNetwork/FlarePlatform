package co.technove.flareplatform.velocity;

import co.technove.flare.FlareInitializer;
import co.technove.flare.internal.profiling.InitializationException;
import co.technove.flareplatform.FlarePlatformConfig;
import co.technove.flareplatform.velocity.utils.PluginLookup;
import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FlarePlatform {

    private static FlarePlatform instance;

    @Inject
    private PluginContainer container;

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    private PluginLookup lookup;

    private static FlarePlatformConfig config;

    private static boolean shouldRegister = true;

    @Inject
    public FlarePlatform() {
        instance = this;
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
                this.logger.log(Level.WARNING, "Warnings while initializing Flare: " + String.join(", ", warnings));
                // register commands
                CommandManager commandManager = server.getCommandManager();
                CommandMeta commandMeta = commandManager.metaBuilder("flareprofiler")
                        .aliases("flare", "profiler")
                        .plugin(this)
                        .build();
                BrigadierCommand command = FlareCommand.createBrigadierCommand(server);
                commandManager.register(commandMeta, command);
                lookup = new PluginLookup(server);
            }
        } catch (InitializationException e) {
            this.logger.log(Level.SEVERE, "Failed to initialize Flare", e);
            server.getEventManager().unregisterListeners(this); // unregister everything
        }

        instance = this;
        config = new FlarePlatformConfig("plugins/" + this.container.getDescription().getName().orElse(
                "FlarePlatform"), this.getLogger());
        // generate defaults at startup - if omitted it'll just generate them the first time those values get -
        // - accessed so no big deal
        getFlareURI();
        getAccessToken();
    }

    public static FlarePlatform getInstance() {
        return instance;
    }

    public ProxyServer getServer() {
        return server;
    }

    public String getVersion() {
        return container.getDescription().getVersion().orElse("unknown");
    }

    public Logger getLogger() {
        return logger;
    }

    public void refreshCommands() {
        CommandManager commandManager = server.getCommandManager();
        commandManager.unregister(commandManager.metaBuilder("flareprofiler").build());
        CommandMeta commandMeta = commandManager.metaBuilder("flareprofiler")
                .aliases("flare", "profiler")
                .plugin(FlarePlatform.getInstance())
                .build();
        BrigadierCommand command = FlareCommand.createBrigadierCommand(server);
        commandManager.register(commandMeta, command);
    }

    public PluginLookup getPluginLookup() {
        return lookup;
    }

    public static FlarePlatformConfig getFlareConfig() {
        return config;
    }

    public URI getFlareURI() {
        return URI.create(getFlareConfig().getString("flare.url", "https://flare.serlith.net"));
    }

    public String getAccessToken() {
        return getFlareConfig().getString("flare.token", "");
    }
}
