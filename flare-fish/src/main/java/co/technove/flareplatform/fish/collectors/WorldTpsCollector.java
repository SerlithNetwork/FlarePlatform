package co.technove.flareplatform.fish.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.common.CustomCategories;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldTpsCollector extends LiveCollector {

    private final List<WeakReference<World>> worlds;
    private final Map<NamespacedKey, CollectorData> msptData;

    private WorldTpsCollector(List<World> worlds, Map<NamespacedKey, CollectorData> msptData) {
        super(msptData.values().toArray(CollectorData[]::new));
        this.worlds = worlds.stream().map(WeakReference::new).toList();
        this.msptData = msptData;
        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
        for (WeakReference<World> worldReference : this.worlds) {
            World world = worldReference.get();
            if (world == null) {
                continue;
            }

            double mspt = Math.max(0.0, world.getAverageTickTime());
            CollectorData data = msptData.get(world.getKey());
            if (data == null) {
                throw new IllegalStateException("A world was scheduled for tracking with no mspt data collector attached");
            }

            this.report(data, Math.round(mspt * 100d) / 100d);
        }
    }

    public static WorldTpsCollector create() {
        List<World> allWorlds = Bukkit.getWorlds();
        allWorlds.sort((a, b) -> (int) Math.round(b.getAverageTickTime() - a.getAverageTickTime()));

        List<World> worlds = allWorlds.subList(0, Math.min(10, allWorlds.size()));
        Map<NamespacedKey, CollectorData> tpsData = new HashMap<>();
        Map<NamespacedKey, CollectorData> msptData = new HashMap<>();
        for (World world : worlds) {
            NamespacedKey key = world.getKey();
            String msptId = String.format("flare:world[%s]:mspt", key.asString().replace(":", "$"));
            msptData.put(key, new CollectorData(msptId, "MSPT", "Milliseconds per tick, which can show how well this world is performing. This value should always be under 50mspt.", SuffixFormatter.of("mspt"), CustomCategories.PERF));
        }

        return new WorldTpsCollector(worlds, msptData);
    }

}
