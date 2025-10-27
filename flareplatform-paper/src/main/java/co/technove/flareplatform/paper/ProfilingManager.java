package co.technove.flareplatform.paper;

import co.technove.flare.Flare;
import co.technove.flare.FlareAuth;
import co.technove.flare.FlareBuilder;
import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import co.technove.flareplatform.common.CustomCategories;
import co.technove.flareplatform.common.collectors.GCEventCollector;
import co.technove.flareplatform.common.collectors.StatCollector;
import co.technove.flareplatform.paper.collectors.TPSCollector;
import co.technove.flareplatform.paper.utils.ServerConfigurations;
import com.google.common.base.Preconditions;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OperatingSystem;

// yuck
@NullMarked
public class ProfilingManager {

    private static final AsyncScheduler scheduler = FlarePlatform.getInstance().getServer().getAsyncScheduler();
    public @Nullable
    static ScheduledTask currentTask;
    private @Nullable
    static Flare currentFlare;

    public static synchronized boolean isProfiling() {
        return currentFlare != null && currentFlare.isRunning();
    }

    public static synchronized String getProfilingUri() {
        Preconditions.checkState(currentFlare != null, "Flare cannot be null!");
        return currentFlare.getURI().map(URI::toString).orElse("Flare is not running");
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
        if (isProfiling()) {
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
                .withAuth(FlareAuth.fromTokenAndUrl(FlarePlatform.getInstance().getAccessToken(), FlarePlatform.getInstance().getFlareURI()))

                .withFiles(ServerConfigurations.getCleanCopies())
                .withVersion("Primary Version", Bukkit.getName() + " | " + Bukkit.getVersion())
                .withVersion("Bukkit Version", Bukkit.getBukkitVersion())
                .withVersion("Minecraft Version", Bukkit.getMinecraftVersion())

                .withGraphCategories(CustomCategories.PERF)
                .withCollectors(new TPSCollector(), new GCEventCollector(), new StatCollector())
                .withClassIdentifier(FlarePlatform.getInstance().getPluginLookup()::getPluginForClass)

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
                );

            currentFlare = builder.build();
        } catch (IOException e) {
            FlarePlatform.getInstance().getLogger().log(Level.WARNING, "Failed to read configuration files:", e);
            throw new UserReportableException("Failed to load configuration files, check logs for further details.");
        }

        try {
            currentFlare.start();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getScheduler().run(FlarePlatform.getInstance(), task -> player.updateCommands(), null);
            }
        } catch (IllegalStateException e) {
            FlarePlatform.getInstance().getLogger().log(Level.WARNING, "Error starting Flare:", e);
            throw new UserReportableException("Failed to start Flare, check logs for further details.");
        }

        currentTask = scheduler.runDelayed(FlarePlatform.getInstance(),
            task -> ProfilingManager.stop(),
            15L,
            TimeUnit.MINUTES);
        FlarePlatform.getInstance().getLogger().log(Level.INFO, "Flare has been started: " + getProfilingUri());
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
        FlarePlatform.getInstance().getLogger().log(Level.INFO, "Flare has been stopped: " + getProfilingUri());
        try {
            currentFlare.stop();
            if (!FlarePlatform.getInstance().getServer().isStopping()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.getScheduler().run(FlarePlatform.getInstance(), task -> player.updateCommands(), null);
                }
            }
        } catch (IllegalStateException e) {
            FlarePlatform.getInstance().getLogger().log(Level.WARNING, "Error occurred stopping Flare", e);
        }
        currentFlare = null;

        try {
            if (currentTask != null) {
                currentTask.cancel();
            }
        } catch (Throwable t) {
            FlarePlatform.getInstance().getLogger().log(Level.WARNING, "Error occurred stopping Flare", t);
        }
        currentTask = null;

        return true;
    }

}
