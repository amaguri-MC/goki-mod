package net.amaguri.goki.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonSittingFlamingPhase;

import net.amaguri.goki.server.SprayClouds;

/**
 * The breath the dragon spits while perched is an AreaEffectCloud. While a target is set it becomes spray; in the
 * late fight (dragon HP low, target in the End) the fresh cloud is also put on the target so the breath lands.
 */
@Mixin(DragonSittingFlamingPhase.class)
public abstract class DragonFlamingPhaseMixin {
	@Shadow
	private AreaEffectCloud flame;

	@Inject(method = "doServerTick", at = @At("TAIL"))
	private void goki$spray(ServerLevel level, CallbackInfo ci) {
		AreaEffectCloud cloud = this.flame;
		if (cloud == null || cloud.isRemoved() || !SprayClouds.active()) {
			return;
		}
		if (SprayClouds.convert(cloud)) { // true only the first time, so the move below happens once
			EnderDragon dragon = SprayClouds.dragonOf(level, (DragonSittingFlamingPhase) (Object) this);
			ServerPlayer target = dragon == null ? null : SprayClouds.focusTarget(level, dragon);
			if (target != null) {
				cloud.setPos(target.getX(), target.getY(), target.getZ());
			}
		}
	}
}
