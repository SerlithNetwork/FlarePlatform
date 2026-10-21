package co.technove.flareplatform.paper.scheduler;

import co.technove.flareplatform.common.scheduler.IScheduler;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import java.lang.reflect.Field;
import java.util.concurrent.ThreadPoolExecutor;

public final class BukkitSchedulerImpl implements IScheduler {

    private final ThreadPoolExecutor executor;

    @SuppressWarnings("deprecation")
    public BukkitSchedulerImpl() throws NoSuchFieldException, IllegalAccessException {
        final BukkitScheduler bukkit = Bukkit.getScheduler();
        final Field fieldBukkitAsyncScheduler = bukkit.getClass().getDeclaredField("asyncScheduler");
        fieldBukkitAsyncScheduler.setAccessible(true);

        final Object bukkitAsyncScheduler = fieldBukkitAsyncScheduler.get(bukkit);
        final Field fieldBukkitExecutor = bukkitAsyncScheduler.getClass().getDeclaredField("executor");
        fieldBukkitExecutor.setAccessible(true);

        this.executor = (ThreadPoolExecutor) fieldBukkitExecutor.get(bukkitAsyncScheduler);
    }

    @Override
    public int getPoolSize() {
        return this.executor.getPoolSize();
    }
}
