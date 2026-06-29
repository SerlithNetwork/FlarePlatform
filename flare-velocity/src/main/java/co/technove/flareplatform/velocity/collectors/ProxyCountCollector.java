package co.technove.flareplatform.velocity.collectors;

import co.technove.flare.live.CollectorData;
import co.technove.flare.live.LiveCollector;
import co.technove.flare.live.formatter.SuffixFormatter;
import co.technove.flareplatform.common.CustomCategories;
import java.time.Duration;
import co.technove.flareplatform.velocity.FlarePlatformVelocity;

public class ProxyCountCollector extends LiveCollector {

    private static final CollectorData PLAYER_COUNT = new CollectorData("flare:proxy:playercount", "Player Count", "The number of players currently connected to this proxy.", new SuffixFormatter(" Player", " Players"), CustomCategories.PLAYERS);
    private static final CollectorData SERVER_COUNT = new CollectorData("flare:proxy:servercount", "Server Count", "The number of servers this proxy manages", new SuffixFormatter(" Server", " Servers"), CustomCategories.SERVERS);

    public ProxyCountCollector() {
        super(PLAYER_COUNT, SERVER_COUNT);

        this.interval = Duration.ofSeconds(5);
    }

    @Override
    public void run() {
        FlarePlatformVelocity platform = FlarePlatformVelocity.getInstance();
        this.report(PLAYER_COUNT, platform.getServer().getPlayerCount());
        this.report(SERVER_COUNT, platform.getServer().getAllServers().size());
    }
}
