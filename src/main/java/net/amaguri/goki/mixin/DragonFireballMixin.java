package net.amaguri.goki.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import net.amaguri.goki.server.SprayClouds;

/**
 * While a target is set, a dragon fireball that lands makes an insecticide spray cloud instead of the vanilla
 * harming cloud (and skips the vanilla dragon-breath burst). The fireball itself never damaged anybody.
 * With no target the injection does nothing and vanilla runs.
 */
@Mixin(DragonFireball.class)
public abstract class DragonFireballMixin {
	@Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
	private void goki$spray(HitResult hit, CallbackInfo ci) {
		DragonFireball self = (DragonFireball) (Object) this;
		if (!(self.level() instanceof ServerLevel level) || !SprayClouds.active()) {
			return;
		}
		if (hit.getType() == HitResult.Type.ENTITY && self.getOwner() == ((EntityHitResult) hit).getEntity()) {
			return; // vanilla ignores the shooter itself
		}
		LivingEntity owner = self.getOwner() instanceof LivingEntity living ? living : null;
		SprayClouds.spawn(level, self.getX(), self.getY(), self.getZ(), owner, 300);
		self.discard();
		ci.cancel();
	}
}
