package net.amaguri.goki.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

import net.amaguri.goki.GokiAssets;
import net.amaguri.goki.GokiStage;
import net.amaguri.goki.client.render.GokiRender;

/**
 * WingsLayer declares submit(PoseStack, SubmitNodeCollector, int, S, float, float) and the private ElytraModel field
 * (verified in apidump). For the target's elytra we draw the same model ourselves with a tint / the cockroach-wing texture;
 * everybody else (and level 0) takes the untouched vanilla path.
 */
@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin {
	@Unique
	private static final Identifier GOKI_VANILLA_ELYTRA = Identifier.withDefaultNamespace("textures/entity/equipment/wings/elytra.png");

	@Shadow
	@Final
	private ElytraModel elytraModel;

	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("HEAD"), cancellable = true)
	private void goki$targetElytra(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidRenderState state, float yRot, float xRot, CallbackInfo ci) {
		int level = GokiStage.elytraLevel(GokiRender.stage(state));
		if (level <= 0 || state.chestEquipment.getItem() != Items.ELYTRA) {
			return;
		}
		Identifier texture = GOKI_VANILLA_ELYTRA;
		int tint = 0xFFFFFFFF;
		if (level == 1) {
			tint = GokiAssets.ELYTRA_TINT;
		} else if (GokiRender.textureExists(GokiAssets.ELYTRA_COCKROACH)) {
			texture = GokiAssets.ELYTRA_COCKROACH;
		}
		this.elytraModel.setupAnim(state);
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.0F, 0.125F); // same offset as vanilla
		collector.submitModel(this.elytraModel, state, poseStack, RenderTypes.armorCutoutNoCull(texture), light,
				LivingEntityRenderer.getOverlayCoords(state, 0.0F), tint, null, state.outlineColor, null);
		poseStack.popPose();
		ci.cancel();
	}
}
