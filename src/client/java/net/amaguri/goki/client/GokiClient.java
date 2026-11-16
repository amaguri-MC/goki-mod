package net.amaguri.goki.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;

import net.amaguri.goki.GokiHooks;
import net.amaguri.goki.GokiMod;
import net.amaguri.goki.client.render.AntennaHud;
import net.amaguri.goki.client.render.AntennaLayer;
import net.amaguri.goki.client.render.OverlayLayer;
import net.amaguri.goki.net.StageSyncPayload;

/** Client entrypoint (W2): state sync, render layers, first-person antenna HUD. */
public final class GokiClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// common code (climbing etc.) asks the client-side stage through this hook
		GokiHooks.CLIENT_STAGE = p -> ClientGokiState.stageOf(p.getUUID());

		// server -> client: who the target is and the current stage
		ClientPlayNetworking.registerGlobalReceiver(StageSyncPayload.TYPE, (payload, context) -> {
			ClientGokiState.target = payload.target();
			ClientGokiState.stage = payload.stage();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientGokiState.reset());

		// 3rd person: skin overlay + head antennae (only ever draw for the target, see GokiRender.stage)
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, helper, context) -> {
			if (entityRenderer instanceof AvatarRenderer<?> avatarRenderer) {
				helper.register(new OverlayLayer(avatarRenderer));
				helper.register(new AntennaLayer(avatarRenderer));
			}
		});

		// 1st person: antenna tips at the top of the screen. Attached after a vanilla element so it
		// inherits the vanilla render condition (hidden with F1).
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, GokiMod.id("antenna_hud"), AntennaHud::extract);
	}
}
