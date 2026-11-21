package zako.opt.cosmetic;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;



public final class ZakoCosmeticModel {
     private ZakoCosmeticModel() {}


    public static PlayerModel create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
            .addBox(-5, -9.5f, -4, 10, 9.5f, 8)
            .texOffs(64, 0).addBox(-5.05f, -10, -3.8f, 10.1f, 1.2f, 7.8f)
            .texOffs(64, 96).addBox(-5.55f, -6.7f, -1.5f, .6f, 2.5f, 2)
            .addBox(4.95f, -6.7f, -1.5f, .6f, 2.5f, 2), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 32)
            .addBox(-6.3f, 0, -4.3f, 12.6f, 12, 8.6f)
            .addBox(-5.8f, 3, -5.1f, 11.6f, 8, .9f), PartPose.ZERO);
        body.addOrReplaceChild("jacket", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(64, 32)
            .addBox(-1, -2, -2.3f, 4.6f, 5.5f, 4.6f)
            .texOffs(64, 96).addBox(-.7f, 3.5f, -2, 4, 4.5f, 4), PartPose.offset(6.3f, 2, 0));
        leftArm.addOrReplaceChild("left_sleeve", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(64, 32)
            .addBox(-3.6f, -2, -2.3f, 4.6f, 5.5f, 4.6f)
            .texOffs(64, 96).addBox(-3.3f, 3.5f, -2, 4, 4.5f, 4), PartPose.offset(-6.3f, 2, 0));
        rightArm.addOrReplaceChild("right_sleeve", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 64)
            .addBox(-2, 0, -2.6f, 5.2f, 9, 5.2f)
            .texOffs(64, 64).addBox(-2.1f, 9, -3.2f, 5.4f, 3, 6), PartPose.offset(2.8f, 12, 0));
        leftLeg.addOrReplaceChild("left_pants", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 64)
            .addBox(-3.2f, 0, -2.6f, 5.2f, 9, 5.2f)
            .texOffs(64, 64).addBox(-3.3f, 9, -3.2f, 5.4f, 3, 6), PartPose.offset(-2.8f, 12, 0));
        rightLeg.addOrReplaceChild("right_pants", CubeListBuilder.create(), PartPose.ZERO);
        return new PlayerModel(LayerDefinition.create(mesh, 128, 128).bakeRoot(), false);
    }
}
