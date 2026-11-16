package net.amaguri.goki;

import net.minecraft.resources.Identifier;

/** Texture paths shared by client code (W2) and assets (W3). CONTRACT FILE. */
public final class GokiAssets {
	private GokiAssets() {
	}

	/**
	 * 64x64, standard player-skin UV layout (incl. second layer areas unused/transparent),
	 * mostly transparent; drawn ON TOP of the player's own skin (translucent). Index = overlayLevel 1..4 (4 is fully opaque over body, head and arms).
	 */
	public static final Identifier[] OVERLAY = {
			null,
			GokiMod.id("textures/goki/overlay_1.png"),
			GokiMod.id("textures/goki/overlay_2.png"),
			GokiMod.id("textures/goki/overlay_3.png"),
			GokiMod.id("textures/goki/overlay_4.png"),
	};
	/** First-person HUD: antenna tips peeking from the top edge. 128x64, transparent, left & right antenna in one image. */
	public static final Identifier ANTENNA_HUD = GokiMod.id("textures/goki/antenna_hud.png");
	/** Antennae on the head (3rd person, stage 5-6). Owner W2 (model + texture). */
	public static final Identifier ANTENNA_HEAD = GokiMod.id("textures/entity/antenna_head.png");
	/** Ender Dragon replacement: insecticide spray can model texture. Owner W4. */
	public static final Identifier SPRAY_CAN = GokiMod.id("textures/entity/spray_can.png");
	/** Dragon fireball replacement sprite (a puff of spray). Owner W4. */
	public static final Identifier SPRAY_BALL = GokiMod.id("textures/entity/spray_ball.png");
}
