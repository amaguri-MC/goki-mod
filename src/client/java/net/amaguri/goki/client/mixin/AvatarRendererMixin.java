package net.amaguri.goki.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

	/** Level 4 (stage 7) is opaque: hide the skin's second layer (hat/jacket/sleeves/pants) so it cannot show through. */
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void goki$hideSecondLayer(AvatarlikeEntity entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		if (GokiStage.overlayLevel(GokiRender.stage(state)) >= 4) {
			state.showHat = false;
			state.showJacket = false;
			state.showLeftSleeve = false;
			state.showRightSleeve = false;
			state.showLeftPants = false;
			state.showRightPants = false;
		}
	}

	/** First-person: same for the sleeve layer of the arm. */
	@ModifyVariable(method = "renderHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Lnet/minecraft/client/model/geom/ModelPart;Z)V", at = @At("HEAD"), argsOnly = true)
	private boolean goki$noSleeve(boolean sleeveVisible) {
		return sleeveVisible && !(ClientGokiState.selfIsTarget() && GokiStage.overlayLevel(ClientGokiState.stage) >= 4);
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
		collector.submitModelPart(arm, poseStack, (level >= 4 ? RenderTypes.entityCutout(texture) : RenderTypes.entityTranslucent(texture)), light, OverlayTexture.NO_OVERLAY, null);
	}
}
