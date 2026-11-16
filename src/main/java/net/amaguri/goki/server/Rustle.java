package net.amaguri.goki.server;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.amaguri.goki.GokiStage;

/**
 * Stage 6+: while the target walks, now and then a faint, high-pitched scuttle (silverfish step) that the
 * people around can hear (not the target). Distance based, so it does not depend on player.tickCount.
 */
public final class Rustle {
	private Rustle() {
	}

	private static UUID lastId;
	private static ServerLevel lastLevel;
	private static double lastX;
	private static double lastZ;
	private static double walked;

	public static void tick(MinecraftServer server) {
		ServerPlayer p = GokiState.onlineTarget(server);
		if (p == null || !GokiStage.rustle(GokiState.stage())) {
			lastId = null;
			return;
		}
		ServerLevel level = p.level();
		double x = p.getX();
		double z = p.getZ();
		if (!p.getUUID().equals(lastId) || level != lastLevel) {
			lastId = p.getUUID();
			lastLevel = level;
			lastX = x;
			lastZ = z;
			walked = 0.0;
			return;
		}
		double d = Math.hypot(x - lastX, z - lastZ);
		lastX = x;
		lastZ = z;
		if (d > 2.0 || !p.onGround() || p.isShiftKeyDown() || p.isSpectator() || p.isPassenger() || p.isInWater()) {
			// teleports, air, sneaking and so on do not count as steps
			return;
		}
		walked += d;
		if (StageLogic.rustleDue(walked)) {
			walked = 0.0;
			if (p.getRandom().nextFloat() < StageLogic.RUSTLE_CHANCE) {
				float pitch = 1.6f + p.getRandom().nextFloat() * 0.4f;
				level.playSound(p, x, p.getY(), z, SoundEvents.SILVERFISH_STEP, SoundSource.PLAYERS, 0.3f, pitch);
			}
		}
	}
}
