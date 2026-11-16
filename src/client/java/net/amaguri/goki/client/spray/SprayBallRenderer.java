package net.amaguri.goki.client.spray;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

import net.amaguri.goki.GokiAssets;

/** Draws the dragon fireball as a puff of insecticide mist (two soft billboards turning against each other). */
public final class SprayBallRenderer {
	private SprayBallRenderer() {
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("goki/spray");
	private static boolean failedOnce;
	private static final int FULL_BRIGHT = 0xF000F0;

	/** @return true if drawn (cancel vanilla), false = keep vanilla. */
	public static boolean submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (failedOnce) {
			return false;
		}
		try {
			float t = state.ageInTicks;
			float pulse = 1.0F + 0.07F * (float) Math.sin(t * 0.7F);
			poseStack.pushPose();
			poseStack.translate(0.0F, 0.5F, 0.0F);
			poseStack.mulPose(camera.orientation);
			poseStack.pushPose();
			poseStack.mulPose(Axis.ZP.rotationDegrees(t * 11.0F));
			collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(GokiAssets.SPRAY_BALL),
					(pose, vc) -> quad(pose, vc, 1.15F * pulse, 235));
			poseStack.popPose();
			poseStack.pushPose();
			poseStack.translate(0.0F, 0.0F, 0.01F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(-t * 7.0F + 40.0F));
			collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(GokiAssets.SPRAY_BALL),
					(pose, vc) -> quad(pose, vc, 0.8F * pulse, 170));
			poseStack.popPose();
			poseStack.popPose();
			return true;
		} catch (Throwable e) {
			failedOnce = true;
			LOGGER.error("Spray ball rendering failed; falling back to the vanilla fireball", e);
			return false;
		}
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer vc, float s, int alpha) {
		v(vc, pose, -s, s, 0.0F, 0.0F, alpha);
		v(vc, pose, -s, -s, 0.0F, 1.0F, alpha);
		v(vc, pose, s, -s, 1.0F, 1.0F, alpha);
		v(vc, pose, s, s, 1.0F, 0.0F, alpha);
	}

	private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v, int alpha) {
		vc.addVertex(pose, x, y, 0.0F)
				.setColor(255, 255, 255, alpha)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(FULL_BRIGHT)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}
}
