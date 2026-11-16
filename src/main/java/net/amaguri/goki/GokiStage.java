package net.amaguri.goki;

/**
 * Stage numbers and what each stage turns on. Shared by server and client. CONTRACT FILE.
 * Stages only go up during play (commands may set any value 0..TRAPPED).
 *
 * 0 start
 * 1 minecraft:story/mine_stone       all: speed +10%
 * 2 minecraft:story/smelt_iron       all: armor +2            target: hunger never drops
 * 3 minecraft:story/mine_diamond     all: speed +20% total    target: hands/forearms slightly brown
 * 4 minecraft:story/enter_the_nether all: fire resistance     target: arms dark brown & glossy, can climb walls
 * 5 minecraft:nether/obtain_blaze_rod all: attack +2          target: antennae (HUD tips in 1st person, on head for others)
 * 6 minecraft:story/follow_ender_eye all: night vision        target: whole body brown + wing pattern, rustling steps
 * 7 minecraft:story/enter_the_end    all: max health +4       target: others see a full cockroach instead of the player
 * 8 TRAPPED (dragon HP low in the End) target: stuck on a torimochi (glue trap) block
 */
public final class GokiStage {
	private GokiStage() {
	}

	public static final int START = 0;
	public static final int MAX_ADVANCEMENT_STAGE = 7;
	public static final int TRAPPED = 8;

	public static final String[] ADVANCEMENTS = {
			null,
			"minecraft:story/mine_stone",
			"minecraft:story/smelt_iron",
			"minecraft:story/mine_diamond",
			"minecraft:story/enter_the_nether",
			"minecraft:nether/obtain_blaze_rod",
			"minecraft:story/follow_ender_eye",
			"minecraft:story/enter_the_end",
	};

	/** Dragon health ratio (health/maxHealth) at or below which the trap fires (stage 7 -> 8). */
	public static final float TRAP_DRAGON_HEALTH_RATIO = 0.25f;

	// ---- target-only looks/abilities (true when stage >= n) ----
	public static boolean noHunger(int s) { return s >= 2; }
	/** 0 = none, 1 = light (stage 3), 2 = dark glossy (stage 4-5), 3 = whole body (stage 6+). */
	public static int overlayLevel(int s) { return s >= 6 ? 3 : s >= 4 ? 2 : s >= 3 ? 1 : 0; }
	public static boolean wallClimb(int s) { return s >= 4; }
	public static boolean antennae(int s) { return s >= 5; }
	public static boolean rustle(int s) { return s >= 6; }
	public static boolean cockroachModel(int s) { return s >= 7; }
	public static boolean trapped(int s) { return s >= TRAPPED; }
}
