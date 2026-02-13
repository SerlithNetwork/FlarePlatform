package co.technove.flareplatform.common.config;

import net.j4c0b3y.api.config.ConfigHandler;
import net.j4c0b3y.api.config.StaticConfig;
import net.j4c0b3y.api.config.platform.adventure.types.PrefixedComponent;
import java.net.URI;
import java.nio.file.Path;

@StaticConfig.Header({
    "Flare main configuration file"
})
public abstract class FlareConfig extends StaticConfig {

    @Ignore
    public static FlareConfig INSTANCE;

    public FlareConfig(Path directory, ConfigHandler handler) {
        super(directory.resolve("config.yml"), handler);
        INSTANCE = this;
    }

    @Priority(1)
    @SuppressWarnings("unused")
    public static class INFO {
        public static final String VERSION = "1.0";
    }

    @Priority(2)
    public static class PROFILING {

        @Comment({
            "Your Flare authorization token",
            "If your viewer is public, you can input anything here"
        })
        public static String TOKEN = "";

        @Comment({
            "URI to upload the profiler samples",
            "It will be used for both the backend and viewer"
        })
        public static URI BACKEND_URL = URI.create("https://flare.airplane.gg");

        @Comment({
            "If provided, it will replace the backend URI before handling it to you",
            "Useful if your backend and frontend do not share the same path"
        })
        public static String FRONTEND_URL = "";

    }



    @Priority(4)
    public static class MESSAGES {
        public static PrefixedComponent PLUGIN_RELOAD_SUCCESS = new PrefixedComponent("<green>Flare config reloaded successfully!");
        public static PrefixedComponent PLUGIN_RELOAD_FAILED = new PrefixedComponent("<red>Failed to reload Flare config, check your logs!");
        @Comment("Used only if running the plugin on Velocity")
        public static PrefixedComponent PLUGIN_RELOAD_DENIED = new PrefixedComponent("<red>You cannot reload Flare while profiling!");
    }

}
