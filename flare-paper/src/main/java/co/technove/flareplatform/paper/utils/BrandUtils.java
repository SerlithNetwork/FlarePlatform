package co.technove.flareplatform.paper.utils;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class BrandUtils {
    private BrandUtils() {
    }

    public static boolean isParallelWorldTicking() {

        boolean enabled;
        try {
            final Method methodEnabled = Server.class.getMethod("isParallelWorldTickingEnabled");
            enabled = (boolean) methodEnabled.invoke(Bukkit.getServer());
        } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }

        if (!enabled) {
            return false;
        }

        try {
            World.class.getMethod("getAverageTickTime");
            return true;
        } catch (final NoSuchMethodException ignored) {
            return false;
        }
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }

    public static boolean isCanvas() {
        final Class<?> tickDataClass;
        final Class<?> frameClass;
        try {
            Class.forName("io.canvasmc.canvas.GlobalConfiguration");
            tickDataClass = Class.forName("io.canvasmc.canvas.region.RegionTickData");
            frameClass = Class.forName("io.canvasmc.canvas.region.RegionTickData$Frame");
        } catch (final ClassNotFoundException ignored) {
            return false;
        }

        try {
            tickDataClass.getMethod("getTPS", frameClass);
            tickDataClass.getMethod("getMSPT", frameClass);
        } catch (final NoSuchMethodException ignored) {
            return false;
        }

        return true;
    }
}
