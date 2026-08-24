package dev.birinder.ac.block;

import com.mojang.serialization.MapCodec;
import dev.birinder.ac.block.entity.GamblingTableBlockEntity;
import dev.birinder.ac.block.entity.ModBlockEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GamblingTableBlock extends BlockWithEntity {

    public static final MapCodec<GamblingTableBlock> CODEC = createCodec(GamblingTableBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    // Full tabletop with centered carved base (14 pixels high)
    protected static final VoxelShape TABLE_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0.0, 12.0, 0.0, 16.0, 15.0, 16.0), // Felt top
            Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 12.0, 14.0)   // Carved wooden pedestal
    );

    public GamblingTableBlock(Settings settings) {
        super(settings);
        setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return TABLE_SHAPE;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GamblingTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.GAMBLING_TABLE_BLOCK_ENTITY, GamblingTableBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof GamblingTableBlockEntity table) {
                // If player sneaks + right clicks, allow toggling bet stake
                if (player.isSneaking()) {
                    int nextBet = switch (table.getBetLimit()) {
                        case 1 -> 5;
                        case 5 -> 16;
                        case 16 -> 32;
                        case 32 -> 64;
                        default -> 1;
                    };
                    table.setBetLimit(nextBet);
                    player.sendMessage(Text.literal("🎲 Gambling Table stake set to: " + nextBet + " Emeralds")
                            .formatted(Formatting.GREEN, Formatting.BOLD), true);
                    return ActionResult.SUCCESS;
                }

                player.sendMessage(Text.literal("🎲 Table Stakes: " + table.getBetLimit() + " Emeralds. Sit on opposite Gambling Stools to play!")
                        .formatted(Formatting.YELLOW), true);
            }
        }
        return ActionResult.SUCCESS;
    }
}
