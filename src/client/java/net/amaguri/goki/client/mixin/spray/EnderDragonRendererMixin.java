package net.amaguri.goki.client.mixin.spray;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import net.amaguri.goki.client.spray.SprayCanRenderer;
import net.amaguri.goki.client.spray.SprayState;

/**
 * EnderDragonRenderer declares submit(EnderDragonRenderState, ...) and the private final field "model".
 * While a target is set: submit the spray can first, then hide the vanilla dragon model (root.visible = false) so
 * vanilla still draws everything else itself (death rays, crystal beams, ...). Without a target the model is visible again.
 */
@Mixin(EnderDragonRenderer.class)
public abstract class EnderDragonRendererMixin {
	@Shadow
	@Final
	private EnderDragonModel model;

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"))
	private void goki$submitSprayCan(EnderDragonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		boolean can = SprayState.active() && SprayCanRenderer.submit(state, poseStack, collector);
		this.model.root().visible = !can;
	}
}
