package dev.birinder.ac.client;

import dev.birinder.ac.block.ModBlocks;
import dev.birinder.ac.client.gui.SpyglassHudOverlay;
import dev.birinder.ac.client.render.GamblerEntityRenderer;
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
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class AncientCraftClient implements ClientModInitializer {

	private static long lastThreatCheckTime = 0;

	private static boolean canSpeak(Entity entity) {
		return entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity || entity instanceof GamblerEntity;
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

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.SLINGSHOT_PROJECTILE, FlyingItemEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.GAMBLER, GamblerEntityRenderer::new);
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

		// Register Speech Bubble countdown tick & proximity threat check
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			SpeechBubbleManager.tick();
			checkWeaponThreatProximity(client);
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
}
