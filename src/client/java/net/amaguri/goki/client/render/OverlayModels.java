package net.amaguri.goki.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;

/**
 * Player models inflated beyond the skin's own second layer (vanilla second layer = +0.25), used to draw the overlay so it
 * never z-fights with jacket/hat/sleeves: head +0.6, body/arms/legs +0.35 (their "second layer" parts end up +0.6).
 */
public final class OverlayModels {
	private OverlayModels() {
	}

	private static PlayerModel wide;
	private static PlayerModel slim;

	public static PlayerModel get(boolean slimArms) {
		if (slimArms) {
			if (slim == null) {
				slim = create(true);
			}
			return slim;
		}
		if (wide == null) {
			wide = create(false);
		}
		return wide;
	}

	private static PlayerModel create(boolean slimArms) {
		MeshDefinition mesh = PlayerModel.createMesh(new CubeDeformation(0.35F), slimArms);
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.6F)), PartPose.ZERO);
		head.addOrReplaceChild("hat",
				CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.85F)), PartPose.ZERO);
		ModelPart baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
		return new PlayerModel(baked, slimArms);
	}
}
