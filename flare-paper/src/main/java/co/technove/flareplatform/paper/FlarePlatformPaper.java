package co.technove.flareplatform.paper;

import co.technove.flare.FlareInitializer;
import co.technove.flare.internal.profiling.InitializationException;
import co.technove.flareplatform.common.config.FlareConfig;
import co.technove.flareplatform.paper.command.FlareCommand;
import co.technove.flareplatform.paper.manager.ProfilingManager;
import co.technove.flareplatform.paper.utils.PluginLookup;
import co.technove.flareplatform.paper.utils.ServerListener;
import com.google.common.base.Preconditions;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;
import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.platform.adventure.AdventureConfigHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import static org.bukkit.event.HandlerList.unregisterAll;

public class FlarePlatformPaper extends JavaPlugin {

    @Getter
    private static FlarePlatformPaper instance;
    @Getter
    private static final Component prefix = MiniMessage.miniMessage().deserialize("<gradient:#1A46FF:#63ABFF:#1A46FF>Flare ✈</gradient> <gray>•</gray> ");

    public static final boolean IS_FOLIA = detectFolia();

    private static boolean shouldRegister = true;

    @Getter
    private final ConfigHandler configHandler = new AdventureConfigHandler(this.getLogger(), FlarePlatformPaper.getPrefix());

    @Setter
    private @Nullable PluginLookup pluginLookup;


    /**
     * internal
     */
    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        new FlareConfig(this.getDataPath(), this.getConfigHandler()).load();

        // detect unsupported platforms
        final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (!OS_NAME.contains("linux") && !OS_NAME.contains("mac")) {
            this.getSLF4JLogger().warn("Flare does not support running on {}, will not enable!", OS_NAME);
            shouldRegister = false;
        }

        if (!shouldRegister) {
            return;
        }

        try {
            Class.forName("co.technove.flare.Flare", false, ClassLoader.getSystemClassLoader());
            this.getSLF4JLogger().warn("Your platform already bundles flare on its classpath!");
        } catch (ClassNotFoundException ignored) {
        }

        try {
            if (IS_FOLIA) {
                this.getSLF4JLogger().info("You're running a Folia based platform. TPS information won't be reported.");
            }
            final List<String> warnings = FlareInitializer.initialize();
            if (!warnings.isEmpty()) {
                this.getSLF4JLogger().warn("Warnings while initializing Flare: {}", String.join(", ", warnings));
            }
            this.getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS, commands -> {
                    commands.registrar().register(FlareCommand.createCommand(), "Flare profiling commands",
                        List.of("flare", "profiler"));
                }
            );
            new ServerListener(this);
        } catch (InitializationException e) {
            this.getSLF4JLogger().error("Failed to initialize Flare", e);
            unregisterAll(this);
        }

    }

    @Override
    public void onDisable() {
        if (ProfilingManager.isProfiling()) {
            ProfilingManager.stop();
        }
    }

    public PluginLookup getPluginLookup() {
        Preconditions.checkState(pluginLookup != null, "Plugin lookup cannot be null!");
        return pluginLookup;
    }
}
