package co.technove.flareplatform.fidorial.manager;

import co.technove.flare.Flare;
import co.technove.flare.FlareAuth;
import co.technove.flare.FlareBuilder;
import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import co.technove.flareplatform.fidorial.collectors.RegionTpsCollector;
import co.technove.flareplatform.common.CustomCategories;
import co.technove.flareplatform.common.collectors.GCEventCollector;
import co.technove.flareplatform.common.collectors.StatCollector;
import co.technove.flareplatform.common.scheduler.IScheduler;
import co.technove.flareplatform.fidorial.FlarePlatformFidorial;
import co.technove.flareplatform.fidorial.collectors.FidorialThreadCollector;
import co.technove.flareplatform.fidorial.collectors.WorldCountCollector;
import co.technove.flareplatform.fidorial.command.FlareCommand;
import co.technove.flareplatform.fidorial.config.FlareFidorialConfig;
import co.technove.flareplatform.fidorial.scheduler.NoOpSchedulerImpl;
import co.technove.flareplatform.fidorial.utils.ServerConfigurations;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import fr.fidorial.Server;
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
                if (!FlareFidorialConfig.PROFILING.VIEWER_URL.isBlank()) {
                    return s.replace(FlareFidorialConfig.PROFILING.BACKEND_URL.toString(), FlareFidorialConfig.PROFILING.VIEWER_URL);
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

    private static FlarePlatformFidorial platform() {
        final FlarePlatformFidorial platform = FlarePlatformFidorial.getInstance();
        Preconditions.checkState(platform != null, "Plugin has not been initialized!");
        return platform;
    }

    private static Server server() {
        return platform().context().server();
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
                .withAuth(FlareAuth.fromTokenAndUrl(FlareFidorialConfig.PROFILING.TOKEN, FlareFidorialConfig.PROFILING.BACKEND_URL))

                .withFiles(ServerConfigurations.getCleanCopies())
                .withVersion("Primary Version", server().brandName() + " | " + server().buildSpecification().version())
                .withVersion("Fidorial Version", server().buildSpecification().apiVersion())
                .withVersion("Minecraft Version", server().buildSpecification().minecraftVersion().name())

                .withGraphCategories(CustomCategories.PERF)
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
                        platform().context().logger().warn("Error occurred stopping Flare", t);
                    } finally {
                        currentTask = null;
                    }
                    FlareCommand.broadcastException();
                });

            final IScheduler scheduler = new NoOpSchedulerImpl();

            builder.withCollectors(new RegionTpsCollector(server()), new GCEventCollector(), new StatCollector(), new WorldCountCollector(server()), new FidorialThreadCollector(scheduler));

            currentFlare = builder.build();
        } catch (final IOException e) {
            platform().context().logger().warn("Failed to read configuration files:", e);
            throw new UserReportableException("Failed to load configuration files, check logs for further details.");
        }

        try {
            currentFlare.start();
            platform().refreshCommands();
        } catch (final IllegalStateException e) {
            platform().context().logger().warn("Error starting Flare:", e);
            throw new UserReportableException("Failed to start Flare, check logs for further details.");
        }

        currentTask = scheduler.schedule(ProfilingManager::stop, 15, TimeUnit.MINUTES);
        // platform().context().logger().info("Flare has been started: " + getProfilingUri());
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
            platform().refreshCommands();
        } catch (final IllegalStateException e) {
            platform().context().logger().warn("Error occurred stopping Flare", e);
        }
        currentFlare = null;

        try {
            if (currentTask != null) {
                currentTask.cancel(true);
            }
        } catch (final Throwable t) {
            platform().context().logger().warn("Error occurred stopping Flare", t);
        }
        currentTask = null;

        return true;
    }
}
