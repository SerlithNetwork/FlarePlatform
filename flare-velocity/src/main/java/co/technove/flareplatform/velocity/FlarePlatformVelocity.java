package co.technove.flareplatform.velocity;

import co.technove.flare.FlareInitializer;
import co.technove.flare.internal.profiling.InitializationException;
import co.technove.flareplatform.common.config.FlareConfig;
import co.technove.flareplatform.velocity.command.FlareCommand;
import co.technove.flareplatform.velocity.manager.ProfilingManager;
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
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.platform.adventure.AdventureConfigHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.Nullable;

public class FlarePlatformVelocity {

    @Getter
    private static @Nullable FlarePlatformVelocity instance;
    private static boolean shouldRegister = true;

    @Getter
    private final PluginContainer container;
    @Getter
    private final Logger logger;
    @Getter
    private final ProxyServer server;
    @Getter
    private final Path dataDirectory;

    @Nullable
    private PluginLookup lookup;

    @Getter
    private final Component prefix = MiniMessage.miniMessage().deserialize("<gradient:#1A46FF:#63ABFF:#1A46FF>Flare ✈</gradient> <gray>•</gray> ");
    private final ConfigHandler configHandler;

    @Inject
    public FlarePlatformVelocity(
        PluginContainer container,
        Logger logger,
        ProxyServer server,
        @DataDirectory Path dataDirectory
    ) {
        this.container = container;
        this.logger = logger;
        this.server = server;
        this.dataDirectory = dataDirectory;
        this.configHandler = new AdventureConfigHandler(this.logger, this.prefix);
        instance = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        new FlareConfig(dataDirectory, this.configHandler).load();

        // detect unsupported platforms
        final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (!OS_NAME.contains("linux") && !OS_NAME.contains("mac")) {
            this.logger.log(Level.WARNING, "Flare does not support running on " + OS_NAME + ", will not enable!");
            shouldRegister = false;
        }

        if (!shouldRegister) {
            return;
        }

        try {
            final List<String> warnings = FlareInitializer.initialize();
            if (!warnings.isEmpty()) {
                this.logger.log(Level.WARNING, "Warnings while initializing Flare: " + String.join(", ", warnings));
            }
            // register commands
            CommandManager commandManager = this.getServer().getCommandManager();
            CommandMeta commandMeta = commandManager.metaBuilder("vflareprofiler")
                .aliases("vflare", "vprofiler")
                .plugin(this)
                .build();
            BrigadierCommand command = FlareCommand.createBrigadierCommand(this.getServer());
            commandManager.register(commandMeta, command);
            lookup = new PluginLookup(this.getServer());
        } catch (InitializationException e) {
            this.logger.log(Level.SEVERE, "Failed to initialize Flare", e);
            this.getServer().getEventManager().unregisterListeners(this); // unregister everything
        }

    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (ProfilingManager.isProfiling()) {
            ProfilingManager.stop();
        }
    }

    public PluginLookup getPluginLookup() {
        Preconditions.checkState(lookup != null, "Plugin lookup cannot be null!");
        return lookup;
    }

}
