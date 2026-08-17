package dev.birinder.ac.entity.projectile;

import dev.birinder.ac.entity.ModEntities;
import dev.birinder.ac.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.util.hit.BlockHitResult;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ItemStackParticleEffect;

public class SlingshotProjectileEntity extends ThrownItemEntity {

    private float damage = 2.0f;

    public SlingshotProjectileEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    public SlingshotProjectileEntity(World world, LivingEntity owner) {
        super(ModEntities.SLINGSHOT_PROJECTILE, owner, world);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return this.damage;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PEBBLE;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity entity = entityHitResult.getEntity();

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            entity.damage(this.getDamageSources().thrown(this, this.getOwner()), this.damage);

            ItemStack ammoStack = this.getStack();
            if (entity instanceof LivingEntity target) {
                if (ammoStack.isOf(ModItems.POISON_PEBBLE)) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 120, 0));
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 0));
                } else if (ammoStack.isOf(Items.POISONOUS_POTATO)) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 0));
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0));
                }
            }

            // Spawn entity impact particles (item crumble/shatter effect)
            if (!ammoStack.isEmpty()) {
                serverWorld.spawnParticles(
                        new ItemStackParticleEffect(ParticleTypes.ITEM, ammoStack),
                        entity.getX(), entity.getBodyY(0.5), entity.getZ(),
                        12, 0.1, 0.1, 0.1, 0.15
                );
            }
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        BlockPos pos = blockHitResult.getBlockPos();

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            BlockState state = serverWorld.getBlockState(pos);

            // Shatters ONLY normal Glass (Blocks.GLASS). Panes, Tinted Glass, and Stained Glass are safe!
            if (state.isOf(Blocks.GLASS)) {
                serverWorld.breakBlock(pos, false);
            }

            // Spawn block impact particles (block dust burst)
            serverWorld.spawnParticles(
                    new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                    blockHitResult.getPos().x, blockHitResult.getPos().y, blockHitResult.getPos().z,
                    10, 0.1, 0.1, 0.1, 0.15
            );

            // Spawn item particle burst
            ItemStack ammoStack = this.getStack();
            if (!ammoStack.isEmpty()) {
                serverWorld.spawnParticles(
                        new ItemStackParticleEffect(ParticleTypes.ITEM, ammoStack),
                        blockHitResult.getPos().x, blockHitResult.getPos().y, blockHitResult.getPos().z,
                        8, 0.1, 0.1, 0.1, 0.1
                );
            }
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient()) {
            this.getWorld().sendEntityStatus(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Break foliage (Grass, Flowers, Crops, Ferns) in flight
        if (!this.getWorld().isClient()) {
            BlockPos currentPos = this.getBlockPos();
            BlockState blockState = this.getWorld().getBlockState(currentPos);

            if (blockState.getBlock() instanceof PlantBlock
                    || blockState.getBlock() instanceof FlowerBlock
                    || blockState.getBlock() instanceof CropBlock
                    || blockState.getBlock() instanceof TallPlantBlock) {
                this.getWorld().breakBlock(currentPos, true); // Breaks block and drops item on ground
            }
        }

        // Particle trail
        if (this.getWorld().isClient()) {
            ItemStack ammoStack = this.getStack();
            if (ammoStack.isOf(ModItems.PEBBLE)) {
                this.getWorld().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0,
                        0.0);
            }
        }
    }

}
