package net.amaguri.goki.server;

/**
 * Texts shown on level-ups (pure strings). Nothing here may hint at what the target is turning into:
 * only positive, game-like wording.
 */
public final class StageText {
	private StageText() {
	}

	public static final String TITLE = "LEVEL UP!";

	/** Subtitle shown to everybody, or null if the stage has no level-up. */
	public static String publicSubtitle(int stage) {
		return switch (stage) {
			case 1 -> "足が速くなった！";
			case 2 -> "防御力が上がった！";
			case 3 -> "さらに足が速くなった！";
			case 4 -> "炎に強くなった！";
			case 5 -> "攻撃力が上がった！";
			case 6 -> "暗闇でも見えるようになった！";
			case 7 -> "体力の上限が増えた！";
			default -> null;
		};
	}

	/** Extra chat line only for the target, or null if the stage has none. */
	public static String targetBonus(int stage) {
		return switch (stage) {
			case 2 -> "お腹が空かない体になった！";
			case 4 -> "壁を登れるようになった！";
			case 5 -> "感覚が鋭くなった！";
			case 6 -> "身のこなしが軽くなった！";
			case 7 -> "さらに強くなった気がする…！";
			default -> null;
		};
	}
}
