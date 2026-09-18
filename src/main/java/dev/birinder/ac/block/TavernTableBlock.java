package dev.birinder.ac.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.Scanner;

import org.jetbrains.annotations.Nullable;

public class TavernTableBlock extends HorizontalFacingBlock {

    public static final MapCodec<TavernTableBlock> CODEC = createCodec(TavernTableBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<TablePart> PART = EnumProperty.of("part", TablePart.class);

    // KCD Medieval Tavern Table Shape (14 pixels high)
    protected static final VoxelShape TABLE_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0.0, 11.0, 0.0, 16.0, 14.0, 16.0), // Thick timber tabletop
            Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 11.0, 15.0)   // Outer end slab legs
    );

    public TavernTableBlock(Settings settings) {
        super(settings);
        setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(PART, TablePart.SINGLE));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return TABLE_SHAPE;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        return this.getDefaultState().with(FACING, facing).with(PART, getPart(world, pos, facing));
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                   WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.get(FACING);
        return state.with(PART, getPart(world, pos, facing));
    }

    private TablePart getPart(WorldAccess world, BlockPos pos, Direction facing) {
        Direction leftDir = facing.rotateYCounterclockwise();
        Direction rightDir = facing.rotateYClockwise();

        boolean hasLeft = isMatchingTable(world, pos.offset(leftDir), facing);
        boolean hasRight = isMatchingTable(world, pos.offset(rightDir), facing);

        if (hasLeft && hasRight) {
            return TablePart.MIDDLE;
        } else if (hasLeft) {
            return TablePart.RIGHT;
        } else if (hasRight) {
            return TablePart.LEFT;
        }
        return TablePart.SINGLE;
    }

    private boolean isMatchingTable(WorldAccess world, BlockPos pos, Direction facing) {
        BlockState state = world.getBlockState(pos);
        // Connect if it is a TavernTableBlock and shares the same axis (e.g. North/South or East/West)
        if (state.getBlock() instanceof TavernTableBlock && state.contains(FACING)) {
            return state.get(FACING).getAxis() == facing.getAxis();
        }
        return false;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    public enum TablePart implements StringIdentifiable {
        SINGLE("single"),
        LEFT("left"),
        RIGHT("right"),
        MIDDLE("middle");

        private final String name;

        TablePart(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }
    }
}
