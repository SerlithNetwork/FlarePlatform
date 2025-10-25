package co.technove.flareplatform.velocity;

import co.technove.flare.FlareInitializer;
import co.technove.flare.internal.profiling.InitializationException;
import co.technove.flareplatform.velocity.utils.PluginLookup;
import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Plugin(
        id = "flareplatformvelocity",
        name = "FlarePlatformVelocity",
        version = "2.0.0",
        description = "Profile your proxy with flare",
        authors = "PaulBGD, SerlithNetwork"
)
public class FlarePlatformVelocity {

    private static FlarePlatformVelocity instance;

    @Inject
    private PluginContainer container;

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer server;

    private PluginLookup lookup;

    private static FlareConfig config;

    @Inject
    public FlarePlatformVelocity() {
        instance = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
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
            instance = this;
            config = new FlareConfig("plugins/" + this.container.getDescription().getName().orElse(
                    "FlarePlatformVelocity"));
        } catch (InitializationException e) {
            this.logger.log(Level.SEVERE, "Failed to initialize Flare", e);
            server.getEventManager().unregisterListeners(this); // unregister everything
        }
    }

    public static FlarePlatformVelocity getInstance() {
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
                .plugin(FlarePlatformVelocity.getInstance())
                .build();
        BrigadierCommand command = FlareCommand.createBrigadierCommand(server);
        commandManager.register(commandMeta, command);
    }

    public PluginLookup getPluginLookup() {
        return lookup;
    }

    public static FlareConfig getFlareConfig() {
        return config;
    }

    public URI getFlareURI() {
        return URI.create(getFlareConfig().getString("flare.url", "https://flare.serlith.net"));
    }

    public String getAccessToken() {
        return getFlareConfig().getString("flare.token", "");
    }
}
