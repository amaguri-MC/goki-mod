package net.amaguri.goki.client.mixin.spray;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DragonFireballRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import net.amaguri.goki.client.spray.SprayBallRenderer;
import net.amaguri.goki.client.spray.SprayState;

/** DragonFireballRenderer declares submit(EntityRenderState, ...). While a target is set the fireball is a puff of spray mist. */
@Mixin(DragonFireballRenderer.class)
public abstract class DragonFireballRendererMixin {
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
	private void goki$submitSprayBall(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (SprayState.active() && SprayBallRenderer.submit(state, poseStack, collector, camera)) {
			ci.cancel();
		}
	}
}
