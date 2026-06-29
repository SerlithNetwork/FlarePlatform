package co.technove.flareplatform.canvas.utils;

import co.technove.flareplatform.common.types.ChunkPos;
import co.technove.flareplatform.common.util.CoordinateUtils;
import io.canvasmc.canvas.region.RegionTickData;
import io.canvasmc.canvas.region.WorldRegionizer;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegionUtils {
    private RegionUtils() {
    }

    public static @Nullable ChunkPos getChunkPos(final WorldRegionizer.ChunkRegion region) {
        // note: regions don't always have chunks, they can be killed with no chunks existing
        final long[] packedPositions = region.getOwnedPackedChunkPositions();
        if (packedPositions.length == 0) {
            return null;
        }

        final List<Long> ownedChunks = new ArrayList<>(Arrays.stream(packedPositions).boxed().toList());
        ownedChunks.sort(CoordinateUtils::comparePackedChunkPositions);

        final long key = ownedChunks.get(ownedChunks.size() >> 1);
        final int centerChunkX = (int) key;
        final int centerChunkZ = (int) (key >>> 32);

        return new ChunkPos(centerChunkX, centerChunkZ);
    }

    public static int compareRegionsMspt(WorldRegionizer.ChunkRegion a, WorldRegionizer.ChunkRegion b) {
        final long[] chunksA = a.getOwnedPackedChunkPositions();
        if (chunksA.length == 0) {
            return Integer.MIN_VALUE;
        }

        final long[] chunksB = b.getOwnedPackedChunkPositions();
        if (chunksB.length == 0) {
            return Integer.MIN_VALUE;
        }

        RegionTickData dataA = a.getTickData();
        RegionTickData dataB = b.getTickData();
        return (int) Math.round(dataB.getMSPT(RegionTickData.Frame._5_SECONDS) - dataA.getMSPT(RegionTickData.Frame._5_SECONDS));
    }

}
