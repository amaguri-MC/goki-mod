package net.amaguri.goki.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import net.amaguri.goki.client.render.CockroachRenderer;
import net.amaguri.goki.client.render.GokiRender;

/**
 * LivingEntityRenderer declares submit(S,...) and getRenderType(S,boolean,boolean,boolean) (AvatarRenderer only inherits them),
 * so the cockroach swap has to hook here. Everything is gated on "AvatarRenderState of the target at stage 7+".
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	/** Draw the cockroach first; vanilla then continues (its player body is suppressed below) and still submits the name tag. */
	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"))
	private void goki$submitCockroach(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		if (state instanceof AvatarRenderState avatar && GokiRender.isCockroach(avatar)) {
			CockroachRenderer.submit(avatar, poseStack, collector);
		}
	}

	/** No render type = vanilla skips the player body (the same path vanilla uses for invisible entities). */
	@Inject(method = "getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), cancellable = true)
	private void goki$hideBodyOfCockroach(LivingEntityRenderState state, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
		if (state instanceof AvatarRenderState avatar && GokiRender.isCockroach(avatar)) {
			cir.setReturnValue(null);
		}
	}
}
