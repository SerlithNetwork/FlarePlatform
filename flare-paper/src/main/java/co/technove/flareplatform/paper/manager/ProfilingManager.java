package co.technove.flareplatform.paper.manager;

import co.technove.flare.Flare;
import co.technove.flare.FlareAuth;
import co.technove.flare.FlareBuilder;
import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import co.technove.flare.live.Collector;
import co.technove.flareplatform.canvas.collectors.WorldRegionCountCollector;
import co.technove.flareplatform.canvas.collectors.RegionTpsCollector;
import co.technove.flareplatform.common.CustomCategories;
import co.technove.flareplatform.common.collectors.GCEventCollector;
import co.technove.flareplatform.common.collectors.StatCollector;
import co.technove.flareplatform.common.scheduler.IScheduler;
import co.technove.flareplatform.fish.collectors.WorldTpsCollector;
import co.technove.flareplatform.paper.FlarePlatformPaper;
import co.technove.flareplatform.paper.collectors.PaperThreadCollector;
import co.technove.flareplatform.paper.collectors.TPSCollector;
import co.technove.flareplatform.paper.collectors.WorldCountCollector;
import co.technove.flareplatform.paper.command.FlareCommand;
import co.technove.flareplatform.paper.config.FlarePaperConfig;
import co.technove.flareplatform.paper.scheduler.BukkitSchedulerImpl;
import co.technove.flareplatform.paper.scheduler.FoliaSchedulerImpl;
import co.technove.flareplatform.paper.scheduler.NoOpSchedulerImpl;
import co.technove.flareplatform.paper.utils.ServerConfigurations;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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
                if (!FlarePaperConfig.PROFILING.VIEWER_URL.isBlank()) {
                    return s.replace(FlarePaperConfig.PROFILING.BACKEND_URL.toString(), FlarePaperConfig.PROFILING.VIEWER_URL);
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

    private static FlarePlatformPaper platform() {
        final FlarePlatformPaper platform = FlarePlatformPaper.getInstance();
        Preconditions.checkState(platform != null, "Plugin has not been initialized!");
        return platform;
    }

    public static synchronized boolean start(final ProfileType profileType) throws UserReportableException {
        if (currentFlare != null && !currentFlare.isRunning()) {
            currentFlare = null; // errored out
        }
        if (ProfilingManager.isProfiling()) {
            return false;
        }
        if (Bukkit.isPrimaryThread()) {
            throw new UserReportableException("Profiles should be started off-thread");
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
                .withAuth(FlareAuth.fromTokenAndUrl(FlarePaperConfig.PROFILING.TOKEN, FlarePaperConfig.PROFILING.BACKEND_URL))

                .withFiles(ServerConfigurations.getCleanCopies())
                .withVersion("Primary Version", Bukkit.getName() + " | " + Bukkit.getVersion())
                .withVersion("Bukkit Version", Bukkit.getBukkitVersion())
                .withVersion("Minecraft Version", Bukkit.getMinecraftVersion())

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
                        platform().getLogger().log(Level.WARNING, "Error occurred stopping Flare", t);
                    } finally {
                        currentTask = null;
                    }
                    FlareCommand.broadcastException();
                });

            builder.withCollectors(collectors().toArray(Collector[]::new));
            currentFlare = builder.build();
        } catch (final IOException e) {
            platform().getLogger().log(Level.WARNING, "Failed to read configuration files:", e);
            throw new UserReportableException("Failed to load configuration files, check logs for further details.");
        }

        try {
            currentFlare.start();
            platform().refreshCommands();
        } catch (final IllegalStateException e) {
            platform().getLogger().log(Level.WARNING, "Error starting Flare:", e);
            throw new UserReportableException("Failed to start Flare, check logs for further details.");
        }

        currentTask = scheduler.schedule(ProfilingManager::stop, 15, TimeUnit.MINUTES);
        // platform().getLogger().log(Level.INFO, "Flare has been started: " + getProfilingUri());
        return true;
    }

    private static List<Collector> collectors() {
        IScheduler bukkitScheduler, foliaScheduler;
        try {
            bukkitScheduler = new BukkitSchedulerImpl();
        } catch (final NoSuchFieldException | IllegalAccessException ignored) {
            bukkitScheduler = new NoOpSchedulerImpl();
        }
        try {
            foliaScheduler = new FoliaSchedulerImpl();
        } catch (final NoSuchFieldException | IllegalAccessException ignored) {
            foliaScheduler = new NoOpSchedulerImpl();
        }

        final List<Collector> collectors = new ArrayList<>(List.of(
            new TPSCollector(), new GCEventCollector(), new StatCollector(),
            new WorldCountCollector(platform()), new PaperThreadCollector(bukkitScheduler, foliaScheduler)));
        if (FlarePlatformPaper.IS_PWT) {
            collectors.add(WorldTpsCollector.create());
        } else if (FlarePlatformPaper.IS_CANVAS) {
            collectors.add(RegionTpsCollector.create());
            collectors.add(WorldRegionCountCollector.create());
        }
        return collectors;
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
