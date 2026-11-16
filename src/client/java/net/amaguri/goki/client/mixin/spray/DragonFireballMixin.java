package net.amaguri.goki.client.mixin.spray;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;

import net.amaguri.goki.GokiParticles;
import net.amaguri.goki.client.spray.SprayState;

/** DragonFireball declares getTrailParticle(). The trail is only ever spawned as a client particle; swap purple smoke for the spray mist. */
@Mixin(DragonFireball.class)
public abstract class DragonFireballMixin {
	@Inject(method = "getTrailParticle()Lnet/minecraft/core/particles/ParticleOptions;", at = @At("HEAD"), cancellable = true)
	private void goki$sprayTrail(CallbackInfoReturnable<ParticleOptions> cir) {
		if (SprayState.active()) {
			cir.setReturnValue(GokiParticles.SPRAY);
		}
	}
}
