package dev.birinder.ac.entity.custom;

import dev.birinder.ac.sound.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class GamblerEntity extends PathAwareEntity {

    public GamblerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createGamblerAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new dev.birinder.ac.entity.ai.GamblerSeekTableGoal(this));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.6));
        this.goalSelector.add(4, new LookAroundGoal(this));
    }

    public void onMatchInitiated(LivingEntity challenger) {
        String[] lines = {
                "The dice are hot today, friend. Care to test your luck?",
                "Fortune favors the bold! Let's see your wager.",
                "Ah, a worthy challenger! Roll true, traveler."
        };
        int index = Math.abs((int) System.currentTimeMillis()) % lines.length;
        String line = lines[index];

        if (challenger instanceof PlayerEntity player) {
            player.sendMessage(Text.literal("The Gambler: \"" + line + "\"")
                    .formatted(Formatting.GOLD, Formatting.ITALIC), false);
        }
    }
}
