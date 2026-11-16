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

import net.amaguri.goki.GokiAssets;
import net.amaguri.goki.GokiStage;

/** Two cute curled antennae on the target's head (stage 5-6). Follows the head pose. */
public final class AntennaLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	public AntennaLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
		int stage = GokiRender.stage(state);
		if (!GokiStage.antennae(stage) || GokiStage.cockroachModel(stage) || state.isInvisible) {
			return;
		}
		PlayerModel player = this.getParentModel();
		player.setupAnim(state); // make sure the head pose matches this state (idempotent)
		AntennaModel model = AntennaModel.get();
		model.setupAnim(state);
		poseStack.pushPose();
		player.head.translateAndRotate(poseStack); // head pivot + rotation (+ crouch offsets)
		collector.submitModel(model, state, poseStack, RenderTypes.entityCutout(GokiAssets.ANTENNA_HEAD), light,
				LivingEntityRenderer.getOverlayCoords(state, 0.0F), EntityRenderState.NO_OUTLINE, null);
		poseStack.popPose();
	}
}
