package net.amaguri.goki.client;

import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;

/** Client copy of the server state. Filled by the StageSyncPayload receiver. CONTRACT FILE. */
public final class ClientGokiState {
	private ClientGokiState() {
	}

	/** All targets (share one stage). Empty = mod inactive. */
	public static volatile Set<UUID> targets = Set.of();
	public static volatile int stage = 0;

	public static boolean isTarget(UUID uuid) {
		return uuid != null && targets.contains(uuid);
	}

	/** True while at least one target is set (the mod is active for everyone). */
	public static boolean active() {
		return !targets.isEmpty();
	}

	/** Stage for this player if it is a target, else 0. */
	public static int stageOf(UUID uuid) {
		return isTarget(uuid) ? stage : 0;
	}

	public static boolean selfIsTarget() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && isTarget(mc.player.getUUID());
	}

	public static void reset() {
		targets = Set.of();
		stage = 0;
	}
}
