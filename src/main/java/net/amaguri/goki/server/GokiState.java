package net.amaguri.goki.server;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import net.amaguri.goki.GokiStage;
import net.amaguri.goki.net.StageSyncPayload;

/**
 * Server-side truth: who the targets are (any number, all sharing one stage) and the current stage. Saved next to
 * the world as goki_state.json (written on every change). An old file with a single "target" is migrated on load.
 * Only touched from the server thread.
 */
public final class GokiState {
	private GokiState() {
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** One target: id and last known name. */
	public static final class Entry {
		public String uuid;
		public String name;
		/** Legacy: read once for migration, then nulled so it is no longer written. */
		public Boolean elytraGiven;

		public UUID id() {
			return UUID.fromString(uuid);
		}
	}

	/** JSON shape on disk. target/targetName/elytraGiven are the legacy single-target fields (read only). */
	private static final class Data {
		List<Entry> targets;
		List<String> elytraGivenIds;
		int stage;
		String target;
		String targetName;
		boolean elytraGiven;
	}

	private static final List<Entry> targets = new CopyOnWriteArrayList<>();
	/** Who already got the elytra. Independent of the target list (removing a target keeps it). */
	private static final Set<UUID> elytraGiven = ConcurrentHashMap.newKeySet();
	private static volatile int stage = 0;
	private static Path file;

	// ---- load / save ----

	public static void load(MinecraftServer server) {
		file = server.getWorldPath(LevelResource.ROOT).resolve("goki_state.json");
		reset();
		if (!Files.exists(file)) {
			return;
		}
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			Data d = GSON.fromJson(r, Data.class);
			if (d != null) {
				stage = StageLogic.clamp(d.stage);
				if (d.targets != null) {
					for (Entry e : d.targets) {
						if (e != null && e.uuid != null) {
							e.id(); // validates the UUID
							if (e.name == null) {
								e.name = "";
							}
							targets.add(e);
							if (Boolean.TRUE.equals(e.elytraGiven)) {
								elytraGiven.add(e.id());
							}
							e.elytraGiven = null;
						}
					}
					if (d.elytraGivenIds != null) {
						for (String id : d.elytraGivenIds) {
							elytraGiven.add(UUID.fromString(id));
						}
					}
				} else if (d.target != null && !d.target.isBlank()) { // migration from the single-target format
					Entry e = new Entry();
					e.uuid = UUID.fromString(d.target).toString();
					e.name = d.targetName == null ? "" : d.targetName;
					if (d.elytraGiven) {
						elytraGiven.add(e.id());
					}
					targets.add(e);
				}
			}
		} catch (Exception e) {
			System.err.println("[goki] goki_state.json is broken, starting fresh: " + e);
			reset();
		}
	}

	private static void reset() {
		targets.clear();
		elytraGiven.clear();
		stage = 0;
	}

	public static void unload() {
		file = null;
		reset();
	}

	private static void save() {
		if (file == null) {
			return;
		}
		Data d = new Data();
		d.targets = new ArrayList<>(targets);
		d.elytraGivenIds = new ArrayList<>();
		for (UUID id : elytraGiven) {
			d.elytraGivenIds.add(id.toString());
		}
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

	/** True while at least one target is set (otherwise the mod is inactive: vanilla). */
	public static boolean hasTargets() {
		return !targets.isEmpty();
	}

	/** Snapshot of all targets (online or not). */
	public static List<Entry> entries() {
		return List.copyOf(targets);
	}

	private static Entry find(UUID uuid) {
		for (Entry e : targets) {
			if (e.uuid.equals(uuid.toString())) {
				return e;
			}
		}
		return null;
	}

	public static boolean isTarget(UUID uuid) {
		return uuid != null && find(uuid) != null;
	}

	/** Stage for this player if it is a target, else 0 (matches the client-side contract). */
	public static int stageFor(UUID uuid) {
		return isTarget(uuid) ? stage : 0;
	}

	/** All targets that are currently online. */
	public static List<ServerPlayer> onlineTargets(MinecraftServer server) {
		List<ServerPlayer> out = new ArrayList<>();
		for (Entry e : targets) {
			ServerPlayer p = server.getPlayerList().getPlayer(e.id());
			if (p != null) {
				out.add(p);
			}
		}
		return out;
	}

	public static boolean elytraGiven(UUID uuid) {
		return elytraGiven.contains(uuid);
	}

	public static void markElytraGiven(UUID uuid) {
		if (elytraGiven.add(uuid)) {
			save();
		}
	}

	// ---- changes (each saves and syncs to every client) ----

	/** Adds a target (or refreshes its name). Returns true if it was new. */
	public static boolean addTarget(MinecraftServer server, ServerPlayer player) {
		Entry e = find(player.getUUID());
		boolean added = e == null;
		if (added) {
			e = new Entry();
			e.uuid = player.getUUID().toString();
			targets.add(e);
		}
		e.name = player.getPlainTextName();
		save();
		syncAll(server);
		return added;
	}

	/** Removes a target. Returns true if it was one. Items already given are not taken back. */
	public static boolean removeTarget(MinecraftServer server, UUID uuid) {
		Entry e = find(uuid);
		if (e == null) {
			return false;
		}
		targets.remove(e);
		save();
		syncAll(server);
		return true;
	}

	public static void clearTargets(MinecraftServer server) {
		targets.clear();
		save();
		syncAll(server);
	}

	/** Sets the stage (clamped). Returns the previous stage. */
	public static int setStage(MinecraftServer server, int newStage) {
		int old = stage;
		stage = StageLogic.clamp(newStage);
		if (!GokiStage.elytra(stage)) {
			elytraGiven.clear(); // set back below the elytra stage (re-shoots): may be granted again
		}
		save();
		syncAll(server);
		return old;
	}

	// ---- network ----

	private static StageSyncPayload payload() {
		List<UUID> ids = new ArrayList<>();
		for (Entry e : targets) {
			ids.add(e.id());
		}
		return new StageSyncPayload(ids, stage);
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
