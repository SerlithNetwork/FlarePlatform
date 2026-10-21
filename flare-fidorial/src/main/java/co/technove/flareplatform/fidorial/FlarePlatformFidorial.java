package co.technove.flareplatform.fidorial;

import co.technove.flare.FlareInitializer;
import co.technove.flare.internal.profiling.InitializationException;
import co.technove.flareplatform.fidorial.command.FlareCommand;
import co.technove.flareplatform.fidorial.config.FlareFidorialConfig;
import co.technove.flareplatform.fidorial.manager.ProfilingManager;
import co.technove.flareplatform.fidorial.utils.PluginLookup;
import com.google.common.base.Preconditions;
import fr.fidorial.entity.player.Player;
import fr.fidorial.plugin.Plugin;
import fr.fidorial.plugin.PluginContext;
import fr.fidorial.plugin.PluginMeta;
import lombok.Getter;
import lombok.Setter;
import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.platform.adventure.AdventureConfigHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class FlarePlatformFidorial implements Plugin {

    @Getter
    private @Nullable static FlarePlatformFidorial instance;

    @Getter
    private @Nullable static PluginContext context;

    @Getter
    private static final Component prefix = MiniMessage.miniMessage().deserialize("<gradient:#1A46FF:#63ABFF:#1A46FF>Flare ✈</gradient> <gray>•</gray> ");

    // TODO: Implement faststats error tracking
    /*
    private static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware()
        .ignoreError(IllegalStateException.class, "No AllocTracer symbols found.*")
        .ignoreError(IOException.class, "Error ocurred sending data.*")
        .ignoreError(UserReportableException.class);
     */

    private static boolean shouldRegister = true;

    @Getter
    private @Nullable ConfigHandler configHandler;

    @Setter
    private @Nullable PluginLookup pluginLookup;

    @Override
    public void onLoad(final PluginContext context) {
        FlarePlatformFidorial.context = context;
        FlarePlatformFidorial.instance = this;
    }

    @Override
    public void onEnable() {
        final ConfigHandler configHandler = new AdventureConfigHandler(getLogger(context().meta()), FlarePlatformFidorial.getPrefix());
        this.configHandler = configHandler;

        new FlareFidorialConfig(context().dataFolder(), configHandler).load();

        // detect unsupported platforms
        final String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (!osName.contains("linux") && !osName.contains("mac")) {
            context().logger().warn("Flare does not support running on {}, will not enable!", osName);
            shouldRegister = false;
        }

        // check fidorial version - workaround until `supported-versions` gets added to `fidorial.json`
        final String requiredVersion = "26.4-snapshot-4";
        final String currentVersion = context().server().minecraftVersion();

        final int firstDash = currentVersion.indexOf('-');
        final String suffix = firstDash == -1 ? "" : currentVersion.substring(firstDash + 1);

        final boolean supported = !currentVersion.startsWith("26.2")
            && !currentVersion.startsWith("26.3")
            && (!suffix.toLowerCase(Locale.ROOT).startsWith("snapshot-")
            || Integer.parseInt(currentVersion.substring(currentVersion.lastIndexOf('-') + 1)) >= 4);

        if (!supported) {
            context().logger().warn(
                "FlarePlatform requires Minecraft {} or newer (current: {}), will not enable!",
                requiredVersion, currentVersion
            );
            shouldRegister = false;
        }

        if (!shouldRegister) {
            return;
        }

        try {
            final List<String> warnings = FlareInitializer.initialize();
            if (!warnings.isEmpty()) {
                context().logger().warn("Warnings while initializing Flare: {}", String.join(", ", warnings));
            }
            context().server().commands().register(context().meta(), FlareCommand.createCommand(), Set.of("flare"));
            setPluginLookup(new PluginLookup(context().server()));
        } catch (final InitializationException e) {
            context().logger().error("Failed to initialize Flare", e);
        }
    }

    @Override
    public void onDisable() {
        if (ProfilingManager.isProfiling()) {
            ProfilingManager.stop();
        }
    }

    public void refreshCommands() {
        for (final Player player : context().server().players().online()) {
            player.refreshCommands();
        }
    }

    public PluginContext context() {
        if (context == null) {
            throw new IllegalStateException("Plugin not loaded yet");
        }
        return context;
    }

    public PluginLookup getPluginLookup() {
        Preconditions.checkState(pluginLookup != null, "Plugin lookup cannot be null!");
        return pluginLookup;
    }

    private static Logger getLogger(final PluginMeta meta) {
        return LogManager.getLogManager().getLogger(meta.name());
    }
}
