package net.amaguri.goki.server;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import net.amaguri.goki.net.StageSyncPayload;

/**
 * Server-side truth: who the target is and the current stage. Saved next to the world as goki_state.json
 * (written on every change, so a crash loses nothing). Only touched from the server thread.
 */
public final class GokiState {
	private GokiState() {
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** JSON shape on disk. */
	private static final class Data {
		String target;
		String targetName;
		int stage;
	}

	private static volatile UUID target = null;
	private static volatile String targetName = "";
	private static volatile int stage = 0;
	private static Path file;

	// ---- load / save ----

	public static void load(MinecraftServer server) {
		file = server.getWorldPath(LevelResource.ROOT).resolve("goki_state.json");
		target = null;
		targetName = "";
		stage = 0;
		if (!Files.exists(file)) {
			return;
		}
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			Data d = GSON.fromJson(r, Data.class);
			if (d != null) {
				stage = StageLogic.clamp(d.stage);
				targetName = d.targetName == null ? "" : d.targetName;
				if (d.target != null && !d.target.isBlank()) {
					target = UUID.fromString(d.target);
				}
			}
		} catch (Exception e) {
			System.err.println("[goki] goki_state.json is broken, starting fresh: " + e);
			target = null;
			targetName = "";
			stage = 0;
		}
	}

	public static void unload() {
		file = null;
		target = null;
		targetName = "";
		stage = 0;
	}

	private static void save() {
		if (file == null) {
			return;
		}
		Data d = new Data();
		d.target = target == null ? null : target.toString();
		d.targetName = targetName;
		d.stage = stage;
		try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			GSON.toJson(d, w);
		} catch (Exception e) {
			System.err.println("[goki] could not write goki_state.json: " + e);
		}
	}

	// ---- queries ----

	public static int stage() {
		return stage;
	}

	public static UUID target() {
		return target;
	}

	public static String targetName() {
		return targetName;
	}

	public static boolean isTarget(UUID uuid) {
		UUID t = target;
		return t != null && t.equals(uuid);
	}

	/** Stage for this player if it is the target, else 0 (matches the client-side contract). */
	public static int stageFor(UUID uuid) {
		return isTarget(uuid) ? stage : 0;
	}

	/** The target if currently online, else null. */
	public static ServerPlayer onlineTarget(MinecraftServer server) {
		UUID t = target;
		return t == null ? null : server.getPlayerList().getPlayer(t);
	}

	// ---- changes (each saves and syncs to every client) ----

	public static void setTarget(MinecraftServer server, ServerPlayer player) {
		target = player == null ? null : player.getUUID();
		targetName = player == null ? "" : player.getPlainTextName();
		save();
		syncAll(server);
	}

	/** Sets the stage (clamped). Returns the previous stage. */
	public static int setStage(MinecraftServer server, int newStage) {
		int old = stage;
		stage = StageLogic.clamp(newStage);
		save();
		syncAll(server);
		return old;
	}

	// ---- network ----

	private static StageSyncPayload payload() {
		UUID t = target;
		return new StageSyncPayload(t == null ? StageSyncPayload.NONE : t, stage);
	}

	public static void syncTo(ServerPlayer player) {
		ServerPlayNetworking.send(player, payload());
	}

	public static void syncAll(MinecraftServer server) {
		StageSyncPayload p = payload();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, p);
		}
	}

}
