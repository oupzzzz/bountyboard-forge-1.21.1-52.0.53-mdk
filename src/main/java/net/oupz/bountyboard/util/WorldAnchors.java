package net.oupz.bountyboard.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

public final class WorldAnchors {
    private WorldAnchors() {}

    @Nullable
    public static BlockPos randomSurfaceAnchorNear(ServerLevel level, BlockPos center, int radius) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Try a bunch of times; increase if your worlds are very rugged.
        final int MAX_TRIES = 80;

        for (int i = 0; i < MAX_TRIES; i++) {
            int dx = rng.nextInt(-radius, radius + 1);
            int dz = rng.nextInt(-radius, radius + 1);
            BlockPos col = center.offset(dx, 0, dz);

            // Snap to world surface at this column
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, col);

            // Quick biome veto: skip oceans
            if (level.getBiome(surface).is(BiomeTags.IS_OCEAN)) continue;

            // Feet block must be air (or at least non-solid), and not contain any fluid
            if (!level.getBlockState(surface).isAir()) continue;
            if (!level.getFluidState(surface).isEmpty()) continue;

            // The block below must be real ground: not air, not leaves, no fluid, sturdy on top
            BlockPos below = surface.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.isAir()) continue;
            if (belowState.is(BlockTags.LEAVES)) continue;
            if (!level.getFluidState(below).isEmpty()) continue;
            if (!belowState.isFaceSturdy(level, below, Direction.UP)) continue;

            // Looks good: return a safe standing position
            return surface.immutable();
        }

        // Nothing acceptable found
        return null;
    }
}
