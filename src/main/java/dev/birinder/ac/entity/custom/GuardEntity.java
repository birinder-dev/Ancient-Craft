package dev.birinder.ac.entity.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GuardEntity extends PathAwareEntity {

    public GuardEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createGuardAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 35.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.3)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F, 0.04F));
        this.goalSelector.add(3, new LookAtEntityGoal(this, GuardEntity.class, 6.0F, 0.02F));
        this.goalSelector.add(4, new LookAtEntityGoal(this, TavernVillagerEntity.class, 6.0F, 0.02F));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6, 100));
        this.goalSelector.add(6, new LookAroundGoal(this));

        // Target goals: Defend against attackers with group assistance, and protect against monsters
        this.targetSelector.add(1, new RevengeGoal(this, GuardEntity.class).setGroupRevenge(GuardEntity.class));
        this.targetSelector.add(2, new ActiveTargetGoal<net.minecraft.entity.mob.HostileEntity>(this,
                net.minecraft.entity.mob.HostileEntity.class, 10, true, false,
                entity -> !(entity instanceof CreeperEntity)));
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PLAYER_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PLAYER_DEATH;
    }
}
