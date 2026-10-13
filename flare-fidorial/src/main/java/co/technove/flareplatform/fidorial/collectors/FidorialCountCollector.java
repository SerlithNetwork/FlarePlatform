package co.technove.flareplatform.fidorial.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.common.CustomCategories;
import fr.fidorial.Server;

import java.time.Duration;

public class FidorialCountCollector extends LiveCollector {

    private static final CollectorData PLAYER_COUNT = new CollectorData("flare:fidorial:playercount", "Player Count", "The number of players currently connected to this server.", new SuffixFormatter(" Player", " Players"), CustomCategories.PLAYERS);
    private static final CollectorData REGION_COUNT = new CollectorData("flare:fidorial:regioncount", "Active Regions", "The number of regions currently ticking on their own worker thread.", new SuffixFormatter(" Region", " Regions"), CustomCategories.PERF);

    private final Server server;

    public FidorialCountCollector(Server server) {
        super(PLAYER_COUNT, REGION_COUNT);
        this.server = server;
        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
        this.report(PLAYER_COUNT, server.onlinePlayers().size());
        this.report(REGION_COUNT, server.scheduler().tpsSnapshots().size());
    }
}
