package net.amaguri.goki.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;

import net.amaguri.goki.server.SprayClouds;

/**
 * Fireball aim: the strafe phase shoots at its {@code attackTarget}. In the late fight (dragon HP low, target in
 * the End) that is swapped for the target before every tick; otherwise vanilla keeps its own choice.
 */
@Mixin(DragonStrafePlayerPhase.class)
public abstract class DragonStrafeFocusMixin {
	@Shadow
	private LivingEntity attackTarget;

	@Inject(method = "doServerTick", at = @At("HEAD"))
	private void goki$focus(ServerLevel level, CallbackInfo ci) {
		if (!SprayClouds.active()) {
			return;
		}
		EnderDragon dragon = SprayClouds.dragonOf(level, (DragonStrafePlayerPhase) (Object) this);
		if (dragon == null) {
			return;
		}
		ServerPlayer target = SprayClouds.focusTarget(level, dragon);
		if (target != null) {
			this.attackTarget = target;
		}
	}
}
