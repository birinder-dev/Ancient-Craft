package dev.birinder.ac.block;

import com.mojang.serialization.MapCodec;
import dev.birinder.ac.util.SitUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class TavernBenchBlock extends HorizontalFacingBlock {

    public static final MapCodec<TavernBenchBlock> CODEC = createCodec(TavernBenchBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<BenchPart> PART = EnumProperty.of("part", BenchPart.class);

    // Thick bench voxel shape (10 pixels high matching the 3D model)
    protected static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 10.0, 15.0)
    );

    public TavernBenchBlock(Settings settings) {
        super(settings);
        setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(PART, BenchPart.SINGLE));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
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

    private BenchPart getPart(WorldAccess world, BlockPos pos, Direction facing) {
        Direction leftDir = facing.rotateYCounterclockwise();
        Direction rightDir = facing.rotateYClockwise();

        boolean hasLeft = isMatchingBench(world, pos.offset(leftDir), facing);
        boolean hasRight = isMatchingBench(world, pos.offset(rightDir), facing);

        if (hasLeft && hasRight) {
            return BenchPart.MIDDLE;
        } else if (hasLeft) {
            return BenchPart.RIGHT;
        } else if (hasRight) {
            return BenchPart.LEFT;
        }
        return BenchPart.SINGLE;
    }

    private boolean isMatchingBench(WorldAccess world, BlockPos pos, Direction facing) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof TavernBenchBlock && state.contains(FACING)) {
            return state.get(FACING).getAxis() == facing.getAxis();
        }
        return false;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            if (SitUtil.sitEntity(world, pos, player, SitUtil.BENCH_OFFSET)) {
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.SUCCESS;
    }

    public enum BenchPart implements StringIdentifiable {
        SINGLE("single"),
        LEFT("left"),
        RIGHT("right"),
        MIDDLE("middle");

        private final String name;

        BenchPart(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }
    }
}
