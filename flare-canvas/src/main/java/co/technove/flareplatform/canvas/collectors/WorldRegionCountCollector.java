package co.technove.flareplatform.canvas.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.common.CustomCategories;
import io.canvasmc.canvas.region.WorldRegionizer;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldRegionCountCollector extends LiveCollector {

    private final Map<World, CollectorData> worldCollectors;

    public WorldRegionCountCollector() {
        this(createCollectors());
    }

    public WorldRegionCountCollector(Map<World, CollectorData> worldCollectors) {
        super(worldCollectors.values().toArray(CollectorData[]::new));
        this.worldCollectors = worldCollectors;
        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
        for (World world : Bukkit.getServer().getWorlds()) {
            final List<WorldRegionizer.ChunkRegion> regionCount = new ArrayList<>();
            world.getRegionizer().computeForAllChunkRegions(regionCount::add);
            CollectorData collector = collectorForWorld(world);
            if (collector != null) {
                this.report(collector, regionCount.size());
            }
        }
    }

    public CollectorData collectorForWorld(World world) {
        return worldCollectors.get(world);
    }

    private static Map<World, CollectorData> createCollectors() {
        Map<World, CollectorData> collectors = new HashMap<>();

        for (World world : Bukkit.getWorlds()) {
            collectors.put(world, new CollectorData(
                "flare:world:[" + world.key().asMinimalString() + "]:regioncount",
                "Region Count",
                "The number of regions in this world.",
                new SuffixFormatter(" Region", " Regions"),
                CustomCategories.ENTITIES_AND_CHUNKS
            ));
        }

        return collectors;
    }
}
