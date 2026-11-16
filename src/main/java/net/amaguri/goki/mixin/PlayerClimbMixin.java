package net.amaguri.goki.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.amaguri.goki.GokiHooks;
import net.amaguri.goki.GokiStage;

/**
 * Wall climbing for the target (stage 4+), like a spider: while pushing into a wall and holding forward the
 * player counts as "on a climbable". Vanilla LivingEntity.travel then lifts the player at 0.2/tick on
 * collision, and sneaking holds position, exactly like a ladder.
 * Player declares onClimbable itself, so this targets Player (not LivingEntity).
 * Common code: the stage comes from GokiHooks.stageOf (server: saved state, client: ClientGokiState).
 */
@Mixin(Player.class)
public abstract class PlayerClimbMixin {
	@Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
	private void goki$wallClimb(CallbackInfoReturnable<Boolean> cir) {
		Player self = (Player) (Object) this;
		if (self.isSpectator() || !self.horizontalCollision) {
			return;
		}
		if (!GokiStage.wallClimb(GokiHooks.stageOf(self))) {
			return;
		}
		if (goki$forwardPressed(self)) {
			cir.setReturnValue(true);
		}
	}

	/** Client (own player): zza is filled from the movement keys each tick. Server: the last input packet. */
	private static boolean goki$forwardPressed(Player p) {
		if (p.level().isClientSide()) {
			return p.zza > 0.0f;
		}
		return p instanceof ServerPlayer sp && sp.getLastClientInput().forward();
	}
}
