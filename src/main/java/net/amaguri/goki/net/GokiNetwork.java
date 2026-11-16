package net.amaguri.goki.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Registers all payload types (both sides). CONTRACT FILE. */
public final class GokiNetwork {
	private GokiNetwork() {
	}

	public static void registerPayloads() {
		PayloadTypeRegistry.clientboundPlay().register(StageSyncPayload.TYPE, StageSyncPayload.CODEC);
	}
}
