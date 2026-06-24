package co.technove.flareplatform.paper.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.category.GraphCategory;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.common.collectors.ThreadCollector;
import co.technove.flareplatform.common.scheduler.IScheduler;

public class PaperThreadCollector extends ThreadCollector {

    private static final CollectorData SCHEDULER_THREADS = new CollectorData("builtin:thread:schedulercount", "CraftScheduler Threads", "Number of CraftScheduler threads", new SuffixFormatter("CraftScheduler Thread", "CraftScheduler Threads"), GraphCategory.SYSTEM);
    private static final CollectorData FOLIA_SCHEDULER_THREADS = new CollectorData("builtin:thread:foliaschedulercount", "Folia Async Scheduler Threads", "Number of Folia Async Scheduler threads", new SuffixFormatter("Folia Async Scheduler Thread", "Folia Async Scheduler Threads"), GraphCategory.SYSTEM);

    private final IScheduler bukkitScheduler;
    private final IScheduler foliaScheduler;

    public PaperThreadCollector(final IScheduler bukkitScheduler, final IScheduler foliaScheduler) {
        super(SCHEDULER_THREADS, FOLIA_SCHEDULER_THREADS);
        this.bukkitScheduler = bukkitScheduler;
        this.foliaScheduler = foliaScheduler;
    }

    @Override
    protected void collectAdditionalMetrics() {
        this.report(SCHEDULER_THREADS, this.bukkitScheduler.getPoolSize());
        this.report(FOLIA_SCHEDULER_THREADS, this.foliaScheduler.getPoolSize());
    }
}
