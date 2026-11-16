package net.amaguri.goki.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Cartoon cockroach (stage 7+): flat oval body, bulging eyes, 6 short thick legs, long swaying antennae.
 * Geometry/UV of createLayer() is GENERATED from agent/tasks/w2/tools/spec.py (keep in sync with textures/entity/cockroach.png).
 * Model space: y down, front = -z, ground at y = 24.
 */
public final class CockroachModel extends EntityModel<AvatarRenderState> {
	private static CockroachModel instance;

	/** Baked lazily on first use (own LayerDefinition, no model-layer registration needed). */
	public static CockroachModel get() {
		if (instance == null) {
			instance = new CockroachModel(createLayer().bakeRoot());
		}
		return instance;
	}

	private final ModelPart[] legsL = new ModelPart[3];
	private final ModelPart[] legsR = new ModelPart[3];
	private final ModelPart[][] antennae = new ModelPart[2][3]; // [0] = left (+x), [1] = right

	private CockroachModel(ModelPart root) {
		super(root);
		for (int i = 0; i < 3; i++) {
			this.legsL[i] = root.getChild("leg_l" + i);
			this.legsR[i] = root.getChild("leg_r" + i);
		}
		for (int s = 0; s < 2; s++) {
			String n = s == 0 ? "ant_l" : "ant_r";
			ModelPart a1 = root.getChild(n + "1");
			ModelPart a2 = a1.getChild(n + "2");
			ModelPart a3 = a2.getChild(n + "3");
			this.antennae[s][0] = a1;
			this.antennae[s][1] = a2;
			this.antennae[s][2] = a3;
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 28).addBox(-8.0F, 13.0F, -5.0F, 16.0F, 6.0F, 5.0F).texOffs(0, 0).addBox(-9.0F, 13.0F, 0.0F, 18.0F, 6.0F, 8.0F).texOffs(42, 28).addBox(-8.0F, 13.0F, 8.0F, 16.0F, 6.0F, 4.0F).texOffs(0, 39).addBox(-6.0F, 14.0F, 12.0F, 12.0F, 5.0F, 3.0F).texOffs(90, 39).addBox(-3.0F, 15.0F, 15.0F, 6.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
		PartDefinition p_pronotum = root.addOrReplaceChild("pronotum", CubeListBuilder.create().texOffs(80, 14).addBox(-8.0F, 12.0F, -11.0F, 16.0F, 7.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
		PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(52, 0).addBox(-7.0F, 13.0F, -18.0F, 14.0F, 7.0F, 7.0F).texOffs(78, 39).addBox(-6.0F, 14.0F, -19.0F, 5.0F, 6.0F, 1.0F).texOffs(78, 39).addBox(1.0F, 14.0F, -19.0F, 5.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
		PartDefinition p_wing_l = root.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 14).addBox(0.0F, -2.0F, 0.0F, 8.0F, 2.0F, 12.0F).texOffs(34, 39).addBox(0.0F, -2.0F, 12.0F, 6.0F, 2.0F, 5.0F).texOffs(0, 47).addBox(0.0F, -2.0F, 17.0F, 3.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 13.0F, -5.0F, 0.0F, 0.0F, 0.07F));
		PartDefinition p_wing_r = root.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(40, 14).addBox(-8.0F, -2.0F, 0.0F, 8.0F, 2.0F, 12.0F).texOffs(56, 39).addBox(-6.0F, -2.0F, 12.0F, 6.0F, 2.0F, 5.0F).texOffs(12, 47).addBox(-3.0F, -2.0F, 17.0F, 3.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 13.0F, -5.0F, 0.0F, 0.0F, -0.07F));
		PartDefinition p_leg_l0 = root.addOrReplaceChild("leg_l0", CubeListBuilder.create().texOffs(106, 39).addBox(0.0F, -1.5F, -1.5F, 4.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(8.0F, 18.5F, -6.0F, 0.0F, 0.55F, -0.18F));
		PartDefinition p_shin_l0 = p_leg_l0.addOrReplaceChild("shin_l0", CubeListBuilder.create().texOffs(82, 28).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F));
		PartDefinition p_leg_r0 = root.addOrReplaceChild("leg_r0", CubeListBuilder.create().texOffs(106, 39).addBox(-4.0F, -1.5F, -1.5F, 4.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-8.0F, 18.5F, -6.0F, 0.0F, -0.55F, 0.18F));
		PartDefinition p_shin_r0 = p_leg_r0.addOrReplaceChild("shin_r0", CubeListBuilder.create().texOffs(82, 28).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F));
		PartDefinition p_leg_l1 = root.addOrReplaceChild("leg_l1", CubeListBuilder.create().texOffs(106, 39).addBox(0.0F, -1.5F, -1.5F, 4.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(8.0F, 18.5F, 2.0F, 0.0F, 0.05F, -0.18F));
		PartDefinition p_shin_l1 = p_leg_l1.addOrReplaceChild("shin_l1", CubeListBuilder.create().texOffs(82, 28).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F));
		PartDefinition p_leg_r1 = root.addOrReplaceChild("leg_r1", CubeListBuilder.create().texOffs(106, 39).addBox(-4.0F, -1.5F, -1.5F, 4.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-8.0F, 18.5F, 2.0F, 0.0F, -0.05F, 0.18F));
		PartDefinition p_shin_r1 = p_leg_r1.addOrReplaceChild("shin_r1", CubeListBuilder.create().texOffs(82, 28).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F));
		PartDefinition p_leg_l2 = root.addOrReplaceChild("leg_l2", CubeListBuilder.create().texOffs(106, 39).addBox(0.0F, -1.5F, -1.5F, 4.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(8.0F, 18.5F, 9.0F, 0.0F, -0.55F, -0.18F));
		PartDefinition p_shin_l2 = p_leg_l2.addOrReplaceChild("shin_l2", CubeListBuilder.create().texOffs(82, 28).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3F));
		PartDefinition p_leg_r2 = root.addOrReplaceChild("leg_r2", CubeListBuilder.create().texOffs(106, 39).addBox(-4.0F, -1.5F, -1.5F, 4.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-8.0F, 18.5F, 9.0F, 0.0F, 0.55F, 0.18F));
		PartDefinition p_shin_r2 = p_leg_r2.addOrReplaceChild("shin_r2", CubeListBuilder.create().texOffs(82, 28).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3F));
		PartDefinition p_ant_l1 = root.addOrReplaceChild("ant_l1", CubeListBuilder.create().texOffs(94, 28).addBox(-0.5F, -9.0F, -0.5F, 1.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(3.5F, 13.2F, -15.0F, 0.75F, 0.0F, 0.5F));
		PartDefinition p_ant_l2 = p_ant_l1.addOrReplaceChild("ant_l2", CubeListBuilder.create().texOffs(98, 28).addBox(-0.5F, -9.0F, -0.5F, 1.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, 0.3F, 0.0F, 0.0F));
		PartDefinition p_ant_l3 = p_ant_l2.addOrReplaceChild("ant_l3", CubeListBuilder.create().texOffs(30, 39).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F).texOffs(24, 47).addBox(-1.0F, -7.5F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, 0.3F, 0.0F, 0.0F));
		PartDefinition p_ant_r1 = root.addOrReplaceChild("ant_r1", CubeListBuilder.create().texOffs(94, 28).addBox(-0.5F, -9.0F, -0.5F, 1.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(-3.5F, 13.2F, -15.0F, 0.75F, 0.0F, -0.5F));
		PartDefinition p_ant_r2 = p_ant_r1.addOrReplaceChild("ant_r2", CubeListBuilder.create().texOffs(98, 28).addBox(-0.5F, -9.0F, -0.5F, 1.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, 0.3F, 0.0F, 0.0F));
		PartDefinition p_ant_r3 = p_ant_r2.addOrReplaceChild("ant_r3", CubeListBuilder.create().texOffs(30, 39).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F).texOffs(24, 47).addBox(-1.0F, -7.5F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, 0.3F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		super.setupAnim(state);
		this.resetPose();
		float t = state.ageInTicks;
		float amp = Math.min(1.0F, state.walkAnimationSpeed * 2.5F);
		float ph = state.walkAnimationPos * 1.3F;
		// tripod gait: legs L0, R1, L2 move together, opposite to R0, L1, R2
		for (int i = 0; i < 3; i++) {
			animLeg(this.legsL[i], 1.0F, (i % 2 == 0) ? 0.0F : (float) Math.PI, ph, amp);
			animLeg(this.legsR[i], -1.0F, (i % 2 == 1) ? 0.0F : (float) Math.PI, ph, amp);
		}
		// antennae: idle sway + stronger while walking
		for (int s = 0; s < 2; s++) {
			float side = s == 0 ? 1.0F : -1.0F;
			float pp = s == 0 ? 0.0F : 1.7F;
			for (int lvl = 1; lvl <= 3; lvl++) {
				ModelPart a = this.antennae[s][lvl - 1];
				a.xRot += (float) Math.sin(t * 0.13F + pp - lvl * 0.7F) * (0.06F + 0.05F * lvl)
						+ amp * 0.05F * (float) Math.sin(state.walkAnimationPos * 1.3F + lvl);
				if (lvl == 1) {
					a.zRot += side * (float) Math.sin(t * 0.09F + pp) * 0.10F;
					a.yRot += (float) Math.sin(t * 0.11F + pp) * 0.08F;
				}
			}
		}
	}

	private static void animLeg(ModelPart leg, float side, float phase, float ph, float amp) {
		float a = ph + phase;
		leg.yRot += side * (float) Math.sin(a) * 0.5F * amp;
		float lift = Math.max(0.0F, (float) Math.cos(a)) * 0.45F * amp;
		leg.zRot += side > 0 ? -lift : lift;
	}
}
