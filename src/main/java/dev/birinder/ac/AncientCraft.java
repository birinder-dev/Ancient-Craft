package dev.birinder.ac;

import dev.birinder.ac.block.ModBlocks;
import dev.birinder.ac.entity.ModEntities;
import dev.birinder.ac.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AncientCraft implements ModInitializer {

	public static final String MOD_ID = "ancient_craft";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModEntities.registerModEntities();
		dev.birinder.ac.sound.ModSounds.initialize();
		dev.birinder.ac.worldgen.ModWorldGen.generateWorldGen();

		// Register /sit command
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(net.minecraft.server.command.CommandManager.literal("sit").executes(context -> {
				net.minecraft.server.network.ServerPlayerEntity player = context.getSource().getPlayer();
				if (player != null) {
					dev.birinder.ac.util.SitUtil.sitPlayer(player.getWorld(), player.getBlockPos(), player, 0.0);
				}
				return 1;
			}));
		});

		// Register Right-Click Sit Callback on Stairs, Slabs, and Logs (when empty-handed)
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClient() && hand == net.minecraft.util.Hand.MAIN_HAND && player.getStackInHand(hand).isEmpty()) {
				net.minecraft.util.math.BlockPos pos = hitResult.getBlockPos();
				net.minecraft.block.BlockState state = world.getBlockState(pos);
				if (state.getBlock() instanceof net.minecraft.block.StairsBlock
						|| state.getBlock() instanceof net.minecraft.block.SlabBlock
						|| state.getBlock() instanceof net.minecraft.block.PillarBlock) {
					if (dev.birinder.ac.util.SitUtil.sitPlayer(world, pos, player, 0.2)) {
						return net.minecraft.util.ActionResult.SUCCESS;
					}
				}
			}
			return net.minecraft.util.ActionResult.PASS;
		});

		LOGGER.info("Ancient Craft initialized with Sitting Features!");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
