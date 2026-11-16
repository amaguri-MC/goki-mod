package net.amaguri.goki.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

import net.amaguri.goki.GokiHooks;
import net.amaguri.goki.GokiStage;

/**
 * Target (GokiStage.noHunger) never gets hungry: the hidden exhaustion counter is cleared before every food tick, so
 * neither food level nor saturation is ever drained. Regeneration and eating work as usual.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {
	@Shadow
	private float exhaustionLevel;

	@Inject(method = "tick", at = @At("HEAD"))
	private void goki$noHunger(ServerPlayer player, CallbackInfo ci) {
		if (GokiStage.noHunger(GokiHooks.stageOf(player))) {
			this.exhaustionLevel = 0.0f;
		}
	}
}
