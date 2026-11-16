package net.amaguri.goki;

/**
 * Stage numbers and what each stage turns on. Shared by server and client. CONTRACT FILE.
 * Stages only go up during play (commands may set any value 0..MAX).
 *
 * 0 start
 * 1 minecraft:story/mine_stone        all: speed +10%            target: -
 * 2 minecraft:story/smelt_iron        all: armor +2              target: hunger never drops
 * 3 minecraft:story/mine_diamond      all: speed +20% total      target: hands/forearms slightly brown (overlay 1)
 * 4 minecraft:story/enter_the_nether  all: fire resistance       target: arms brown (overlay 2), can climb walls
 * 5 minecraft:nether/obtain_blaze_rod all: attack +2             target: gets a normal-looking elytra equipped ("you can glide now!")
 * 6 item:minecraft:ender_eye          all: block break speed +20% target: antennae (HUD tips in 1st person, on head for others)
 *   ("item:<id>" = somebody has that item in their inventory; there is no vanilla advancement for it)
 * 7 minecraft:story/follow_ender_eye  all: night vision          target: whole body brown + wings (overlay 3), rustling steps, elytra slightly brown
 * 8 minecraft:story/enter_the_end     all: max health +4         target: full cockroach bodysuit (overlay 4, own face visible), elytra = cockroach wings
 *
 * Ending (no stage of its own): while a target is set, the Ender Dragon looks like an insecticide spray can
 * (to everyone). Its breath / fireballs become insecticide spray: harmless to everyone except the target,
 * who dies instantly on contact ("sprayed"). When the dragon's HP ratio <= SPRAY_FOCUS_HEALTH_RATIO it aims
 * its fireball / breath at the target so the gag happens near the end of the fight.
 */
public final class GokiStage {
	private GokiStage() {
	}

	public static final int START = 0;
	public static final int MAX_ADVANCEMENT_STAGE = 8;
	public static final int MAX = 8;

	public static final String[] ADVANCEMENTS = {
			null,
			"minecraft:story/mine_stone",
			"minecraft:story/smelt_iron",
			"minecraft:story/mine_diamond",
			"minecraft:story/enter_the_nether",
			"minecraft:nether/obtain_blaze_rod",
			"item:minecraft:ender_eye",
			"minecraft:story/follow_ender_eye",
			"minecraft:story/enter_the_end",
	};

	/** Dragon health ratio (health/maxHealth) at or below which the dragon focuses its spray on the target. */
	public static final float SPRAY_FOCUS_HEALTH_RATIO = 0.25f;

	// ---- target-only looks/abilities (true when stage >= n) ----
	public static boolean noHunger(int s) { return s >= 2; }
	/** 0 = none, 1 = light (stage 3), 2 = arms (stage 4-6), 3 = whole body, face kept (stage 7), 4 = bodysuit, opaque except the face (stage 8). */
	public static int overlayLevel(int s) { return s >= 8 ? 4 : s >= 7 ? 3 : s >= 4 ? 2 : s >= 3 ? 1 : 0; }
	public static boolean wallClimb(int s) { return s >= 4; }
	public static boolean elytra(int s) { return s >= 5; }
	public static boolean antennae(int s) { return s >= 6; }
	public static boolean rustle(int s) { return s >= 7; }
	/** Elytra look for the target: 0 = vanilla texture (stage 5-6), 1 = slightly brown (7), 2 = cockroach wings (8). */
	public static int elytraLevel(int s) { return s >= 8 ? 2 : s >= 7 ? 1 : 0; }
}
