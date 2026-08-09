package dev.birinder.ac.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class AncientToolItem extends Item {

    private static final int MAX_CHARGE_TIME = 30;

    public AncientToolItem(Settings settings) {
        super(settings);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return MAX_CHARGE_TIME;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public TypedActionResult<ItemStack> use(
            World world,
            PlayerEntity user,
            Hand hand
    ) {
        user.setCurrentHand(hand);
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void onStoppedUsing(
            ItemStack stack,
            World world,
            LivingEntity user,
            int remainingUseTicks
    ) {
        int chargeTicks = MAX_CHARGE_TIME - remainingUseTicks;

        if (!world.isClient) {
            System.out.println("Charged for " + chargeTicks + " ticks");
        }
    }
}