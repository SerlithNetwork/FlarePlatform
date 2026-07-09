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
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OperatingSystem;

// yuck
public class ProfilingManager {

    private static final FlarePlatformPaper platform = FlarePlatformPaper.getInstance();
    private static final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1, r -> {
        Thread t = new Thread(r);
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
        Flare flare = currentFlare; // copy reference so no need to sync
        if (flare == null) {
            return Duration.ofMillis(0);
        }
        return flare.getCurrentDuration();
    }

    public static synchronized boolean start(ProfileType profileType) throws UserReportableException {
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
            OperatingSystem os = new SystemInfo().getOperatingSystem();

            SystemInfo systemInfo = new SystemInfo();
            HardwareAbstractionLayer hardware = systemInfo.getHardware();

            CentralProcessor processor = hardware.getProcessor();
            CentralProcessor.ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();

            GlobalMemory memory = hardware.getMemory();
            VirtualMemory virtualMemory = memory.getVirtualMemory();

            FlareBuilder builder = new FlareBuilder()
                .withProfileType(profileType)
                .withMemoryProfiling(true)
                .withAuth(FlareAuth.fromTokenAndUrl(FlarePaperConfig.PROFILING.TOKEN, FlarePaperConfig.PROFILING.BACKEND_URL))

                .withFiles(ServerConfigurations.getCleanCopies())
                .withVersion("Primary Version", Bukkit.getName() + " | " + Bukkit.getVersion())
                .withVersion("Bukkit Version", Bukkit.getBukkitVersion())
                .withVersion("Minecraft Version", Bukkit.getMinecraftVersion())

                .withGraphCategories(CustomCategories.PERF)
                .withClassIdentifier(platform.getPluginLookup()::getPluginForClass)

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
                    } catch (Throwable t) {
                        platform.getLogger().log(Level.WARNING, "Error occurred stopping Flare", t);
                    } finally {
                        currentTask = null;
                    }

                    String profilingUri = FlareCommand.PROFILING_URI;
                    FlareCommand.broadcastPrefixed(
                        Component.text("An exception happened and profiling has stopped", EXCEPTION_COLOR),
                        Component.text(profilingUri, HEX).clickEvent(ClickEvent.openUrl(profilingUri))
                    );
                });

            IScheduler bukkitScheduler, foliaScheduler;
            try {
                bukkitScheduler = new BukkitSchedulerImpl();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                bukkitScheduler = new NoOpSchedulerImpl();
            }
            try {
                foliaScheduler = new FoliaSchedulerImpl();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                foliaScheduler = new NoOpSchedulerImpl();
            }

            List<Collector> baseCollectors = List.of(new TPSCollector(), new GCEventCollector(), new StatCollector(), new WorldCountCollector(), new PaperThreadCollector(bukkitScheduler, foliaScheduler));
            List<Collector> extraCollectors = new ArrayList<>();

            if (FlarePlatformPaper.IS_PWT) {
                extraCollectors.add(WorldTpsCollector.create());
            } else if (FlarePlatformPaper.IS_CANVAS) {
                extraCollectors.add(RegionTpsCollector.create());
                extraCollectors.add(WorldRegionCountCollector.create());
            }

            builder.withCollectors(Stream.concat(baseCollectors.stream(), extraCollectors.stream()).toArray(Collector[]::new));
            currentFlare = builder.build();
        } catch (IOException e) {
            platform.getLogger().log(Level.WARNING, "Failed to read configuration files:", e);
            throw new UserReportableException("Failed to load configuration files, check logs for further details.");
        }

        try {
            currentFlare.start();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getScheduler().run(platform, task -> player.updateCommands(), null);
            }
        } catch (IllegalStateException e) {
            platform.getLogger().log(Level.WARNING, "Error starting Flare:", e);
            throw new UserReportableException("Failed to start Flare, check logs for further details.");
        }

        currentTask = scheduler.schedule(ProfilingManager::stop, 15, TimeUnit.MINUTES);
        // platform.getLogger().log(Level.INFO, "Flare has been started: " + getProfilingUri());
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
        String profilingUri = ProfilingManager.getProfilingUri();
        FlareCommand.broadcastPrefixed(
            Component.text("Profiling has been stopped.", MAIN_COLOR),
            Component.text(profilingUri, HEX).clickEvent(ClickEvent.openUrl(profilingUri))
        );
        try {
            currentFlare.stop();
        } catch (IllegalStateException e) {
            platform.getLogger().log(Level.WARNING, "Error occurred stopping Flare", e);
        }
        currentFlare = null;

        try {
            if (currentTask != null) {
                currentTask.cancel(true);
            }
        } catch (Throwable t) {
            platform.getLogger().log(Level.WARNING, "Error occurred stopping Flare", t);
        }
        currentTask = null;

        return true;
    }

}
