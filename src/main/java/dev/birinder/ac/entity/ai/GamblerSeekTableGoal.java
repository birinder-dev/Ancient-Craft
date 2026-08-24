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
        this.searchCooldown = 15; // Fast check: every 0.75 seconds

        this.targetStoolPos = findNearestGamblingStool();
        return this.targetStoolPos != null;
    }

    @Override
    public boolean shouldContinue() {
        return targetStoolPos != null && !this.gambler.hasVehicle()
                && !SitUtil.isSeatOccupied(this.gambler.getWorld(), targetStoolPos);
    }

    @Override
    public void start() {
        if (targetStoolPos != null) {
            this.gambler.getNavigation().startMovingTo(
                    targetStoolPos.getX() + 0.5,
                    targetStoolPos.getY(),
                    targetStoolPos.getZ() + 0.5,
                    1.15 // Brisk, confident walking speed
            );
        }
    }

    @Override
    public void tick() {
        if (targetStoolPos == null) {
            return;
        }

        this.gambler.getLookControl().lookAt(
                targetStoolPos.getX() + 0.5,
                targetStoolPos.getY() + 0.5,
                targetStoolPos.getZ() + 0.5
        );

        if (this.gambler.getBlockPos().isWithinDistance(targetStoolPos, 2.0)) {
            World world = this.gambler.getWorld();
            if (!world.isClient() && !SitUtil.isSeatOccupied(world, targetStoolPos)) {
                // Sit down on the gambling stool at calibrated height!
                SitUtil.sitEntity(world, targetStoolPos, this.gambler, SitUtil.STOOL_OFFSET);
                this.targetStoolPos = null;
            }
        }
    }

    private BlockPos findNearestGamblingStool() {
        World world = this.gambler.getWorld();
        BlockPos origin = this.gambler.getBlockPos();
        int radius = 20; // 20 blocks detection range

        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        // Full volumetric 3D scan within 20 blocks
        for (BlockPos pos : BlockPos.iterate(origin.add(-radius, -4, -radius), origin.add(radius, 4, radius))) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof GamblingStoolBlock) {
                if (isAdjacentToGamblingTable(world, pos) && !SitUtil.isSeatOccupied(world, pos)) {
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
