package net.amaguri.goki;

import java.util.function.ToIntFunction;

import net.minecraft.world.entity.player.Player;

/**
 * Bridge so common code (mixins in main) can ask the client-side state without referencing client classes.
 * CONTRACT FILE.
 * W2 sets CLIENT_STAGE in GokiClient: p -> ClientGokiState.stageOf(p.getUUID()).
 * W1 uses stageOf(player): server -> saved state, client (level().isClientSide()) -> CLIENT_STAGE.
 */
public final class GokiHooks {
	private GokiHooks() {
	}

	/** Client-side lookup: target's stage, or 0 for non-targets. Set by the client entrypoint. */
	public static volatile ToIntFunction<Player> CLIENT_STAGE = p -> 0;
	/** Server-side lookup: set by GokiServer.init (W1). */
	public static volatile ToIntFunction<Player> SERVER_STAGE = p -> 0;

	public static int stageOf(Player p) {
		return p.level().isClientSide() ? CLIENT_STAGE.applyAsInt(p) : SERVER_STAGE.applyAsInt(p);
	}
}
