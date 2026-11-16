package net.amaguri.goki.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerModelType;

import net.amaguri.goki.GokiAssets;
import net.amaguri.goki.GokiStage;
import net.amaguri.goki.client.ClientGokiState;
import net.amaguri.goki.client.render.GokiRender;
import net.amaguri.goki.client.render.OverlayModels;

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

	/** First-person (vanilla path: ItemInHandRenderer.renderPlayerArm -> renderRightHand/renderLeftHand): hide the sleeve layer at stage 7. */
	@ModifyVariable(method = "renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V", at = @At("HEAD"), argsOnly = true)
	private boolean goki$noSleeveR(boolean sleeveVisible) {
		return sleeveVisible && !goki$level4();
	}

	@ModifyVariable(method = "renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V", at = @At("HEAD"), argsOnly = true)
	private boolean goki$noSleeveL(boolean sleeveVisible) {
		return sleeveVisible && !goki$level4();
	}

	/** First-person arm: after vanilla drew the skin arm, draw the overlay on an inflated arm (outside the sleeve layer). */
	@Inject(method = "renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V", at = @At("TAIL"))
	private void goki$overlayRight(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeveVisible, CallbackInfo ci) {
		goki$overlayArm(poseStack, collector, light, true);
	}

	@Inject(method = "renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V", at = @At("TAIL"))
	private void goki$overlayLeft(PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeveVisible, CallbackInfo ci) {
		goki$overlayArm(poseStack, collector, light, false);
	}

	private static boolean goki$level4() {
		return ClientGokiState.selfIsTarget() && GokiStage.overlayLevel(ClientGokiState.stage) >= 4;
	}

	private static void goki$overlayArm(PoseStack poseStack, SubmitNodeCollector collector, int light, boolean right) {
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
		Minecraft mc = Minecraft.getInstance();
		boolean slim = mc.player != null && mc.player.getSkin().model() == PlayerModelType.SLIM;
		PlayerModel model = OverlayModels.get(slim);
		ModelPart arm = right ? model.rightArm : model.leftArm;
		arm.resetPose();
		arm.visible = true;
		model.leftSleeve.visible = false;
		model.rightSleeve.visible = false;
		arm.zRot = right ? 0.1F : -0.1F; // same pose tweak vanilla applies to the first-person arm
		collector.submitModelPart(arm, poseStack, RenderTypes.entityTranslucent(texture), light, OverlayTexture.NO_OVERLAY, null);
	}
}
