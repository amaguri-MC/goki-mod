package net.amaguri.goki.client.spray;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;

import net.amaguri.goki.GokiParticles;

/**
 * Client-only show: while the dragon perches and breathes, the can sprays mist out of its nozzle.
 * (The server's lingering cloud is separate.) Uses the same pose maths as SprayCanRenderer.
 */
public final class SprayBreath {
	private SprayBreath() {
	}

	public static void init() {
		ClientTickEvents.END_LEVEL_TICK.register(SprayBreath::tick);
	}

	private static void tick(ClientLevel level) {
		if (!SprayState.active()) {
			return;
		}
		for (Entity entity : level.entitiesForRendering()) {
			if (entity instanceof EnderDragon dragon && dragon.isAlive()
					&& dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.SITTING_FLAMING) {
				spray(level, dragon);
			}
		}
	}

	private static void spray(ClientLevel level, EnderDragon dragon) {
		Quaternionf q = new Quaternionf()
				.rotateY((float) Math.toRadians(-SprayState.yaw(dragon)))
				.rotateX((float) Math.toRadians(SprayState.pitch(dragon)))
				.rotateY((float) Math.PI);
		Vector3f tip = q.transform(new Vector3f(SprayCanGeometry.NOZZLE_X, SprayCanGeometry.NOZZLE_Y, SprayCanGeometry.NOZZLE_Z + SprayCanGeometry.Z_SHIFT));
		Vector3f dir = q.transform(new Vector3f(0.0F, 0.0F, 1.0F));
		double px = dragon.getX() + tip.x();
		double py = dragon.getY() + SprayState.CENTER_Y + tip.y();
		double pz = dragon.getZ() + tip.z();
		var random = level.getRandom();
		for (int i = 0; i < 5; i++) {
			double speed = 0.55 + random.nextDouble() * 0.45;
			double sx = (random.nextDouble() - 0.5) * 0.22;
			double sy = (random.nextDouble() - 0.5) * 0.22 - 0.03;
			double sz = (random.nextDouble() - 0.5) * 0.22;
			level.addParticle(GokiParticles.SPRAY, px, py, pz, dir.x() * speed + sx, dir.y() * speed + sy, dir.z() * speed + sz);
		}
	}
}
