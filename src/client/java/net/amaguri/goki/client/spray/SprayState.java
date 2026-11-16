package net.amaguri.goki.client.spray;

import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;

import net.amaguri.goki.client.ClientGokiState;
import net.amaguri.goki.net.StageSyncPayload;

/** Shared bits of the spray-can look (render + particles). */
public final class SprayState {
	private SprayState() {
	}

	/** Height of the can axis above the dragon's position (the can's belly touches the ground when perched). */
	public static final float CENTER_Y = 2.2F;

	/** True while a target is set: the dragon is a spray can for everybody. */
	public static boolean active() {
		return !StageSyncPayload.NONE.equals(ClientGokiState.target);
	}

	/** Same yaw source as vanilla's dragon renderer (flight history, 7 ticks back). */
	public static float yaw(EnderDragonRenderState state) {
		return state.getHistoricalPos(7).yRot();
	}

	/** Same pitch source as vanilla (height change between 5 and 10 ticks back), degrees. */
	public static float pitch(EnderDragonRenderState state) {
		DragonFlightHistory.Sample a = state.getHistoricalPos(5);
		DragonFlightHistory.Sample b = state.getHistoricalPos(10);
		return clampPitch((float) (a.y() - b.y()) * 10.0F);
	}

	public static float yaw(EnderDragon dragon) {
		return dragon.flightHistory.get(7).yRot();
	}

	public static float pitch(EnderDragon dragon) {
		return clampPitch((float) (dragon.flightHistory.get(5).y() - dragon.flightHistory.get(10).y()) * 10.0F);
	}

	private static float clampPitch(float p) {
		return Math.max(-40.0F, Math.min(40.0F, p));
	}
}
