package net.amaguri.goki.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.amaguri.goki.GokiStage;

/**
 * GokiStage.rustle: while a target walks, now and then a faint, high-pitched scuttle (silverfish step) that the
 * people around can hear (not the target). Distance based, so it does not depend on player.tickCount.
 */
public final class Rustle {
	private Rustle() {
	}

	/** Per-target walking state. */
	private static final class Track {
		ServerLevel level;
		double x;
		double z;
		double walked;
	}

	private static final Map<UUID, Track> TRACKS = new HashMap<>();

	public static void tick(MinecraftServer server) {
		if (!GokiStage.rustle(GokiState.stage())) {
			TRACKS.clear();
			return;
		}
		List<ServerPlayer> targets = GokiState.onlineTargets(server);
		TRACKS.keySet().removeIf(id -> targets.stream().noneMatch(p -> p.getUUID().equals(id)));
		for (ServerPlayer p : targets) {
			step(p);
		}
	}

	private static void step(ServerPlayer p) {
		ServerLevel level = p.level();
		double x = p.getX();
		double z = p.getZ();
		Track t = TRACKS.get(p.getUUID());
		if (t == null || t.level != level) {
			t = new Track();
			t.level = level;
			t.x = x;
			t.z = z;
			TRACKS.put(p.getUUID(), t);
			return;
		}
		double d = Math.hypot(x - t.x, z - t.z);
		t.x = x;
		t.z = z;
		if (d > 2.0 || !p.onGround() || p.isShiftKeyDown() || p.isSpectator() || p.isPassenger() || p.isInWater()) {
			// teleports, air, sneaking and so on do not count as steps
			return;
		}
		t.walked += d;
		if (StageLogic.rustleDue(t.walked)) {
			t.walked = 0.0;
			if (p.getRandom().nextFloat() < StageLogic.RUSTLE_CHANCE) {
				float pitch = 1.6f + p.getRandom().nextFloat() * 0.4f;
				level.playSound(p, x, p.getY(), z, SoundEvents.SILVERFISH_STEP, SoundSource.PLAYERS, 0.3f, pitch);
			}
		}
	}
}
