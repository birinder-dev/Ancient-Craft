package dev.birinder.ac.client;

import dev.birinder.ac.block.ModBlocks;
import dev.birinder.ac.client.gui.SpyglassHudOverlay;
import dev.birinder.ac.client.render.GamblerEntityRenderer;
import dev.birinder.ac.client.render.TavernVillagerEntityRenderer;
import dev.birinder.ac.client.speech.SpeechBubbleManager;
import dev.birinder.ac.entity.ModEntities;
import dev.birinder.ac.entity.custom.GamblerEntity;
import dev.birinder.ac.item.ModItems;
import dev.birinder.ac.sound.ModSounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import dev.birinder.ac.AncientCraft;
import dev.birinder.ac.client.render.GuardEntityRenderer;
import dev.birinder.ac.client.render.model.GuardEntityModel;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;

import dev.birinder.ac.entity.custom.TavernVillagerEntity;

public class AncientCraftClient implements ClientModInitializer {

	public static final EntityModelLayer GUARD_MODEL_LAYER = new EntityModelLayer(AncientCraft.id("guard"), "main");

	private static long lastThreatCheckTime = 0;
	private static long lastSeatedDialogueTime = 0;

	private static boolean canSpeak(Entity entity) {
		return entity instanceof GamblerEntity || entity instanceof TavernVillagerEntity;
	}

	private static final String[] HURT_LINES = {
		"Ouch! Do I look like a training dummy to you?!",
		"Hey! Keep your hands to yourself!",
		"Ow! What is your problem?!"
	};

	private static final String[] THREAT_LINES = {
		"Whoa! Point that pointy thing somewhere else!",
		"Put that blade away, I am just a simple villager!",
		"Is that sword really necessary for buying carrots?"
	};

	private static final String[] GREET_LINES = {
		"Greetings, traveler! Got emeralds, or just wasting my time?",
		"Welcome! Best prices in the biome, guaranteed."
	};

	private static final String[] TAVERN_AMBIENT_LINES = {
		"Looks like rain's brewing over the hills...",
		"My joints ache today. Weather's turning foul for sure.",
		"A fine crisp breeze today. Good for brewing ale.",
		"Grain prices are highway robbery these days.",
		"The blacksmith charged me two emeralds for a simple door hinge!",
		"Merchant caravan from the dunes brought sour wine again.",
		"Been tending fields since sunrise... my back is killing me.",
		"Tending the sheep all day with wolves lurking in the scrub...",
		"Another day, another harvest. At least the tavern is dry.",
		"Look at that wanderer strutting around with weapons drawn...",
		"These adventurers carry trouble wherever their boots step.",
		"Always rummaging through our barrels. What are they looking for?!"
	};

	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(GUARD_MODEL_LAYER, GuardEntityModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.SLINGSHOT_PROJECTILE, FlyingItemEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.GAMBLER, GamblerEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.TAVERN_VILLAGER, TavernVillagerEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.GUARD, GuardEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.SEAT, dev.birinder.ac.client.render.EmptyEntityRenderer::new);

		// Cutout rendering for ground pebble, table, and stools
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GROUND_PEBBLE, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GAMBLING_TABLE, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GAMBLING_STOOL, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TAVERN_STOOL, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TAVERN_TABLE, RenderLayer.getCutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.TAVERN_BENCH, RenderLayer.getCutout());

		// Register Spyglass HUD Inspection overlay
		HudRenderCallback.EVENT.register(new SpyglassHudOverlay());

		// Register Client Command: /cards [1-9 / off] and /cardhand
		net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			var cardsNode = net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("cards")
					.executes(context -> {
						boolean active = dev.birinder.ac.client.gambling.GamblingClientState.toggle(5);
						sendGamblingStateFeedback(context.getSource(), active, dev.birinder.ac.client.gambling.GamblingClientState.getCardCount());
						return 1;
					})
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("off")
							.executes(context -> {
								dev.birinder.ac.client.gambling.GamblingClientState.setActive(false);
								sendGamblingStateFeedback(context.getSource(), false, 0);
								return 1;
							}))
					.then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 9))
							.executes(context -> {
								int count = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "count");
								if (count <= 0) {
									dev.birinder.ac.client.gambling.GamblingClientState.setActive(false);
									sendGamblingStateFeedback(context.getSource(), false, 0);
								} else {
									dev.birinder.ac.client.gambling.GamblingClientState.setActive(true);
									dev.birinder.ac.client.gambling.GamblingClientState.setCardCount(count);
									sendGamblingStateFeedback(context.getSource(), true, count);
								}
								return 1;
							}));

			dispatcher.register(cardsNode);
			dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("cardhand")
					.redirect(dispatcher.getRoot().getChild("cards")));
		});

		// Register Speech Bubble countdown tick, proximity threat check, and ambient seated dialogue
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			SpeechBubbleManager.tick();
			checkWeaponThreatProximity(client);
			checkAmbientSeatedDialogue(client);
		});

		// 1. Being Hit: Random Hurt Line from locked-in v0 voice set
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient() && entity instanceof LivingEntity living && canSpeak(living)) {
				int randomLineIndex = player.getRandom().nextInt(HURT_LINES.length);
				SoundEvent sound = ModSounds.HURT_VOICES[randomLineIndex];
				SpeechBubbleManager.say(living, HURT_LINES[randomLineIndex], sound);
			}
			return ActionResult.PASS;
		});

		// 2. Right-Clicking: Random Greeting Line from locked-in v0 voice set
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient() && entity instanceof LivingEntity living && canSpeak(living) && hand == Hand.MAIN_HAND) {
				int randomLineIndex = player.getRandom().nextInt(GREET_LINES.length);
				SoundEvent sound = ModSounds.GREET_VOICES[randomLineIndex];
				SpeechBubbleManager.say(living, GREET_LINES[randomLineIndex], sound);
			}
			return ActionResult.PASS;
		});

		ModelPredicateProviderRegistry.register(
				ModItems.ANCIENT_TOOL,
				Identifier.ofVanilla("pulling"),
				(stack, world, entity,
						seed) -> entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1.0F
								: 0.0F);

		ModelPredicateProviderRegistry.register(
				ModItems.ANCIENT_TOOL,
				Identifier.ofVanilla("pull"),
				(stack, world, entity, seed) -> {
					if (entity == null) {
						return 0.0F;
					} else {
						return entity.getActiveItem() != stack ? 0.0F
								: (float) (stack.getMaxUseTime(entity) - entity.getItemUseTimeLeft()) / 20.0F;
					}
				});
	}

	// Proximity check: If player is within 4.5 blocks holding a drawn Sword/Axe facing a villager
	private static void checkWeaponThreatProximity(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastThreatCheckTime < 2500) { // Check every 2.5 seconds
			return;
		}

		var mainHand = client.player.getMainHandStack().getItem();
		boolean holdingWeapon = mainHand instanceof SwordItem || mainHand instanceof AxeItem;

		if (!holdingWeapon) {
			return;
		}

		HitResult crosshair = client.crosshairTarget;
		if (crosshair instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
			if (canSpeak(living) && client.player.distanceTo(living) <= 4.5F) {
				lastThreatCheckTime = now;
				int randomLineIndex = client.player.getRandom().nextInt(THREAT_LINES.length);
				SoundEvent sound = ModSounds.THREAT_VOICES[randomLineIndex];
				SpeechBubbleManager.say(living, THREAT_LINES[randomLineIndex], sound);
			}
		}
	}

	// Ambient Seated Banter: Exactly ONE seated villager in the vicinity speaks every 12-16 seconds via overhead speech bubble
	private static void checkAmbientSeatedDialogue(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastSeatedDialogueTime < 13000) { // Minimum 13 seconds between ambient chatter
			return;
		}

		net.minecraft.util.math.Box searchBox = client.player.getBoundingBox().expand(14.0);
		java.util.List<LivingEntity> seatedVillagers = client.world.getEntitiesByClass(
				LivingEntity.class,
				searchBox,
				e -> canSpeak(e) && e.hasVehicle() && e.getVehicle() instanceof dev.birinder.ac.entity.custom.SeatEntity
		);

		if (seatedVillagers.isEmpty()) {
			return;
		}

		lastSeatedDialogueTime = now;

		// Pick exactly ONE seated villager to speak
		LivingEntity speaker = seatedVillagers.get(client.world.random.nextInt(seatedVillagers.size()));
		int lineIndex = client.world.random.nextInt(TAVERN_AMBIENT_LINES.length);
		String dialogue = TAVERN_AMBIENT_LINES[lineIndex];

		int soundIndex = client.world.random.nextInt(ModSounds.GREET_VOICES.length);
		SoundEvent sound = ModSounds.GREET_VOICES[soundIndex];

		SpeechBubbleManager.say(speaker, dialogue, sound);
	}

	private static void sendGamblingStateFeedback(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source, boolean active, int count) {
		if (active) {
			source.sendFeedback(net.minecraft.text.Text.literal("♠ Gambling State: ACTIVE - Showing ")
					.formatted(net.minecraft.util.Formatting.GOLD, net.minecraft.util.Formatting.BOLD)
					.append(net.minecraft.text.Text.literal(count + (count == 1 ? " card" : " cards")).formatted(net.minecraft.util.Formatting.YELLOW, net.minecraft.util.Formatting.BOLD))
					.append(net.minecraft.text.Text.literal(" in hand.").formatted(net.minecraft.util.Formatting.GOLD)));
		} else {
			source.sendFeedback(net.minecraft.text.Text.literal("♠ Gambling State: DEACTIVATED - Inventory restored.")
					.formatted(net.minecraft.util.Formatting.GRAY));
		}
	}
}
