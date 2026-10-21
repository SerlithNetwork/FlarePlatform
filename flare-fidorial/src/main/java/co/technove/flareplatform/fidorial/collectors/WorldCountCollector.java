package co.technove.flareplatform.fidorial.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.common.CustomCategories;
import fr.fidorial.Server;
import fr.fidorial.world.World;

import java.time.Duration;

public final class WorldCountCollector extends LiveCollector {

    private static final CollectorData PLAYER_COUNT = new CollectorData("airplane:world:playercount", "Player Count", "The number of players currently connected to this server.", new SuffixFormatter(" Player", " Players"), CustomCategories.PLAYERS);
    private static final CollectorData ENTITY_COUNT = new CollectorData("airplane:world:entitycount", "Entity Count", "The number of entities in all worlds", new SuffixFormatter(" Entity", " Entities"), CustomCategories.ENTITIES_AND_CHUNKS);
    private static final CollectorData REGION_COUNT = new CollectorData("flare:fidorial:regioncount", "Active Regions", "The number of regions currently ticking on their own worker thread.", new SuffixFormatter(" Region", " Regions"), CustomCategories.PERF);

    private final Server server;

    public WorldCountCollector(final Server server) {
        super(PLAYER_COUNT, ENTITY_COUNT, REGION_COUNT);
        this.server = server;
        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
        int entities = 0;

        if (this.server.isRunning()) {
            for (final World world : this.server.worlds().all()) {
                entities += world.entities().size();
            }
        }

        this.report(PLAYER_COUNT, this.server.players().online().size());
        this.report(ENTITY_COUNT, entities);
        this.report(REGION_COUNT, this.server.scheduler().tpsSnapshots().size());
    }
}
