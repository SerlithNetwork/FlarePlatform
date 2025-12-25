package co.technove.flareplatform.paper.utils;

import co.technove.flareplatform.paper.FlarePlatformPaper;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import co.technove.flareplatform.common.util.RollingAverage;
import co.technove.flareplatform.common.util.TpsRollingAverage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

// Referenced from both Minecraft internals and Spark
// taken from https://github.com/SerlithNetwork/PurpurBars/blob/ver/3.0/src/main/java/net/serlith/purpur/listeners/ServerListener.java
public class ServerListener implements Listener {

    private static final long SEC_IN_NANO = TimeUnit.SECONDS.toNanos(1);
    private static final int TPS_SAMPLE_INTERVAL = 20;
    private static final BigDecimal TPS_BASE = new BigDecimal(SEC_IN_NANO).multiply(new BigDecimal(TPS_SAMPLE_INTERVAL));

    public static final TpsRollingAverage TPS_AVERAGE = new TpsRollingAverage(5);
    public static final RollingAverage MSPT_AVERAGE = new RollingAverage(20 * 5);

    private int tick = 0;
    private long last = 0;

    private final FlarePlatformPaper plugin;

    public ServerListener(FlarePlatformPaper plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onTickStart(ServerTickStartEvent event) {
        if (this.tick++ % TPS_SAMPLE_INTERVAL != 0) return;

        long now = System.nanoTime();
        if (this.last == 0) {
            this.last = now;
            return;
        }

        long diff = now - this.last;
        if (diff <= 0) return;

        BigDecimal currentTps = TPS_BASE.divide(new BigDecimal(diff), 30, RoundingMode.HALF_UP);
        BigDecimal total = currentTps.multiply(new BigDecimal(diff));

        TPS_AVERAGE.add(currentTps, diff, total);

        this.last = now;
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTickEnd(ServerTickEndEvent event) {
        MSPT_AVERAGE.add(new BigDecimal(event.getTickDuration()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerLoad(ServerLoadEvent event) {
        this.plugin.setPluginLookup(new PluginLookup());
    }

}
