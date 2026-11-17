package net.amaguri.goki.client.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import net.amaguri.goki.GokiStage;
import net.amaguri.goki.client.ClientGokiState;
import net.amaguri.goki.net.StageSyncPayload;

/**
 * Always-on "next goal" box at the top centre for everybody while a target exists. Vanilla-tooltip style box (dark translucent + 1px border) that also hides the
 * first-person antenna tips (AntennaHud); hidden with F1 (registered after a vanilla element) and F3. The goal line blinks gold for a moment when the stage rises.
 */
public final class NextGoalHud {
	private NextGoalHud() {
	}

	private static final long FLASH_MS = 3000L;

	private static int lastStage = -1;
	private static long changedAt;

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!ClientGokiState.active()) {
			lastStage = -1;
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen()) {
			return;
		}
		int stage = Math.max(0, Math.min(GokiStage.MAX, ClientGokiState.stage));
		long now = Util.getMillis();
		if (lastStage >= 0 && stage != lastStage) {
			changedAt = now;
		}
		lastStage = stage;

		Font font = mc.font;
		Component level = Component.translatable("goki.hud.level", stage);
		Component goal = Component.translatable("goki.hud.next_goal", Component.translatable("goki.hud.goal." + stage));
		int lineH = font.lineHeight;
		float lvScale = 0.75F;
		int lvH = (int) (lineH * lvScale);
		// box wide/tall enough to hide the antenna tips of AntennaHud (they hang PEEK px from the top edge, SPREAD px either side of centre)
		int halfW = Math.max(font.width(goal) / 2 + 10, AntennaHud.SPREAD + 24);
		int boxH = Math.max(AntennaHud.PEEK + 2, lvH + lineH + 10);
		int cx = graphics.guiWidth() / 2;
		var boss = mc.gui.hud.getBossOverlay();
		int top = (boss.shouldDarkenScreen() || boss.shouldCreateWorldFog()) ? 36 : 0; // below the (dragon) boss bar
		int x0 = cx - halfW;
		int x1 = cx + halfW;
		int y1 = top + boxH;
		// vanilla-tooltip look: dark translucent rectangle + 1px thin border (no rounding, no gradient)
		graphics.fill(x0, top, x1, y1, 0xC8100010);
		graphics.fill(x0, top, x1, top + 1, 0xFF3A2A5A);
		graphics.fill(x0, y1 - 1, x1, y1, 0xFF3A2A5A);
		graphics.fill(x0, top, x0 + 1, y1, 0xFF3A2A5A);
		graphics.fill(x1 - 1, top, x1, y1, 0xFF3A2A5A);

		int textTop = top + (boxH - (lvH + 1 + lineH)) / 2;
		graphics.pose().pushMatrix();
		graphics.pose().translate(cx - font.width(level) * lvScale / 2.0F, textTop);
		graphics.pose().scale(lvScale, lvScale);
		graphics.text(font, level, 0, 0, 0xFFB8B8B8, true);
		graphics.pose().popMatrix();

		int color = 0xFFFFFFFF;
		long since = now - changedAt;
		if (changedAt != 0 && since < FLASH_MS) {
			color = (since / 250) % 2 == 0 ? 0xFFFFD35A : 0xFFFFFFFF;
		}
		graphics.text(font, goal, cx - font.width(goal) / 2, textTop + lvH + 1, color, true);
	}
}
