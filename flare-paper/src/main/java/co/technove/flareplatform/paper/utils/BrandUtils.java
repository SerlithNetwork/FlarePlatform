package co.technove.flareplatform.paper.utils;

import org.bukkit.Server;
import org.bukkit.World;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BrandUtils {
    private BrandUtils() {
    }

    public static boolean isParallelWorldTicking() {

        boolean enabled;
        try {
            Method methodEnabled = Server.class.getMethod("isParallelWorldTickingEnabled");
            enabled = (boolean) methodEnabled.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return false;
        }

        if (!enabled) {
            return false;
        }

        try {
            World.class.getMethod("getAverageTickTime");
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static boolean isCanvas() {
        Class<?> tickDataClass;
        Class<?> frameClass;
        try {
            Class.forName("io.canvasmc.canvas.GlobalConfiguration");
            tickDataClass = Class.forName("io.canvasmc.canvas.region.RegionTickData");
            frameClass = Class.forName("io.canvasmc.canvas.region.RegionTickData$Frame");
        } catch (ClassNotFoundException ignored) {
            return false;
        }

        try {
            tickDataClass.getMethod("getTPS", frameClass);
            tickDataClass.getMethod("getMSPT", frameClass);
        } catch (NoSuchMethodException ignored) {
            return false;
        }

        return true;
    }

}
