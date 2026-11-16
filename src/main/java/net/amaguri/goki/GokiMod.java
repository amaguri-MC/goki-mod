package net.amaguri.goki;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

import net.amaguri.goki.net.GokiNetwork;
import net.amaguri.goki.server.GokiServer;

/** Main entrypoint. CONTRACT FILE (owner: 司令塔). */
public final class GokiMod implements ModInitializer {
	public static final String MODID = "goki";

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MODID, path);
	}

	@Override
	public void onInitialize() {
		GokiNetwork.registerPayloads();
		GokiParticles.init();
		GokiServer.init(); // owner: W1 (registers blocks, events, commands)
	}
}
