package net.amaguri.goki.client.spray;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

import net.amaguri.goki.GokiAssets;

/** Draws the insecticide spray can in place of the Ender Dragon body. Called from EnderDragonRendererMixin. */
public final class SprayCanRenderer {
	private SprayCanRenderer() {
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("goki/spray");
	private static boolean failedOnce;

	/** @return true if the can was submitted (the vanilla body must then be hidden); false = keep vanilla. */
	public static boolean submit(EnderDragonRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		if (failedOnce) {
			return false;
		}
		try {
			float yaw = SprayState.yaw(state);
			float pitch = SprayState.pitch(state);
			float t = state.ageInTicks;
			boolean calm = state.isSitting;
			float sway = calm ? 0.0F : 1.0F;
			float roll = (float) Math.sin(t * 0.13F) * 5.0F * sway;
			float wobble = (float) Math.cos(t * 0.09F) * 2.5F * sway;
			float bob = (float) Math.sin(t * 0.11F) * 0.18F * sway;
			float scale = 1.0F;
			float shake = 0.0F;
			if (state.deathTime > 0.0F) {
				// vanilla keeps playing its death rays / beams; the can trembles and shrinks away in the last stretch
				shake = (float) Math.sin(state.deathTime * 2.1F) * 3.0F;
				scale = 1.0F - smooth((state.deathTime - 150.0F) / 50.0F);
				if (scale <= 0.01F) {
					return true;
				}
			}
			int light = state.lightCoords;
			int overlay = OverlayTexture.pack(0.0F, state.hasRedOverlay);

			poseStack.pushPose();
			poseStack.translate(0.0F, SprayState.CENTER_Y + bob, 0.0F);
			// same frame as vanilla: the dragon's visual front is -Z at yaw 0, so the can (nose +Z) gets a 180 turn first
			poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
			poseStack.mulPose(Axis.XP.rotationDegrees(pitch + wobble));
			poseStack.mulPose(Axis.ZP.rotationDegrees(roll + shake));
			poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
			poseStack.scale(scale, scale, scale);
			poseStack.translate(0.0F, 0.0F, SprayCanGeometry.Z_SHIFT);
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(GokiAssets.SPRAY_CAN),
					(pose, consumer) -> emit(pose, consumer, light, overlay));
			poseStack.popPose();
			return true;
		} catch (Throwable e) {
			failedOnce = true;
			LOGGER.error("Spray can rendering failed; falling back to the vanilla dragon", e);
			return false;
		}
	}

	private static float smooth(float x) {
		x = Math.max(0.0F, Math.min(1.0F, x));
		return x * x * (3.0F - 2.0F * x);
	}

	private static void emit(PoseStack.Pose pose, VertexConsumer vc, int light, int overlay) {
		float iw = 1.0F / SprayCanGeometry.ATLAS_W;
		float ih = 1.0F / SprayCanGeometry.ATLAS_H;
		for (float[] part : SprayCanGeometry.PARTS) {
			for (int q = 0; q + 20 <= part.length; q += 20) {
				// flat normal from the first, second and fourth vertex (CCW seen from outside)
				float ax = part[q + 5] - part[q], ay = part[q + 6] - part[q + 1], az = part[q + 7] - part[q + 2];
				float bx = part[q + 15] - part[q], by = part[q + 16] - part[q + 1], bz = part[q + 17] - part[q + 2];
				float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
				float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
				if (len < 1.0E-6F) {
					nx = 0.0F; ny = 1.0F; nz = 0.0F;
				} else {
					nx /= len; ny /= len; nz /= len;
				}
				for (int k = 0; k < 4; k++) {
					int o = q + k * 5;
					vc.addVertex(pose, part[o], part[o + 1], part[o + 2])
							.setColor(-1)
							.setUv(part[o + 3] * iw, part[o + 4] * ih)
							.setOverlay(overlay)
							.setLight(light)
							.setNormal(pose, nx, ny, nz);
				}
			}
		}
	}
}
