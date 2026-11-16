package net.amaguri.goki.server;

import java.util.function.IntPredicate;

import net.amaguri.goki.GokiStage;

/** Pure decision logic (no Minecraft types). Everything here is unit-testable. */
public final class StageLogic {
	private StageLogic() {
	}

	/** How far (blocks, horizontal) the target walks between two possible rustle sounds. */
	public static final double RUSTLE_STEP_DISTANCE = 1.6;
	/** Chance that a rustle actually plays once the step distance is reached. */
	public static final float RUSTLE_CHANCE = 0.35f;

	public static int clamp(int stage) {
		return Math.max(GokiStage.START, Math.min(GokiStage.MAX, stage));
	}

	/**
	 * Next stage from advancement progress. Never goes down. The highest n (1..MAX) whose advancement somebody
	 * has wins, so skipped stages are jumped over.
	 *
	 * @param achieved asks "has anybody completed ADVANCEMENTS[n]?" (called top-down, lazily)
	 */
	public static int nextStage(int current, IntPredicate achieved) {
		int s = clamp(current);
		if (s >= GokiStage.MAX) {
			return s;
		}
		for (int n = GokiStage.MAX; n > s; n--) {
			if (achieved.test(n)) {
				return n;
			}
		}
		return s;
	}

	/**
	 * Late-fight focus: the dragon aims its fireballs / breath at the target while its health ratio is at or below
	 * SPRAY_FOCUS_HEALTH_RATIO and the target is in the End.
	 */
	public static boolean sprayFocus(boolean targetInEnd, boolean dragonAlive, float health, float maxHealth) {
		if (!targetInEnd || !dragonAlive || maxHealth <= 0.0f || health <= 0.0f) {
			return false;
		}
		return health / maxHealth <= GokiStage.SPRAY_FOCUS_HEALTH_RATIO;
	}

	/** Same test vanilla uses for a cloud: horizontal distance from the cloud centre within its radius. */
	public static boolean insideCloud(double dx, double dz, float radius) {
		return dx * dx + dz * dz <= (double) radius * radius;
	}

	/** Whether the rustle sound should be considered after walking {@code accumulated} blocks. */
	public static boolean rustleDue(double accumulated) {
		return accumulated >= RUSTLE_STEP_DISTANCE;
	}

	// ---- enhancements for everybody (stage >= n) ----

	/** Movement speed, ADD_MULTIPLIED_BASE: S1 +10%, S3 +20% total. */
	public static double speedBonus(int s) {
		return s >= 3 ? 0.2 : s >= 1 ? 0.1 : 0.0;
	}

	public static double armorBonus(int s) {
		return s >= 2 ? 2.0 : 0.0;
	}

	public static double attackBonus(int s) {
		return s >= 5 ? 2.0 : 0.0;
	}

	public static double maxHealthBonus(int s) {
		return s >= 8 ? 4.0 : 0.0;
	}

	public static boolean fireResistance(int s) {
		return s >= 4;
	}

	public static boolean nightVision(int s) {
		return s >= 7;
	}

	/** Block break speed, ADD_MULTIPLIED_BASE: S6 +20%. */
	public static double breakSpeedBonus(int s) {
		return s >= 6 ? 0.2 : 0.0;
	}
}
