package dev.foabic.api.util;

/** A minimal, Minecraft-free block coordinate. */
public record BlockPos(int x, int y, int z) {

    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(x + dx, y + dy, z + dz);
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}
