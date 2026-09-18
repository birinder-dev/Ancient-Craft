package dev.birinder.ac.entity.ai;

import dev.birinder.ac.block.GamblingStoolBlock;
import dev.birinder.ac.block.GamblingTableBlock;
import dev.birinder.ac.entity.custom.GamblerEntity;
import dev.birinder.ac.util.SitUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.EnumSet;

public class GamblerSeekTableGoal extends Goal {

    private final GamblerEntity gambler;
    private BlockPos targetStoolPos = null;
    private int searchCooldown = 0;

    public GamblerSeekTableGoal(GamblerEntity gambler) {
        this.gambler = gambler;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // If already seated/riding, don't seek
        if (this.gambler.hasVehicle()) {
            return false;
        }

        if (this.searchCooldown-- > 0) {
            return false;
        }
        this.searchCooldown = 20; // Check every 1 second

        this.targetStoolPos = findNearestGamblingStool();
        if (this.targetStoolPos != null) {
            SitUtil.claimSeat(this.gambler.getWorld(), this.targetStoolPos, this.gambler.getUuid(), 300);
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldContinue() {
        if (targetStoolPos == null || this.gambler.hasVehicle()) {
            return false;
        }
        World world = this.gambler.getWorld();
        return !SitUtil.isSeatOccupied(world, targetStoolPos)
                && !SitUtil.isSeatReservedForPlayer(world, targetStoolPos)
                && !SitUtil.isSeatClaimedByOther(world, targetStoolPos, this.gambler.getUuid());
    }

    @Override
    public void start() {
        if (targetStoolPos != null) {
            this.gambler.getNavigation().startMovingTo(
                    targetStoolPos.getX() + 0.5,
                    targetStoolPos.getY(),
                    targetStoolPos.getZ() + 0.5,
                    1.0);
        }
    }

    @Override
    public void tick() {
        if (targetStoolPos == null) {
            return;
        }

        World world = this.gambler.getWorld();

        if (SitUtil.isSeatOccupied(world, targetStoolPos) || SitUtil.isSeatReservedForPlayer(world, targetStoolPos)) {
            SitUtil.releaseSeatClaim(world, targetStoolPos, this.gambler.getUuid());
            this.targetStoolPos = null;
            return;
        }

        SitUtil.claimSeat(world, targetStoolPos, this.gambler.getUuid(), 100);

        this.gambler.getLookControl().lookAt(
                targetStoolPos.getX() + 0.5,
                targetStoolPos.getY() + 0.5,
                targetStoolPos.getZ() + 0.5);

        if (this.gambler.getBlockPos().isWithinDistance(targetStoolPos, 2.0)) {
            if (!world.isClient() && !SitUtil.isSeatOccupied(world, targetStoolPos)) {
                // Sit down on the gambling stool at calibrated height!
                if (SitUtil.sitEntity(world, targetStoolPos, this.gambler, SitUtil.STOOL_OFFSET)) {
                    this.targetStoolPos = null;
                } else {
                    SitUtil.releaseSeatClaim(world, targetStoolPos, this.gambler.getUuid());
                    this.targetStoolPos = null;
                }
            }
        }
    }

    @Override
    public void stop() {
        World world = this.gambler.getWorld();
        if (this.targetStoolPos != null) {
            SitUtil.releaseSeatClaim(world, this.targetStoolPos, this.gambler.getUuid());
            this.targetStoolPos = null;
        }
        this.searchCooldown = 100 + this.gambler.getRandom().nextInt(100);
    }

    private BlockPos findNearestGamblingStool() {
        World world = this.gambler.getWorld();
        BlockPos origin = this.gambler.getBlockPos();
        int radius = 20; // 20 blocks detection range

        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        // STRICT: Only GamblingStoolBlock adjacent to GamblingTableBlock
        for (BlockPos pos : BlockPos.iterate(origin.add(-radius, -4, -radius), origin.add(radius, 4, radius))) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof GamblingStoolBlock) {
                if (isAdjacentToGamblingTable(world, pos)
                        && !SitUtil.isSeatOccupied(world, pos)
                        && !SitUtil.isSeatReservedForPlayer(world, pos)
                        && !SitUtil.isSeatClaimedByOther(world, pos, this.gambler.getUuid())) {
                    double distSq = origin.getSquaredDistance(pos);
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearest = pos.toImmutable();
                    }
                }
            }
        }
        return nearest;
    }

    private boolean isAdjacentToGamblingTable(World world, BlockPos stoolPos) {
        return world.getBlockState(stoolPos.north()).getBlock() instanceof GamblingTableBlock
                || world.getBlockState(stoolPos.south()).getBlock() instanceof GamblingTableBlock
                || world.getBlockState(stoolPos.east()).getBlock() instanceof GamblingTableBlock
                || world.getBlockState(stoolPos.west()).getBlock() instanceof GamblingTableBlock;
    }
}
