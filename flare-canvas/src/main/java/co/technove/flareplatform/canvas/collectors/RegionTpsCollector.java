package co.technove.flareplatform.canvas.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.canvas.utils.RegionUtils;
import co.technove.flareplatform.common.CustomCategories;
import co.technove.flareplatform.common.types.ChunkPos;
import io.canvasmc.canvas.region.WorldRegionizer;
import org.bukkit.Bukkit;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class RegionTpsCollector extends LiveCollector {

    private final List<WeakReference<WorldRegionizer.ChunkRegion>> regions;
    private final Map<ChunkPos, CollectorData> tpsData;
    private final Map<ChunkPos, CollectorData> msptData;

    private RegionTpsCollector(List<WorldRegionizer.ChunkRegion> regions, Map<ChunkPos, CollectorData> tpsData, Map<ChunkPos, CollectorData> msptData) {
        super(Stream.concat(tpsData.values().stream(), msptData.values().stream()).toArray(CollectorData[]::new));
        this.regions = regions.stream().map(WeakReference::new).toList();
        this.tpsData = tpsData;
        this.msptData = msptData;
        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
        for (WeakReference<WorldRegionizer.ChunkRegion> regionReference : this.regions) {
            WorldRegionizer.ChunkRegion region = regionReference.get();
            if (region == null) {
                continue;
            }

            ChunkPos pos = RegionUtils.getChunkPos(region);
            if (pos == null) {
                regionReference.clear();
                continue;
            }

            CollectorData tpsCollector = tpsData.get(pos);
            if (tpsCollector != null) {
                double tps = Math.clamp(region.getTPS(WorldRegionizer.ChunkRegion.Frame._5_SECONDS), 0.0, 20.0);
                this.report(tpsCollector, Math.round(tps * 100d) / 100d);
            }

            CollectorData msptCollector = msptData.get(pos);
            if (msptCollector != null) {
                double mspt = Math.max(0.0, region.getMSPT(WorldRegionizer.ChunkRegion.Frame._5_SECONDS));
                this.report(msptCollector, Math.round(mspt * 100d) / 100d);
            }
        }
    }

    public static RegionTpsCollector create() {
        List<WorldRegionizer.ChunkRegion> allRegions = new ArrayList<>();
        Map<ChunkPos, CollectorData> tpsData = new HashMap<>();
        Map<ChunkPos, CollectorData> msptData = new HashMap<>();
        Bukkit.getWorlds().forEach(world -> {
            world.getRegionizer().computeForAllChunkRegions(allRegions::add);
        });
        allRegions.sort(RegionUtils::compareRegionsMspt);

        List<WorldRegionizer.ChunkRegion> regions = allRegions.subList(0, Math.min(10, allRegions.size() - 1));
        for (WorldRegionizer.ChunkRegion region : regions) {
            ChunkPos pos = RegionUtils.getChunkPos(region);
            if (pos == null) {
                continue;
            }

            String tpsId = String.format("flare:region[%d,%d]:tps", pos.x(), pos.z());
            String msptId = String.format("flare:region[%d,%d]:mspt", pos.x(), pos.z());
            tpsData.put(pos, new CollectorData(tpsId, "TPS", "Ticks per second, or how fast the region updates. For a smooth server this should be a constant 20TPS.", SuffixFormatter.of("TPS"), CustomCategories.PERF));
            msptData.put(pos, new CollectorData(msptId, "MSPT", "Milliseconds per tick, which can show how well this region is performing. This value should always be under 50mspt.", SuffixFormatter.of("mspt"), CustomCategories.PERF));
        }

        return new RegionTpsCollector(regions, tpsData, msptData);
    }



}
