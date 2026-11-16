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
 * Always-on "next goal" text at the top left for everybody while a target exists. Plain vanilla text with shadow on a very
 * faint band; hidden with F1 (registered after a vanilla element) and F3. The goal line blinks gold for a moment when the stage rises.
 */
public final class NextGoalHud {
	private NextGoalHud() {
	}

	private static final int X = 4;
	private static final int Y = 4;
	private static final long FLASH_MS = 3000L;

	private static int lastStage = -1;
	private static long changedAt;

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (StageSyncPayload.NONE.equals(ClientGokiState.target)) {
			lastStage = -1;
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.getDebugOverlay().showDebugScreen()) {
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
		int goalW = font.width(goal);
		int levelW = (int) (font.width(level) * 0.75F);
		int w = Math.max(goalW, levelW);
		int lineH = font.lineHeight;
		// very faint band behind the two lines
		graphics.fill(X - 3, Y - 2, X + w + 4, Y + (int) (lineH * 0.75F) + 1 + lineH + 2, 0x28000000);

		graphics.pose().pushMatrix();
		graphics.pose().translate(X, Y);
		graphics.pose().scale(0.75F, 0.75F);
		graphics.text(font, level, 0, 0, 0xFFB8B8B8, true);
		graphics.pose().popMatrix();

		int color = 0xFFFFFFFF;
		long since = now - changedAt;
		if (changedAt != 0 && since < FLASH_MS) {
			color = (since / 250) % 2 == 0 ? 0xFFFFD35A : 0xFFFFFFFF;
		}
		graphics.text(font, goal, X, Y + (int) (lineH * 0.75F) + 1, color, true);
	}
}
