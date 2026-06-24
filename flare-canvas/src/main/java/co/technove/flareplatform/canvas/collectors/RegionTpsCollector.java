package co.technove.flareplatform.canvas.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import io.canvasmc.canvas.region.WorldRegionizer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class RegionTpsCollector extends LiveCollector {

    private final List<WeakReference<WorldRegionizer.ChunkRegion>> regions;
    private final Map<Location, CollectorData> tpsData;
    private final Map<Location, CollectorData> msptData;

    private RegionTpsCollector(List<WorldRegionizer.ChunkRegion> regions, Map<Location, CollectorData> tpsData, Map<Location, CollectorData> msptData) {
        super(Stream.concat(tpsData.values().stream(), msptData.values().stream()).toArray(CollectorData[]::new));
        this.regions = regions.stream().map(WeakReference::new).toList();
        this.tpsData = tpsData;
        this.msptData = msptData;
        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
    }

    public static RegionTpsCollector create() {
        List<WorldRegionizer.ChunkRegion> regions = new ArrayList<>();
        Map<Location, CollectorData> tpsData = new HashMap<>();
        Map<Location, CollectorData> msptData = new HashMap<>();
        Bukkit.getWorlds().forEach(world -> {
            world.getRegionizer().computeForAllChunkRegions(region -> {
            });
        });
        return new RegionTpsCollector(regions, tpsData, msptData);
    }

}
