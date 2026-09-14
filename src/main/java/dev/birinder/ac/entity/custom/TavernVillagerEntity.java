package dev.birinder.ac.entity.custom;

import dev.birinder.ac.entity.ai.TavernVillagerSeekBenchGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerDataContainer;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class TavernVillagerEntity extends PathAwareEntity implements VillagerDataContainer {

    private VillagerData villagerData;

    public TavernVillagerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.villagerData = new VillagerData(VillagerType.PLAINS, VillagerProfession.FARMER, 1);
    }

    public static DefaultAttributeContainer.Builder createTavernVillagerAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new TavernVillagerSeekBenchGoal(this));
        this.goalSelector.add(2, new StopAndLookAtEntityGoal(this, PlayerEntity.class, 4.0F, 0.04F));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F, 0.02F));
        this.goalSelector.add(4, new LookAtEntityGoal(this, TavernVillagerEntity.class, 6.0F, 0.02F));
        this.goalSelector.add(5, new LookAtEntityGoal(this, VillagerEntity.class, 6.0F, 0.02F));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.5, 120));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }

    @Override
    public VillagerData getVillagerData() {
        return this.villagerData;
    }

    @Override
    public void setVillagerData(VillagerData villagerData) {
        this.villagerData = villagerData;
    }
}
