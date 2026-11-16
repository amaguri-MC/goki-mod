package net.amaguri.goki.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;

import net.amaguri.goki.GokiAssets;
import net.amaguri.goki.GokiStage;
import net.amaguri.goki.client.ClientGokiState;
import net.amaguri.goki.client.render.GokiRender;

/** AvatarRenderer declares: extractRenderState(Avatar,...), shouldRenderLayers(AvatarRenderState), renderHand(...) (verified in apidump). */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> {
	/** Remember the target's stage in the render state so layers/renderers never have to look the entity up again. */
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void goki$extractStage(AvatarlikeEntity entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		state.setData(GokiRender.STAGE, ClientGokiState.stageOf(entity.getUUID()));
	}

	/** Stage 7+: the body is a cockroach, so no armor/cape/held item/etc. layers on top of it. */
	@Inject(method = "shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Z", at = @At("HEAD"), cancellable = true)
	private void goki$noLayersOnCockroach(AvatarRenderState state, CallbackInfoReturnable<Boolean> cir) {
		if (GokiRender.isCockroach(state)) {
			cir.setReturnValue(false);
		}
	}

	/**
	 * 1st person arm: vanilla has just drawn the skin arm; draw the brown overlay over it. Only the local player's arm
	 * ever goes through renderRightHand/renderLeftHand, so the target check is "am I the target".
	 */
	@Inject(method = "renderHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Lnet/minecraft/client/model/geom/ModelPart;Z)V", at = @At("TAIL"))
	private void goki$overlayHand(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin, ModelPart arm, boolean sleeveVisible, CallbackInfo ci) {
		if (!ClientGokiState.selfIsTarget()) {
			return;
		}
		int level = GokiStage.overlayLevel(ClientGokiState.stage);
		if (level <= 0 || level >= GokiAssets.OVERLAY.length) {
			return;
		}
		Identifier texture = GokiAssets.OVERLAY[level];
		if (!GokiRender.textureExists(texture)) {
			return;
		}
		collector.submitModelPart(arm, poseStack, RenderTypes.entityTranslucent(texture), light, OverlayTexture.NO_OVERLAY, null);
	}
}
