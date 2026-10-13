package co.technove.flareplatform.fidorial.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.common.CustomCategories;
import fr.fidorial.Server;
import fr.fidorial.scheduler.RegionTps;

import java.time.Duration;
import java.util.List;

public class RegionTpsCollector extends LiveCollector {

    private static final CollectorData TPS_MIN = new CollectorData("flare:fidorial:region:tps_min", "Worst Region TPS", "TPS of the slowest region. Should stay at 20.", SuffixFormatter.of("tps"), CustomCategories.PERF);
    private static final CollectorData TPS_AVG = new CollectorData("flare:fidorial:region:tps_avg", "Average Region TPS", "Average TPS across all active regions. Should stay at 20.", SuffixFormatter.of("tps"), CustomCategories.PERF);
    private static final CollectorData MSPT_MAX = new CollectorData("flare:fidorial:region:mspt_max", "Worst Region MSPT", "Milliseconds per tick of the slowest region. This value should always be under 50mspt.", SuffixFormatter.of("mspt"), CustomCategories.PERF);
    private static final CollectorData MSPT_AVG = new CollectorData("flare:fidorial:region:mspt_avg", "Average Region MSPT", "Average milliseconds per tick across all active regions. This value should always be under 50mspt.", SuffixFormatter.of("mspt"), CustomCategories.PERF);
    private static final CollectorData QUEUED_TASKS = new CollectorData("flare:fidorial:region:queued", "Queued Region Tasks", "Total number of tasks waiting in region queues.", new SuffixFormatter(" Task", " Tasks"), CustomCategories.PERF);

    private final Server server;

    public RegionTpsCollector(Server server) {
        super(TPS_MIN, TPS_AVG, MSPT_MAX, MSPT_AVG, QUEUED_TASKS);
        this.server = server;
        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
        List<? extends RegionTps> snapshots = server.scheduler().tpsSnapshots();
        if (snapshots.isEmpty()) {
            return;
        }

        double tpsMin = Double.MAX_VALUE;
        double tpsSum = 0;
        double msptMax = 0;
        double msptSum = 0;
        int queued = 0;
        for (RegionTps snapshot : snapshots) {
            tpsMin = Math.min(tpsMin, snapshot.tps());
            tpsSum += snapshot.tps();
            msptMax = Math.max(msptMax, snapshot.msptAvg());
            msptSum += snapshot.msptAvg();
            queued += snapshot.queuedTasks();
        }

        this.report(TPS_MIN, round(tpsMin));
        this.report(TPS_AVG, round(tpsSum / snapshots.size()));
        this.report(MSPT_MAX, round(msptMax));
        this.report(MSPT_AVG, round(msptSum / snapshots.size()));
        this.report(QUEUED_TASKS, queued);
    }

    private static double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
