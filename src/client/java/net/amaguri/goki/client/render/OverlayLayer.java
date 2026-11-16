package net.amaguri.goki.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import net.amaguri.goki.GokiAssets;
import net.amaguri.goki.GokiStage;

/** 3rd person: draws GokiAssets.OVERLAY[level] translucently ON TOP of the player's own skin (target only). */
public final class OverlayLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public OverlayLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		int level = GokiStage.overlayLevel(GokiRender.stage(state));
		if (level <= 0 || level >= GokiAssets.OVERLAY.length || state.isInvisible) {
			return;
		}
		Identifier texture = GokiAssets.OVERLAY[level];
		if (!GokiRender.textureExists(texture)) {
			return;
		}
		poseStack.pushPose();
		// a hair larger than the body so the translucent overlay never z-fights with the skin
		poseStack.scale(1.002F, 1.002F, 1.002F);
		collector.submitModel(this.getParentModel(), state, poseStack, RenderTypes.entityTranslucent(texture), light,
				LivingEntityRenderer.getOverlayCoords(state, 0.0F), EntityRenderState.NO_OUTLINE, null);
		poseStack.popPose();
	}
}
