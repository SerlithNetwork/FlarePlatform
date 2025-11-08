package co.technove.flareplatform.paper.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.common.CustomCategories;
import co.technove.flareplatform.paper.utils.ServerListener;

import java.time.Duration;

public class TPSCollector extends LiveCollector {
    private static final CollectorData TPS = new CollectorData("airplane:tps", "TPS", "Ticks per second, or how fast the server updates. For a smooth server this should be a constant 20TPS.", SuffixFormatter.of("TPS"), CustomCategories.PERF);
    private static final CollectorData MSPT = new CollectorData("airplane:mspt", "MSPT", "Milliseconds per tick, which can show how well your server is performing. This value should always be under 50mspt.", SuffixFormatter.of("mspt"), CustomCategories.PERF);

    public TPSCollector() {
        super(TPS, MSPT);
        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
        double tps = Math.max(Math.min(ServerListener.TPS_AVERAGE.getAverage(), 20.0), 0.0);
        double mspt = Math.max(0.0, ServerListener.MSPT_AVERAGE.getAverage());
        this.report(TPS, Math.round(tps * 100d) / 100d);
        this.report(MSPT, Math.round(mspt * 100d) / 100d);
    }
}
