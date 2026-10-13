package co.technove.flareplatform.fidorial.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.category.GraphCategory;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.common.collectors.ThreadCollector;
import co.technove.flareplatform.common.scheduler.IScheduler;

public class FidorialThreadCollector extends ThreadCollector {

    private static final CollectorData SCHEDULER_THREADS = new CollectorData("builtin:thread:schedulercount", "RegionizedScheduler Threads", "Number of RegionizedScheduler threads", new SuffixFormatter("RegionizedScheduler Thread", "RegionizedScheduler Threads"), GraphCategory.SYSTEM);

    private final IScheduler scheduler;

    public FidorialThreadCollector(final IScheduler scheduler) {
        super(SCHEDULER_THREADS);
        this.scheduler = scheduler;
    }

    @Override
    protected void collectAdditionalMetrics() {
        this.report(SCHEDULER_THREADS, this.scheduler.getPoolSize());
    }
}
