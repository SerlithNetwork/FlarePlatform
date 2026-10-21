package co.technove.flareplatform.canvas.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.canvas.utils.RegionUtils;
import co.technove.flareplatform.common.CustomCategories;
import io.canvasmc.canvas.region.RegionTickData;
import io.canvasmc.canvas.region.WorldRegionizer;
import org.bukkit.Bukkit;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class RegionTpsCollector extends LiveCollector {

    // Getting regions is synchronized, and there might be a lot of regions
    private final List<WeakReference<WorldRegionizer.ChunkRegion>> regions;
    private final Map<Long, CollectorData> tpsData;
    private final Map<Long, CollectorData> msptData;

    private RegionTpsCollector(final List<WorldRegionizer.ChunkRegion> regions, final Map<Long, CollectorData> tpsData, final Map<Long, CollectorData> msptData) {
        super(Stream.concat(tpsData.values().stream(), msptData.values().stream()).toArray(CollectorData[]::new));
        this.regions = regions.stream().map(WeakReference::new).toList();
        this.tpsData = tpsData;
        this.msptData = msptData;
        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
        for (final WeakReference<WorldRegionizer.ChunkRegion> regionReference : this.regions) {
            final WorldRegionizer.ChunkRegion region = regionReference.get();
            if (region == null) {
                continue;
            }

            final long id = region.getId();
            final RegionTickData data = region.getTickData();
            final CollectorData tpsCollector = tpsData.get(id);
            if (tpsCollector != null) {
                final double tps = Math.clamp(data.getTPS(RegionTickData.Frame._5_SECONDS), 0.0, 20.0);
                this.report(tpsCollector, Math.round(tps * 100d) / 100d);
            }

            final CollectorData msptCollector = msptData.get(id);
            if (msptCollector != null) {
                final double mspt = Math.max(0.0, data.getMSPT(RegionTickData.Frame._5_SECONDS));
                this.report(msptCollector, Math.round(mspt * 100d) / 100d);
            }
        }
    }

    public static RegionTpsCollector create() {
        final List<WorldRegionizer.ChunkRegion> allRegions = new ArrayList<>();
        final Map<Long, CollectorData> tpsData = new HashMap<>();
        final Map<Long, CollectorData> msptData = new HashMap<>();
        Bukkit.getWorlds().forEach(world -> world.getRegionizer().computeForAllChunkRegions(allRegions::add));
        allRegions.sort(RegionUtils::compareRegionsMspt);

        final List<WorldRegionizer.ChunkRegion> regions = allRegions.subList(0, Math.min(10, allRegions.size()));
        for (final WorldRegionizer.ChunkRegion region : regions) {
            final long id = region.getId();
            final String tpsId = String.format("flare:perf:region[%d]:tps", id);
            final String msptId = String.format("flare:perf:region[%d]:mspt", id);
            tpsData.put(id, new CollectorData(
                tpsId,
                "TPS",
                "Ticks per second, or how fast the region updates. For a smooth server this should be a constant 20TPS.",
                SuffixFormatter.of("TPS"), CustomCategories.PERF)
            );
            msptData.put(id, new CollectorData(
                msptId,
                "MSPT",
                "Milliseconds per tick, which can show how well this region is performing. This value should always be under 50mspt.",
                SuffixFormatter.of("mspt"), CustomCategories.PERF)
            );
        }

        return new RegionTpsCollector(regions, tpsData, msptData);
    }
}
