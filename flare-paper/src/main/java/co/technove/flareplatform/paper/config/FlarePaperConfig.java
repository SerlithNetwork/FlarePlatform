package co.technove.flareplatform.paper.config;

import co.technove.flareplatform.common.config.FlareConfig;
import net.j4c0b3y.api.config.ConfigHandler;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class FlarePaperConfig extends FlareConfig {

    public FlarePaperConfig(Path directory, ConfigHandler handler) {
        super(directory, handler);
    }

    @Priority(3)
    public static class CONFIGURATIONS {

        @Comment({
            "Configuration files to include on the profiler",
            "paper-world.yml files are included automatically for every world"
        })
        public static List<String> CONFIGURATION_FILES = List.of(
            "server.properties",
            "bukkit.yml",
            "spigot.yml",
            "config/paper-global.yml",
            "config/paper-world-defaults.yml"
        );

        @Comment("Fields to ignore in the above configurations")
        public static List<String> HIDDEN_ENTRIES = List.of(
            "proxies.velocity.secret",
            "web-services.token",
            "misc.sentry-dsn",
            "database",
            "server-ip",
            "motd",
            "resource-pack",
            "level-seed",
            "rcon.password",
            "rcon.ip",
            "feature-seeds",
            "world-settings.*.feature-seeds",
            "world-settings.*.seed-*",
            "seed-*"
        );
        @Ignore
        public static List<Pattern> HIDDEN_ENTRIES_PATTERNS;

    }

    @Override
    public void afterLoad() {
        CONFIGURATIONS.HIDDEN_ENTRIES_PATTERNS = CONFIGURATIONS.HIDDEN_ENTRIES.stream()
            .map(s -> Pattern.compile(s.replace(".", "\\.").replace("*", ".*")))
            .toList();
    }

}
