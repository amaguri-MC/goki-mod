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
 * Two thin curled antennae with little bulbs, attached to the player's head (stage 5-6, 3rd person).
 * Geometry/UV of createLayer() is GENERATED from agent/tasks/w2/tools/spec.py (keep in sync with textures/entity/antenna_head.png).
 * Parts are in head space (origin = head pivot, y down).
 */
public final class AntennaModel extends EntityModel<AvatarRenderState> {
	private static AntennaModel instance;

	public static AntennaModel get() {
		if (instance == null) {
			instance = new AntennaModel(createLayer().bakeRoot());
		}
		return instance;
	}

	private final ModelPart[][] antennae = new ModelPart[2][3];

	private AntennaModel(ModelPart root) {
		super(root);
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
		PartDefinition p_ant_l1 = root.addOrReplaceChild("ant_l1", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(2.0F, -8.0F, -3.0F, 0.4F, 0.0F, 0.3F));
		PartDefinition p_ant_l2 = p_ant_l1.addOrReplaceChild("ant_l2", CubeListBuilder.create().texOffs(4, 0).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.2F, 0.0F, 0.0F));
		PartDefinition p_ant_l3 = p_ant_l2.addOrReplaceChild("ant_l3", CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F).texOffs(12, 0).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.25F, 0.0F, 0.0F));
		PartDefinition p_ant_r1 = root.addOrReplaceChild("ant_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, -8.0F, -3.0F, 0.4F, 0.0F, -0.3F));
		PartDefinition p_ant_r2 = p_ant_r1.addOrReplaceChild("ant_r2", CubeListBuilder.create().texOffs(4, 0).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.2F, 0.0F, 0.0F));
		PartDefinition p_ant_r3 = p_ant_r2.addOrReplaceChild("ant_r3", CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F).texOffs(12, 0).addBox(-1.0F, -4.5F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.25F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 32, 16);
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		super.setupAnim(state);
		this.resetPose();
		float t = state.ageInTicks;
		float amp = Math.min(1.0F, state.walkAnimationSpeed * 2.5F);
		for (int s = 0; s < 2; s++) {
			float side = s == 0 ? 1.0F : -1.0F;
			float pp = s == 0 ? 0.0F : 1.7F;
			for (int lvl = 1; lvl <= 3; lvl++) {
				ModelPart a = this.antennae[s][lvl - 1];
				a.xRot += (float) Math.sin(t * 0.13F + pp - lvl * 0.7F) * (0.05F + 0.04F * lvl)
						+ amp * 0.06F * (float) Math.sin(state.walkAnimationPos * 1.3F + lvl);
				if (lvl == 1) {
					a.zRot += side * (float) Math.sin(t * 0.09F + pp) * 0.08F;
					a.yRot += (float) Math.sin(t * 0.11F + pp) * 0.06F;
				}
			}
		}
	}
}
