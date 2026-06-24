package co.technove.flareplatform.common.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.category.GraphCategory;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.common.scheduler.IScheduler;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Duration;

public class ThreadCollector extends LiveCollector {

    private static final CollectorData NATIVE_THREADS = new CollectorData("builtin:thread:threadcount", "Threads", "Number of threads", new SuffixFormatter("Thread", "Threads"), GraphCategory.SYSTEM);
    private static final CollectorData PEAK_THREADS = new CollectorData("builtin:thread:peakcount", "Peak Threads", "Peak number of threads", new SuffixFormatter("Thread", "Threads"), GraphCategory.SYSTEM);
    private static final CollectorData STARTED_THREADS = new CollectorData("builtin:thread:startedcount", "Started Threads", "Total number of threads created", new SuffixFormatter("Thread Started", "Threads Started"), GraphCategory.SYSTEM);
    private static final CollectorData DAEMON_THREADS = new CollectorData("builtin:thread:daemoncount", "Daemon Threads", "Number of daemon threads", new SuffixFormatter("Daemon Thread", "Daemon Threads"), GraphCategory.SYSTEM);
    private static final CollectorData SCHEDULER_THREADS = new CollectorData("builtin:thread:schedulercount", "CraftScheduler Threads", "Number of CraftScheduler threads", new SuffixFormatter("CraftScheduler Thread", "CraftScheduler Threads"), GraphCategory.SYSTEM);
    private static final CollectorData FOLIA_SCHEDULER_THREADS = new CollectorData("builtin:thread:foliaschedulercount", "Folia Async Scheduler Threads", "Number of Folia Async Scheduler threads", new SuffixFormatter("Folia Async Scheduler Thread", "Folia Async Scheduler Threads"), GraphCategory.SYSTEM);

    private final ThreadMXBean threadMXBean;
    private final IScheduler bukkitScheduler;
    private final IScheduler foliaScheduler;

    public ThreadCollector(final IScheduler bukkitScheduler, final IScheduler foliaScheduler) {
        super(NATIVE_THREADS, PEAK_THREADS, STARTED_THREADS, DAEMON_THREADS, SCHEDULER_THREADS, FOLIA_SCHEDULER_THREADS);
        this.interval = Duration.ofSeconds(5);
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.bukkitScheduler = bukkitScheduler;
        this.foliaScheduler = foliaScheduler;
    }

    @Override
    public void run() {
        this.report(NATIVE_THREADS, this.threadMXBean.getThreadCount());
        this.report(PEAK_THREADS, this.threadMXBean.getPeakThreadCount());
        this.report(STARTED_THREADS, this.threadMXBean.getTotalStartedThreadCount());
        this.report(DAEMON_THREADS, this.threadMXBean.getDaemonThreadCount());
        this.report(SCHEDULER_THREADS, this.bukkitScheduler.getPoolSize());
        this.report(FOLIA_SCHEDULER_THREADS, this.foliaScheduler.getPoolSize());
    }

}
