package co.technove.flareplatform.canvas.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.canvas.utils.RegionUtils;
import co.technove.flareplatform.common.CustomCategories;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldRegionCountCollector extends LiveCollector {

    private final Map<Key, CollectorData> worldCollectors;

    private WorldRegionCountCollector(Map<Key, CollectorData> worldCollectors) {
        super(worldCollectors.values().toArray(CollectorData[]::new));
        this.worldCollectors = worldCollectors;
        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            final CollectorData collector = this.worldCollectors.get(world.key());
            if (collector == null) {
                continue;
            }

            this.report(collector, RegionUtils.countRegionsIn(world));
        }
    }

    public static WorldRegionCountCollector create() {
        List<World> allWorlds = Bukkit.getWorlds();
        allWorlds.sort((a, b) -> RegionUtils.countRegionsIn(b) - RegionUtils.countRegionsIn(a));

        List<World> worlds = allWorlds.subList(0, Math.min(10, allWorlds.size()));
        Map<Key, CollectorData> regionsData = new HashMap<>();
        for (World world : worlds) {
            Key key = world.key();
            String regionsId = String.format("flare:count:world[%s]:regions", key.asString().replace(":", "$"));
            regionsData.put(key, new CollectorData(
                regionsId,
                "Region Count",
                "The number of regions in this world.",
                new SuffixFormatter(" Region", " Regions"),
                CustomCategories.ENTITIES_AND_CHUNKS
            ));
        }

        return new WorldRegionCountCollector(regionsData);
    }

}
