package co.technove.flareplatform.velocity;

import co.technove.flare.Flare;
import co.technove.flare.FlareAuth;
import co.technove.flare.FlareBuilder;
import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import co.technove.flareplatform.CustomCategories;
import co.technove.flareplatform.collectors.GCEventCollector;
import co.technove.flareplatform.collectors.StatCollector;
import co.technove.flareplatform.velocity.utils.ServerConfigurations;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

// yuck
@NullMarked
public class ProfilingManager {

    private static final Scheduler scheduler = FlarePlatformVelocity.getInstance().getServer().getScheduler();

    private @Nullable static Flare currentFlare;
    public @Nullable static ScheduledTask currentTask;

    public static synchronized boolean isProfiling() {
        return currentFlare != null && currentFlare.isRunning();
    }

    public static synchronized String getProfilingUri() {
        return Objects.requireNonNull(currentFlare).getURI().map(URI::toString).orElse("Flare is not running");
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
                    .withAuth(FlareAuth.fromTokenAndUrl(FlarePlatformVelocity.getInstance().getAccessToken(), FlarePlatformVelocity.getInstance().getFlareURI()))

                    .withFiles(ServerConfigurations.getCleanCopies())
                    .withVersion("Primary Version", FlarePlatformVelocity.getInstance().getServer().getVersion().getName())
                    .withVersion("Velocity Version", FlarePlatformVelocity.getInstance().getServer().getVersion())

                    .withGraphCategories(CustomCategories.PERF)
                    .withCollectors(new GCEventCollector(), new StatCollector())
                    .withClassIdentifier(FlarePlatformVelocity.getInstance().getPluginLookup()::getPluginForClass)

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
            FlarePlatformVelocity.getInstance().getLogger().log(Level.WARNING, "Failed to read configuration files:", e);
            throw new UserReportableException("Failed to load configuration files, check logs for further details.");
        }

        try {
            currentFlare.start();
            FlarePlatformVelocity.getInstance().refreshCommands();
        } catch (IllegalStateException e) {
            FlarePlatformVelocity.getInstance().getLogger().log(Level.WARNING, "Error starting Flare:", e);
            throw new UserReportableException("Failed to start Flare, check logs for further details.");
        }

        currentTask = scheduler.buildTask(FlarePlatformVelocity.getInstance(),
                task -> ProfilingManager.stop()).delay(15L, TimeUnit.MINUTES).schedule();
        FlarePlatformVelocity.getInstance().getLogger().log(Level.INFO, "Flare has been started: " + getProfilingUri());
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
        FlarePlatformVelocity.getInstance().getLogger().log(Level.INFO, "Flare has been stopped: " + getProfilingUri());
        try {
            currentFlare.stop();
            FlarePlatformVelocity.getInstance().refreshCommands();
        } catch (IllegalStateException e) {
            FlarePlatformVelocity.getInstance().getLogger().log(Level.WARNING, "Error occurred stopping Flare", e);
        }
        currentFlare = null;

        try {
            if (currentTask != null) currentTask.cancel();
        } catch (Throwable t) {
            FlarePlatformVelocity.getInstance().getLogger().log(Level.WARNING, "Error occurred stopping Flare", t);
        }
        currentTask = null;
        return true;
    }

}
