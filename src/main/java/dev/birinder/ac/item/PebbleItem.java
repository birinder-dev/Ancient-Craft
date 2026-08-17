package dev.birinder.ac.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class PebbleItem extends Item {

    public PebbleItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (stack.isOf(ModItems.PEBBLE)) {
            tooltip.add(Text.literal("A solid ROCK!").formatted(Formatting.GRAY));
            tooltip.add(Text.literal("Can be thrown using Slingshot").formatted(Formatting.DARK_GRAY));
        }
        super.appendTooltip(stack, context, tooltip, type);
    }
}
