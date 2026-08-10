package dev.birinder.ac.item;

import dev.birinder.ac.entity.projectile.SlingshotProjectileEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.function.Predicate;

public class AncientToolItem extends Item {

    public static final Predicate<ItemStack> SLINGSHOT_AMMO = stack -> stack.isOf(Items.SNOWBALL)
            || stack.isOf(Items.CLAY_BALL) || stack.isOf(ModItems.PEBBLE);

    // Charge duration to reach 100% power (20 ticks = 1 second)
    private static final int FULL_CHARGE_TICKS = 20;

    public AncientToolItem(Settings settings) {
        super(settings);
    }

    public ItemStack getAmmo(PlayerEntity player) {
        if (SLINGSHOT_AMMO.test(player.getStackInHand(Hand.OFF_HAND))) {
            return player.getStackInHand(Hand.OFF_HAND);
        }
        if (SLINGSHOT_AMMO.test(player.getStackInHand(Hand.MAIN_HAND))) {
            return player.getStackInHand(Hand.MAIN_HAND);
        }
        for (int i = 0; i < player.getInventory().size(); ++i) {
            ItemStack itemStack = player.getInventory().getStack(i);
            if (SLINGSHOT_AMMO.test(itemStack)) {
                return itemStack;
            }
        }
        return ItemStack.EMPTY;
    }

    // 72000 allows holding right-click indefinitely (like a Bow)
    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        boolean hasAmmo = !getAmmo(user).isEmpty();

        if (!user.getAbilities().creativeMode && !hasAmmo) {
            return TypedActionResult.fail(stack);
        } else {
            user.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        }
    }

    public static float getPullProgress(int useTicks) {
        float f = (float) useTicks / (float) FULL_CHARGE_TICKS;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        return f;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof PlayerEntity player) {
            int chargeTicks = this.getMaxUseTime(stack, user) - remainingUseTicks;
            float pullProgress = getPullProgress(chargeTicks);

            if ((double) pullProgress < 0.1D) {
                return;
            }

            // Finds ammo following Vanilla Priority (Off-Hand -> Hotbar -> Main Inventory)
            ItemStack ammoStack = getAmmo(player);
            boolean isCreative = player.getAbilities().creativeMode;

            if (ammoStack.isEmpty()) {
                if (isCreative) {
                    ammoStack = new ItemStack(ModItems.PEBBLE);
                } else {
                    return;
                }
            }

            if (!world.isClient) {
                SlingshotProjectileEntity projectile = new SlingshotProjectileEntity(world, player);

                // Set damage & visuals based on which projectile was found
                float baseDamage;
                if (ammoStack.isOf(ModItems.PEBBLE)) {
                    baseDamage = 4.0F; // Pebble
                } else if (ammoStack.isOf(Items.CLAY_BALL)) {
                    baseDamage = 3.0F; // Clay Ball
                } else if (ammoStack.isOf(Items.SNOWBALL)) {
                    baseDamage = 1.5F; // Snowball
                } else {
                    baseDamage = 2.0F;
                }

                float finalDamage = baseDamage * pullProgress;
                projectile.setDamage(finalDamage);

                // Set entity stack so the in-flight projectile renders as the thrown item
                projectile.setItem(ammoStack.copyWithCount(1));
                projectile.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, pullProgress * 2.5F, 1.0F);

                world.spawnEntity(projectile);

                EquipmentSlot slot = player.getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;
                stack.damage(1, player, slot);
            }

            // Play shoot sound
            world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_ARROW_SHOOT,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F) + pullProgress * 0.5F);

            // Consumes 1 item directly from the player's inventory slot
            if (!isCreative) {
                ammoStack.decrement(1);
            }
        }
    }

}
