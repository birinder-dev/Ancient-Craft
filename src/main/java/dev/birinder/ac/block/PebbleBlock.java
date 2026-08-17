package dev.birinder.ac.block;

import dev.birinder.ac.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class PebbleBlock extends Block {

    // Small 3D bounding box sitting flat on the ground (6x2x6 pixels)
    protected static final VoxelShape SHAPE = Block.createCuboidShape(5.0D, 0.0D, 5.0D, 11.0D, 2.0D, 11.0D);

    public PebbleBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    // Ensures the pebble can only stay on solid top surfaces (Grass, Sand, Gravel,
    // Dirt)
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos downPos = pos.down();
        BlockState currentState = world.getBlockState(pos);
        return world.getBlockState(downPos).isSideSolidFullSquare(world, downPos, Direction.UP)
                && (currentState.isAir() || currentState.isReplaceable());
    }

    // Right-click to pick up 1 to 3 pebbles (30% chance for Poison Pebble drop!)
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            int total = world.getRandom().nextInt(3) + 1; // 1, 2, or 3
            int poisonCount = 0;
            int normalCount = 0;

            for (int i = 0; i < total; i++) {
                if (world.getRandom().nextFloat() < 0.30F) { // 30% chance per pebble
                    poisonCount++;
                } else {
                    normalCount++;
                }
            }

            if (normalCount > 0) {
                dropStack(world, pos, new ItemStack(ModItems.PEBBLE, normalCount));
            }
            if (poisonCount > 0) {
                dropStack(world, pos, new ItemStack(ModItems.POISON_PEBBLE, poisonCount));
            }

            // Play pickup sound
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5F, 1.0F);

            // Remove pebble block from the world
            world.removeBlock(pos, false);
        }
        return ActionResult.SUCCESS;
    }
}