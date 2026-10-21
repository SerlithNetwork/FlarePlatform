package co.technove.flareplatform.velocity.manager;

import co.technove.flare.Flare;
import co.technove.flare.FlareAuth;
import co.technove.flare.FlareBuilder;
import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import co.technove.flareplatform.common.CustomCategories;
import co.technove.flareplatform.common.collectors.GCEventCollector;
import co.technove.flareplatform.common.collectors.StatCollector;
import co.technove.flareplatform.velocity.FlarePlatformVelocity;
import co.technove.flareplatform.velocity.collectors.ProxyCountCollector;
import co.technove.flareplatform.velocity.collectors.VelocityThreadCollector;
import co.technove.flareplatform.velocity.command.FlareCommand;
import co.technove.flareplatform.velocity.config.FlareVelocityConfig;
import co.technove.flareplatform.velocity.utils.ServerConfigurations;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.jspecify.annotations.Nullable;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OperatingSystem;

// yuck
public final class ProfilingManager {

    private static final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1, r -> {
        final Thread t = new Thread(r);
        t.setName("Flare Profiling Manager Thread");
        return t;
    });

    private static final TextColor MAIN_COLOR = TextColor.color(106, 126, 218);
    private static final TextColor EXCEPTION_COLOR = TextColor.color(218, 144, 147);
    private static final TextColor HEX = TextColor.color(227, 234, 234);

    public static @Nullable ScheduledFuture<?> currentTask;
    private static @Nullable Flare currentFlare;

    public static synchronized boolean isProfiling() {
        return currentFlare != null && currentFlare.isRunning();
    }

    public static synchronized String getProfilingUri() {
        Preconditions.checkState(currentFlare != null, "Flare cannot be null!");
        return currentFlare.getURI()
            .map(URI::toString)
            .map(s -> {
                if (!FlareVelocityConfig.PROFILING.VIEWER_URL.isBlank()) {
                    return s.replace(FlareVelocityConfig.PROFILING.BACKEND_URL.toString(), FlareVelocityConfig.PROFILING.VIEWER_URL);
                }
                return s;
            })
            .orElse("Flare is not running");
    }

    public static Duration getTimeRan() {
        final Flare flare = currentFlare; // copy reference so no need to sync
        if (flare == null) {
            return Duration.ofMillis(0);
        }
        return flare.getCurrentDuration();
    }

    private static FlarePlatformVelocity platform() {
        final FlarePlatformVelocity platform = FlarePlatformVelocity.getInstance();
        Preconditions.checkState(platform != null, "Plugin has not been initialized!");
        return platform;
    }

    private static ProxyServer server() {
        return platform().getServer();
    }

    public static synchronized boolean start(final ProfileType profileType) throws UserReportableException {
        if (currentFlare != null && !currentFlare.isRunning()) {
            currentFlare = null; // errored out
        }
        if (ProfilingManager.isProfiling()) {
            return false;
        }

        try {
            final OperatingSystem os = new SystemInfo().getOperatingSystem();

            final SystemInfo systemInfo = new SystemInfo();
            final HardwareAbstractionLayer hardware = systemInfo.getHardware();

            final CentralProcessor processor = hardware.getProcessor();
            final CentralProcessor.ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();

            final GlobalMemory memory = hardware.getMemory();
            final VirtualMemory virtualMemory = memory.getVirtualMemory();

            final FlareBuilder builder = new FlareBuilder()
                .withProfileType(profileType)
                .withMemoryProfiling(true)
                .withAuth(FlareAuth.fromTokenAndUrl(FlareVelocityConfig.PROFILING.TOKEN,
                    FlareVelocityConfig.PROFILING.BACKEND_URL))

                .withFiles(ServerConfigurations.getCleanCopies())
                // dirty hacks for our flare viewer
                .withVersion("Primary Version",
                    server().getVersion().getName() + " | " + server().getVersion().getVersion())
                .withVersion("Velocity Version",
                    server().getVersion().getVersion())

                .withGraphCategories(CustomCategories.PERF)
                .withCollectors(new GCEventCollector(), new StatCollector(), new VelocityThreadCollector(), new ProxyCountCollector(server()))
                .withClassIdentifier(platform().getPluginLookup()::getPluginForClass)

                .withHardware(new FlareBuilder.HardwareBuilder()
                    .setCoreCount(processor.getPhysicalProcessorCount())
                    .setThreadCount(processor.getLogicalProcessorCount())
                    .setCpuModel(processorIdentifier.getName())
                    .setCpuFrequency(processor.getMaxFreq())

                    .setTotalMemory(memory.getTotal())
                    .setTotalSwap(virtualMemory.getSwapTotal())
                    .setTotalVirtual(virtualMemory.getVirtualMax())
                )

                .withOperatingSystem(new FlareBuilder.OperatingSystemBuilder()
                    .setManufacturer(os.getManufacturer())
                    .setFamily(os.getFamily())
                    .setVersion(os.getVersionInfo().toString())
                    .setBitness(os.getBitness())
                )

                .withExceptionRunnable(() -> {
                    try {
                        if (currentTask != null) {
                            currentTask.cancel(true);
                        }
                    } catch (final Throwable t) {
                        platform().getLogger().log(Level.WARNING, "Error occurred stopping Flare", t);
                    } finally {
                        currentTask = null;
                    }
                    FlareCommand.broadcastException();
                });

            currentFlare = builder.build();
        } catch (final IOException e) {
            platform().getLogger().log(Level.WARNING, "Failed to read configuration files:", e);
            throw new UserReportableException("Failed to load configuration files, check logs for further details.");
        }
        try {
            currentFlare.start();
        } catch (final IllegalStateException e) {
            platform().getLogger().log(Level.WARNING, "Error starting Flare:", e);
            throw new UserReportableException("Failed to start Flare, check logs for further details.");
        }

        currentTask = scheduler.schedule(ProfilingManager::stop, 15, TimeUnit.MINUTES);
        // platform().getLogger().log(Level.INFO, "Flare has been started: " + getProfilingUri());
        return true;
    }

    public static synchronized boolean stop() {
        if (!isProfiling()) {
            return false;
        }
        if (currentFlare != null && !currentFlare.isRunning()) {
            currentFlare = null;
            return true;
        }
        final String profilingUri = ProfilingManager.getProfilingUri();
        FlareCommand.broadcastPrefixed(
            Component.text("Profiling has been stopped.", MAIN_COLOR),
            Component.text(profilingUri, HEX).clickEvent(ClickEvent.openUrl(profilingUri))
        );
        try {
            currentFlare.stop();
        } catch (final IllegalStateException e) {
            platform().getLogger().log(Level.WARNING, "Error occurred stopping Flare", e);
        }
        currentFlare = null;

        try {
            if (currentTask != null) {
                currentTask.cancel(true);
            }
        } catch (final Throwable t) {
            platform().getLogger().log(Level.WARNING, "Error occurred stopping Flare", t);
        }
        currentTask = null;

        return true;
    }
}
