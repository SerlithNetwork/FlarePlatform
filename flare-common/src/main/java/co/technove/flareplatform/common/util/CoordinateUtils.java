package co.technove.flareplatform.common.util;

public class CoordinateUtils {
    private CoordinateUtils() {
    }

    public static int asChunkCoordinate(final double coordinate) {
        return ((int) Math.floor(coordinate)) >> 4;
    }

    public static int comparePackedChunkPositions(final long pos1, final long pos2) {
        final int x1 = CoordinateUtils.asChunkCoordinate(pos1);
        final int x2 = CoordinateUtils.asChunkCoordinate(pos2);

        final int z1 = CoordinateUtils.asChunkCoordinate(pos1);
        final int z2 = CoordinateUtils.asChunkCoordinate(pos2);

        final int zCompare = Integer.compare(z1, z2);
        if (zCompare != 0) {
            return zCompare;
        }

        return Integer.compare(x1, x2);
    }

}
