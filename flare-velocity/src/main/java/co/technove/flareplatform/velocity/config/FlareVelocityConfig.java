package co.technove.flareplatform.velocity.config;

import co.technove.flareplatform.common.config.FlareConfig;
import net.j4c0b3y.api.config.ConfigHandler;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class FlareVelocityConfig extends FlareConfig {

    public FlareVelocityConfig(Path directory, ConfigHandler handler) {
        super(directory, handler);
    }

    @Priority(3)
    public static class CONFIGURATIONS {

        @Comment({
            "Configuration files to include on the profiler"
        })
        public static List<String> CONFIGURATION_FILES = List.of(
            "velocity.toml"
        );

        @Comment("Fields to ignore in the above configurations")
        public static List<String> HIDDEN_ENTRIES = List.of(
            "servers",
            "forced-hosts"
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
