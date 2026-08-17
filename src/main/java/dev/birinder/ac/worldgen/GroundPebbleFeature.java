package dev.birinder.ac.worldgen;

import com.mojang.serialization.Codec;
import dev.birinder.ac.block.ModBlocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class GroundPebbleFeature extends Feature<DefaultFeatureConfig> {

    public GroundPebbleFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();

        // 1st Rock: 70% chance per chunk
        if (random.nextFloat() < 0.70F) {
            placeSingleRock(world, origin, random);

            // 2nd Rock: 25% chance
            if (random.nextFloat() < 0.25F) {
                placeSingleRock(world, origin, random);
            }

            // 3rd Rock: 25% chance
            if (random.nextFloat() < 0.25F) {
                placeSingleRock(world, origin, random);
            }
            return true;
        }
        return false;
    }

    private void placeSingleRock(StructureWorldAccess world, BlockPos origin, Random random) {
        int x = origin.getX() + random.nextInt(16);
        int z = origin.getZ() + random.nextInt(16);
        int y = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z);
        BlockPos pos = new BlockPos(x, y, z);

        if (ModBlocks.GROUND_PEBBLE.getDefaultState().canPlaceAt(world, pos)) {
            world.setBlockState(pos, ModBlocks.GROUND_PEBBLE.getDefaultState(), 2);
        }
    }
}
