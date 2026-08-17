package dev.birinder.ac.client;

import dev.birinder.ac.block.ModBlocks;
import dev.birinder.ac.entity.ModEntities;
import dev.birinder.ac.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.util.Identifier;

import dev.birinder.ac.client.gui.SpyglassHudOverlay;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class AncientCraftClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.SLINGSHOT_PROJECTILE, FlyingItemEntityRenderer::new);

		// Cutout rendering for ground pebble block
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GROUND_PEBBLE, RenderLayer.getCutout());

		// Register Spyglass HUD Inspection overlay
		HudRenderCallback.EVENT.register(new SpyglassHudOverlay());

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
}
