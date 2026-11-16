package net.amaguri.goki.client.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;

import net.amaguri.goki.GokiAssets;
import net.amaguri.goki.GokiStage;
import net.amaguri.goki.client.ClientGokiState;

/**
 * 1st person HUD: the tips of the target's own antennae peek in from the top edge of the screen and sway
 * with walking and camera movement. Hidden with F1 (registered after a vanilla HUD element, see GokiClient)
 * and in 3rd person. The texture (GokiAssets.ANTENNA_HUD, 128x64, left antenna = left half, right = right half,
 * hanging from the top edge) is made by W3; without it nothing is drawn.
 */
public final class AntennaHud {
	private AntennaHud() {
	}

	private static final int TEX_W = 128;
	private static final int TEX_H = 64;
	private static final int HALF_W = TEX_W / 2;
	/** How many GUI pixels of the image hang into the screen (the rest is cut off above the top edge). */
	private static final int PEEK = 30;
	/** Horizontal distance of each half's centre from the screen centre, in GUI pixels. */
	private static final int SPREAD = 22;

	private static float angLeft;
	private static float angRight;
	private static float lastYaw;
	private static float lastPitch;
	private static boolean haveLast;

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || !mc.options.getCameraType().isFirstPerson()) {
			haveLast = false;
			return;
		}
		if (!GokiStage.antennae(ClientGokiState.stageOf(player.getUUID()))) {
			return;
		}
		if (!GokiRender.textureExists(GokiAssets.ANTENNA_HUD)) {
			return;
		}

		// ---- sway: idle + walking bob + inertia from camera turns ----
		float time = (float) ((System.nanoTime() / 1.0e6) / 50.0 % 100000.0); // ticks, smooth per frame
		float yaw = player.getYRot();
		float pitch = player.getXRot();
		float dYaw = haveLast ? yaw - lastYaw : 0.0F;
		float dPitch = haveLast ? pitch - lastPitch : 0.0F;
		lastYaw = yaw;
		lastPitch = pitch;
		haveLast = true;
		float walk = Math.min(1.0F, player.walkAnimation.speed() * 3.0F);
		float walkPos = player.walkAnimation.position();
		float inertia = clamp(-dYaw * 0.9F + dPitch * 0.3F, -14.0F, 14.0F);
		float targetL = (float) Math.sin(time * 0.07F) * 2.5F + walk * (float) Math.sin(walkPos * 0.9F) * 4.5F + inertia;
		float targetR = (float) Math.sin(time * 0.07F + 1.9F) * 2.5F + walk * (float) Math.sin(walkPos * 0.9F + 0.8F) * 4.5F + inertia * 0.85F;
		float follow = clamp(deltaTracker.getRealtimeDeltaTicks() * 0.25F, 0.05F, 0.6F);
		angLeft += (targetL - angLeft) * follow;
		angRight += (targetR - angRight) * follow;

		int cx = graphics.guiWidth() / 2;
		int top = PEEK - TEX_H; // image top is above the screen so only the lower PEEK px (the tips) are visible
		drawHalf(graphics, cx - SPREAD, top, 0, angLeft);
		drawHalf(graphics, cx + SPREAD, top, HALF_W, angRight);
	}

	/** Draws one 64x64 half, hinged at its top centre (off screen) so the tips swing. */
	private static void drawHalf(GuiGraphicsExtractor graphics, int centerX, int top, int u, float degrees) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, top);
		graphics.pose().rotate((float) Math.toRadians(degrees));
		graphics.blit(RenderPipelines.GUI_TEXTURED, GokiAssets.ANTENNA_HUD, -HALF_W / 2, 0, (float) u, 0.0F, HALF_W, TEX_H, TEX_W, TEX_H);
		graphics.pose().popMatrix();
	}

	private static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : Math.min(v, hi);
	}
}
