package net.amaguri.goki.client;

import java.util.UUID;

import net.minecraft.client.Minecraft;

import net.amaguri.goki.net.StageSyncPayload;

/** Client copy of the server state. Filled by the StageSyncPayload receiver (W2). CONTRACT FILE. */
public final class ClientGokiState {
	private ClientGokiState() {
	}

	public static volatile UUID target = StageSyncPayload.NONE;
	public static volatile int stage = 0;

	public static boolean isTarget(UUID uuid) {
		return uuid != null && !StageSyncPayload.NONE.equals(target) && target.equals(uuid);
	}

	/** Stage for this player if it is the target, else 0. */
	public static int stageOf(UUID uuid) {
		return isTarget(uuid) ? stage : 0;
	}

	public static boolean selfIsTarget() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && isTarget(mc.player.getUUID());
	}

	public static void reset() {
		target = StageSyncPayload.NONE;
		stage = 0;
	}
}
