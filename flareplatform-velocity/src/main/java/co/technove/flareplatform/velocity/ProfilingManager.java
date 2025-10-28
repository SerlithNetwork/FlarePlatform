package co.technove.flareplatform.velocity;

import co.technove.flare.Flare;
import co.technove.flare.FlareAuth;
import co.technove.flare.FlareBuilder;
import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import co.technove.flareplatform.common.CustomCategories;
import co.technove.flareplatform.common.collectors.GCEventCollector;
import co.technove.flareplatform.common.collectors.StatCollector;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.jspecify.annotations.Nullable;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OperatingSystem;

// yuck
public class ProfilingManager {

    private static final FlarePlatformVelocity platform = FlarePlatformVelocity.getInstance();
    private static final Scheduler scheduler = platform.getServer().getScheduler();
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
                .withAuth(FlareAuth.fromTokenAndUrl(platform.getAccessToken(),
                    platform.getFlareURI()))

                // dirty hacks for our flare viewer
                .withVersion("Primary Version",
                    platform.getServer().getVersion().getName() + " | " + platform.getServer().getVersion().getVendor())
                .withVersion("Bukkit Version",
                    platform.getServer().getVersion().getName() + " " + platform.getServer().getVersion().getVersion())
                .withVersion("Minecraft Version",
                    platform.getServer().getVersion().getVersion())

                .withGraphCategories(CustomCategories.PERF)
                .withCollectors(new GCEventCollector(), new StatCollector())
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
                );

            currentFlare = builder.build();
        } catch (RuntimeException e) {
            platform.getLogger().log(Level.WARNING, "Error building the Flare instance:", e);
            throw new UserReportableException("Failed to build Flare, check logs for further details.");
        }
        try {
            currentFlare.start();
        } catch (IllegalStateException e) {
            platform.getLogger().log(Level.WARNING, "Error starting Flare:", e);
            throw new UserReportableException("Failed to start Flare, check logs for further details.");
        }

        currentTask = scheduler.buildTask(platform,
            task -> ProfilingManager.stop()).delay(15L, TimeUnit.MINUTES).schedule();
        platform.getLogger().log(Level.INFO, "Flare has been started: " + getProfilingUri());
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
        platform.getLogger().log(Level.INFO, "Flare has been stopped: " + getProfilingUri());
        try {
            currentFlare.stop();
        } catch (IllegalStateException e) {
            platform.getLogger().log(Level.WARNING, "Error occurred stopping Flare", e);
        }
        currentFlare = null;

        try {
            if (currentTask != null) {
                currentTask.cancel();
            }
        } catch (Throwable t) {
            platform.getLogger().log(Level.WARNING, "Error occurred stopping Flare", t);
        }
        currentTask = null;

        return true;
    }

}
