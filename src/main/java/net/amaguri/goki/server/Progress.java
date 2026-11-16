package net.amaguri.goki.server;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.amaguri.goki.GokiStage;

/** Raises the stage when anybody completes the matching advancement. Never lowers it. */
public final class Progress {
	private Progress() {
	}

	public static void check(MinecraftServer server) {
		int current = GokiState.stage();
		if (current >= GokiStage.MAX_ADVANCEMENT_STAGE) {
			return;
		}
		int next = StageLogic.nextStage(current, n -> anyoneDone(server, n));
		if (next > current) {
			GokiState.setStage(server, next);
			Enhancements.applyAll(server);
			LevelUp.announce(server, current, next);
		}
	}

	private static boolean anyoneDone(MinecraftServer server, int n) {
		String raw = GokiStage.ADVANCEMENTS[n];
		if (raw == null) {
			return false;
		}
		Identifier id = Identifier.tryParse(raw);
		if (id == null) {
			return false;
		}
		AdvancementHolder holder = server.getAdvancements().get(id);
		if (holder == null) {
			return false;
		}
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (p.getAdvancements().getOrStartProgress(holder).isDone()) {
				return true;
			}
		}
		return false;
	}
}
