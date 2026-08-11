package dev.birinder.ac.entity.projectile;

import dev.birinder.ac.entity.ModEntities;
import dev.birinder.ac.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

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

        if (!this.getWorld().isClient()) {
            entity.damage(this.getDamageSources().thrown(this, this.getOwner()), this.damage);

            if (entity instanceof LivingEntity target) {
                ItemStack ammoStack = this.getStack();

                if (ammoStack.isOf(ModItems.POISON_PEBBLE)) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 120, 0));
                } else if (ammoStack.isOf(ModItems.SOPORIFIC_STONE)) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 160, 3));
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 1));
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 160, 0));
                } else if (ammoStack.isOf(ModItems.FIRE_PEBBLE)) {
                    target.setOnFireFor(5);
                }
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

        // Spawn white straight streak trail (like RLCraft longbows) for normal pebble
        if (this.getWorld().isClient()) {
            ItemStack ammoStack = this.getStack();

            if (ammoStack.isOf(ModItems.PEBBLE)) {
                // White straight line streak trail
                this.getWorld().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

}
