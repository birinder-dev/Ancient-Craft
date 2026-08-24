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
		dev.birinder.ac.block.entity.ModBlockEntities.registerBlockEntities();
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
            
            // Register /forcesit command for debugging villagers
			dispatcher.register(net.minecraft.server.command.CommandManager.literal("forcesit").executes(context -> {
				net.minecraft.server.network.ServerPlayerEntity player = context.getSource().getPlayer();
				if (player != null) {
					net.minecraft.world.World world = player.getWorld();
					net.minecraft.util.math.BlockPos playerPos = player.getBlockPos();
					java.util.List<net.minecraft.entity.passive.VillagerEntity> villagers = world.getEntitiesByClass(
							net.minecraft.entity.passive.VillagerEntity.class,
							new net.minecraft.util.math.Box(playerPos).expand(10.0),
							v -> !v.hasVehicle()
					);
					int count = 0;
					for (net.minecraft.entity.passive.VillagerEntity villager : villagers) {
						net.minecraft.util.math.BlockPos nearestBench = null;
						double nearestDist = Double.MAX_VALUE;
						for (net.minecraft.util.math.BlockPos pos : net.minecraft.util.math.BlockPos.iterate(villager.getBlockPos().add(-5, -2, -5), villager.getBlockPos().add(5, 2, 5))) {
							if (world.getBlockState(pos).getBlock() instanceof dev.birinder.ac.block.TavernBenchBlock) {
								if (!dev.birinder.ac.util.SitUtil.isSeatOccupied(world, pos)) {
									double distSq = villager.squaredDistanceTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
									if (distSq < nearestDist) {
										nearestDist = distSq;
										nearestBench = pos.toImmutable();
									}
								}
							}
						}
						if (nearestBench != null) {
							if (dev.birinder.ac.util.SitUtil.sitEntity(world, nearestBench, villager, dev.birinder.ac.util.SitUtil.BENCH_OFFSET)) {
								count++;
							}
						}
					}
					player.sendMessage(net.minecraft.text.Text.literal("Forced " + count + " villagers to sit on nearby benches."), false);
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
