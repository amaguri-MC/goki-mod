package net.amaguri.goki.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

import net.amaguri.goki.GokiAssets;

/** Draws the cockroach in place of the player model (stage 7+). Called from LivingEntityRendererMixin. */
public final class CockroachRenderer {
	private CockroachRenderer() {
	}

	/** Overall size of the cockroach relative to the model definition (about player size). */
	private static final float SCALE = 0.8F;

	public static void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		if (state.isInvisible || state.isSpectator) {
			return;
		}
		CockroachModel model = CockroachModel.get();
		model.setupAnim(state);
		poseStack.pushPose();
		// same frame as LivingEntityRenderer.submit: origin = feet, rotate to body yaw, then model space (y down)
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.bodyRot));
		if (state.deathTime > 0.0F) {
			float f = (float) Math.sqrt((state.deathTime - 1.0F) / 20.0F * 1.6F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(Math.min(f, 1.0F) * 90.0F));
		}
		poseStack.scale(-SCALE, -SCALE, SCALE);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		collector.submitModel(model, state, poseStack, RenderTypes.entityCutout(GokiAssets.COCKROACH), state.lightCoords,
				LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor, null);
		poseStack.popPose();
	}
}
