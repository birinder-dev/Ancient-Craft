package dev.birinder.ac.block.entity;

import dev.birinder.ac.block.GamblingStoolBlock;
import dev.birinder.ac.block.GamblingTableBlock;
import dev.birinder.ac.entity.custom.GamblerEntity;
import dev.birinder.ac.util.SitUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class GamblingTableBlockEntity extends BlockEntity {

    private int seatedTicks = 0;
    private boolean matchActive = false;
    private int betLimit = 5; // Default 5 emeralds

    public GamblingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAMBLING_TABLE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, GamblingTableBlockEntity blockEntity) {
        if (world == null || world.isClient()) {
            return;
        }

        Direction facing = state.get(GamblingTableBlock.FACING);
        BlockPos stool1Pos;
        BlockPos stool2Pos;

        if (facing.getAxis() == Direction.Axis.Z) {
            stool1Pos = pos.north();
            stool2Pos = pos.south();
        } else {
            stool1Pos = pos.east();
            stool2Pos = pos.west();
        }

        boolean hasStools = world.getBlockState(stool1Pos).getBlock() instanceof GamblingStoolBlock
                && world.getBlockState(stool2Pos).getBlock() instanceof GamblingStoolBlock;

        if (!hasStools) {
            blockEntity.seatedTicks = 0;
            blockEntity.matchActive = false;
            return;
        }

        LivingEntity occupant1 = SitUtil.getSeatedEntity(world, stool1Pos);
        LivingEntity occupant2 = SitUtil.getSeatedEntity(world, stool2Pos);

        if (occupant1 != null && occupant2 != null) {
            blockEntity.seatedTicks++;

            // 1.5-second (30 ticks) countdown trigger!
            if (blockEntity.seatedTicks == 30) {
                blockEntity.onDualSeatedMatchTrigger(occupant1, occupant2);
            }
        } else {
            if (blockEntity.seatedTicks > 0) {
                blockEntity.seatedTicks = 0;
                blockEntity.matchActive = false;
            }
        }
    }

    private void onDualSeatedMatchTrigger(LivingEntity occupant1, LivingEntity occupant2) {
        if (world == null || world.isClient()) {
            return;
        }

        this.matchActive = true;

        // Play dice cup shake & wooden table sound
        world.playSound(null, pos, SoundEvents.BLOCK_CHERRY_WOOD_PLACE, SoundCategory.BLOCKS, 1.0F, 1.2F);

        PlayerEntity humanPlayer = null;
        GamblerEntity gamblerOpponent = null;
        LivingEntity villagerOpponent = null;
        String opponentTitle = "Village Trader";
        PlayerEntity secondHuman = null;

        // Classify Occupant 1
        if (occupant1 instanceof PlayerEntity p1) {
            humanPlayer = p1;
        } else if (occupant1 instanceof GamblerEntity g1) {
            gamblerOpponent = g1;
        } else if (occupant1 instanceof dev.birinder.ac.entity.custom.TavernVillagerEntity tv1) {
            villagerOpponent = tv1;
            opponentTitle = "Tavern Local";
        } else if (occupant1 instanceof VillagerEntity v1) {
            villagerOpponent = v1;
            opponentTitle = "Village Trader";
        }

        // Classify Occupant 2
        if (occupant2 instanceof PlayerEntity p2) {
            if (humanPlayer == null) {
                humanPlayer = p2;
            } else {
                secondHuman = p2;
            }
        } else if (occupant2 instanceof GamblerEntity g2) {
            gamblerOpponent = g2;
        } else if (occupant2 instanceof dev.birinder.ac.entity.custom.TavernVillagerEntity tv2) {
            villagerOpponent = tv2;
            opponentTitle = "Tavern Local";
        } else if (occupant2 instanceof VillagerEntity v2) {
            villagerOpponent = v2;
            opponentTitle = "Village Trader";
        }

        // Case A: Singleplayer (1 Human Player vs 1 Gambler NPC)
        if (humanPlayer != null && gamblerOpponent != null) {
            humanPlayer.sendMessage(Text.literal("🎲 Match Starting vs The Gambler! (Stakes: " + betLimit + " Emeralds)")
                    .formatted(Formatting.GOLD, Formatting.BOLD), true);
            gamblerOpponent.onMatchInitiated(humanPlayer);
        }
        // Case B: Singleplayer (1 Human Player vs 1 Local Villager / Tavern Villager)
        else if (humanPlayer != null && villagerOpponent != null) {
            humanPlayer.sendMessage(Text.literal("🎲 Match Starting vs " + opponentTitle + "! (Stakes: " + betLimit + " Emeralds)")
                    .formatted(Formatting.GREEN, Formatting.BOLD), true);
        }
        // Case C: Multiplayer (2 Human Players)
        else if (humanPlayer != null && secondHuman != null) {
            humanPlayer.sendMessage(Text.literal("🎲 Match Starting vs " + secondHuman.getName().getString() + "! (Stakes: " + betLimit + " Emeralds)")
                    .formatted(Formatting.GOLD, Formatting.BOLD), true);
            secondHuman.sendMessage(Text.literal("🎲 Match Starting vs " + humanPlayer.getName().getString() + "! (Stakes: " + betLimit + " Emeralds)")
                    .formatted(Formatting.GOLD, Formatting.BOLD), true);
        }
    }

    public int getBetLimit() {
        return betLimit;
    }

    public void setBetLimit(int betLimit) {
        this.betLimit = betLimit;
        markDirty();
    }

    public int getSeatedTicks() {
        return seatedTicks;
    }

    public boolean isMatchActive() {
        return matchActive;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("BetLimit", betLimit);
        nbt.putBoolean("MatchActive", matchActive);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.betLimit = nbt.getInt("BetLimit");
        this.matchActive = nbt.getBoolean("MatchActive");
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}
