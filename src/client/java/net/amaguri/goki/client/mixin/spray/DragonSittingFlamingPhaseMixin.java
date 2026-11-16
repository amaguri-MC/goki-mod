package net.amaguri.goki.client.mixin.spray;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonSittingFlamingPhase;

import net.amaguri.goki.client.spray.SprayState;

/** DragonSittingFlamingPhase declares doClientTick() (the purple breath particles at the mouth). The can sprays from its nozzle instead (SprayBreath). */
@Mixin(DragonSittingFlamingPhase.class)
public abstract class DragonSittingFlamingPhaseMixin {
	@Inject(method = "doClientTick()V", at = @At("HEAD"), cancellable = true)
	private void goki$noPurpleBreath(CallbackInfo ci) {
		if (SprayState.active()) {
			ci.cancel();
		}
	}
}
