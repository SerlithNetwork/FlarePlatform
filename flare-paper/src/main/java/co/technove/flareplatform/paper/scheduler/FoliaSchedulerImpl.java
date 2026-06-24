package co.technove.flareplatform.paper.scheduler;

import co.technove.flareplatform.common.scheduler.IScheduler;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import org.bukkit.Bukkit;
import java.lang.reflect.Field;
import java.util.concurrent.ThreadPoolExecutor;

public class FoliaSchedulerImpl implements IScheduler {

    private final ThreadPoolExecutor executor;

    public FoliaSchedulerImpl() throws NoSuchFieldException, IllegalAccessException {
        AsyncScheduler scheduler = Bukkit.getServer().getAsyncScheduler();

        Field fieldExecutors = scheduler.getClass().getDeclaredField("executors");
        fieldExecutors.setAccessible(true);

        this.executor = (ThreadPoolExecutor) fieldExecutors.get(scheduler);
    }

    @Override
    public int getPoolSize() {
        return this.executor.getPoolSize();
    }

}
