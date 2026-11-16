package net.amaguri.goki.server;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;

import net.amaguri.goki.GokiHooks;

/** Server entry (called from GokiMod.onInitialize): state, events, commands. */
public final class GokiServer {
	private GokiServer() {
	}

	public static void init() {
		// the mixins (wall climbing, hunger) ask this on the logical server
		GokiHooks.SERVER_STAGE = player -> GokiState.stageFor(player.getUUID());

		ServerLifecycleEvents.SERVER_STARTING.register(GokiState::load);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> GokiState.unload());

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			// everybody gets the state (joiner needs it; harmless for the rest), and the joiner gets the buffs back
			GokiState.syncAll(server);
			Enhancements.apply(handler.getPlayer(), GokiState.stage());
		});
		// a new ServerPlayer instance after death loses transient modifiers and effects: reapply at once
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> Enhancements.apply(newPlayer, GokiState.stage()));

		ServerTickEvents.END_SERVER_TICK.register(GokiServer::tick);

		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> GokiCommands.register(dispatcher));
	}

	private static void tick(MinecraftServer server) {
		if (server.getPlayerList().getPlayers().isEmpty()) {
			return;
		}
		int t = server.getTickCount();
		if (t % 20 == 0) {
			Progress.check(server);
			ElytraGrant.check(server);
			Enhancements.applyAll(server); // keeps buffs alive across logout, death, dimension change, milk
		}
		SprayClouds.tick(server);
		Rustle.tick(server);
	}
}
