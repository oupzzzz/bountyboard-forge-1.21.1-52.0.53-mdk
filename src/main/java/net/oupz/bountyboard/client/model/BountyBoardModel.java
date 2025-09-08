package net.oupz.bountyboard.client.model;

import net.oupz.bountyboard.BountyBoard;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class BountyBoardModel<T extends Entity> extends EntityModel<T> {
    // This layer location should be baked with EntityRendererProvider.Context in the model renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "bountyboard"), "main");
    private final ModelPart BOUNTY_BOARD;
    private final ModelPart BOARDS;
    private final ModelPart BOARD1;
    private final ModelPart BOARD2;
    private final ModelPart BOARD3;
    private final ModelPart BOARD4;
    private final ModelPart PAPER;
    private final ModelPart CONTS;
    private final ModelPart PCONS;
    private final ModelPart PCON1;
    private final ModelPart PCON2;
    private final ModelPart PCON3;
    private final ModelPart POSTS;
    private final ModelPart POSTR;
    private final ModelPart POSTL;
    private final ModelPart BEAMS;
    private final ModelPart BEAMT;
    private final ModelPart BEAMB;

    public BountyBoardModel(ModelPart root) {
        this.BOUNTY_BOARD = root.getChild("BOUNTY_BOARD");
        this.BOARDS = this.BOUNTY_BOARD.getChild("BOARDS");
        this.BOARD1 = this.BOARDS.getChild("BOARD1");
        this.BOARD2 = this.BOARDS.getChild("BOARD2");
        this.BOARD3 = this.BOARDS.getChild("BOARD3");
        this.BOARD4 = this.BOARDS.getChild("BOARD4");
        this.PAPER = this.BOARDS.getChild("PAPER");
        this.CONTS = this.BOUNTY_BOARD.getChild("CONTS");
        this.PCONS = this.BOUNTY_BOARD.getChild("PCONS");
        this.PCON1 = this.PCONS.getChild("PCON1");
        this.PCON2 = this.PCONS.getChild("PCON2");
        this.PCON3 = this.PCONS.getChild("PCON3");
        this.POSTS = this.BOUNTY_BOARD.getChild("POSTS");
        this.POSTR = this.POSTS.getChild("POSTR");
        this.POSTL = this.POSTS.getChild("POSTL");
        this.BEAMS = this.BOUNTY_BOARD.getChild("BEAMS");
        this.BEAMT = this.BEAMS.getChild("BEAMT");
        this.BEAMB = this.BEAMS.getChild("BEAMB");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition BOUNTY_BOARD = partdefinition.addOrReplaceChild("BOUNTY_BOARD", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition BOARDS = BOUNTY_BOARD.addOrReplaceChild("BOARDS", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition BOARD1 = BOARDS.addOrReplaceChild("BOARD1", CubeListBuilder.create().texOffs(48, 59).addBox(9.0546F, -2.188F, -1.0F, 10.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 34).addBox(-6.9454F, -2.188F, -1.0F, 16.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(62, 52).addBox(-16.9454F, -2.188F, -1.0F, 10.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -33.5507F, 0.0F, 0.0F, 0.0F, 0.0175F));

        PartDefinition BOARD2 = BOARDS.addOrReplaceChild("BOARD2", CubeListBuilder.create().texOffs(62, 46).addBox(9.4058F, -2.1304F, -1.0F, 10.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 16).addBox(-6.5942F, -2.1304F, -1.0F, 16.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(40, 0).addBox(-18.5942F, -2.1304F, -1.0F, 12.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.1884F, -29.3478F, 0.0F, 0.0F, 0.0F, 0.0175F));

        PartDefinition BOARD3 = BOARDS.addOrReplaceChild("BOARD3", CubeListBuilder.create().texOffs(62, 40).addBox(9.4058F, -2.3043F, -0.9999F, 10.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 22).addBox(-6.5942F, -2.3043F, -0.9999F, 16.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(52, 65).addBox(-15.5942F, -2.3043F, -0.9999F, 9.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.8696F, -24.942F, 0.0F, 0.0003F, 0.0175F, 0.0F));

        PartDefinition BOARD4 = BOARDS.addOrReplaceChild("BOARD4", CubeListBuilder.create().texOffs(40, 6).addBox(8.935F, -2.5941F, -0.9996F, 12.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 28).addBox(-7.065F, -2.5941F, -0.9996F, 16.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(30, 65).addBox(-16.065F, -2.5941F, -0.9996F, 9.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.2174F, -20.6957F, 0.0F, 0.0006F, -0.0175F, 0.0174F));

        PartDefinition PAPER = BOARDS.addOrReplaceChild("PAPER", CubeListBuilder.create().texOffs(62, 71).addBox(-0.2029F, -25.9565F, 1.2899F, 4.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition cube_r1 = PAPER.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(54, 71).addBox(-1.0F, 0.0F, 1.0F, 4.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.6232F, -34.1304F, 0.2899F, 0.0F, 0.0F, 0.0873F));

        PartDefinition cube_r2 = PAPER.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(68, 15).addBox(-3.0F, 0.0F, 1.0F, 6.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.3623F, -31.2899F, 0.2899F, 0.0F, 0.0F, 0.0873F));

        PartDefinition cube_r3 = PAPER.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(68, 7).addBox(-3.0F, 0.0F, 1.0F, 6.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.6377F, -34.2899F, 0.2899F, 0.0F, 0.0F, -0.0873F));

        PartDefinition CONTS = BOUNTY_BOARD.addOrReplaceChild("CONTS", CubeListBuilder.create().texOffs(16, 67).addBox(-1.5F, -32.0F, -2.0F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(20, 67).addBox(25.5F, -32.0F, -2.0F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(30, 60).addBox(25.5F, -16.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(34, 60).addBox(-1.5F, -16.0F, -2.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-13.0F, -4.0F, 2.8261F));

        PartDefinition PCONS = BOUNTY_BOARD.addOrReplaceChild("PCONS", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition PCON1 = PCONS.addOrReplaceChild("PCON1", CubeListBuilder.create(), PartPose.offset(-1.0F, -34.0F, -2.0F));

        PartDefinition cube_r4 = PCON1.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(30, 71).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 13.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(68, 23).addBox(-1.0F, -14.0F, -1.0F, 2.0F, 13.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.6144F));

        PartDefinition PCON2 = PCONS.addOrReplaceChild("PCON2", CubeListBuilder.create(), PartPose.offset(0.0F, -28.0F, -2.0F));

        PartDefinition cube_r5 = PCON2.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(42, 71).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 13.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(36, 71).addBox(-1.0F, -13.0F, -1.0F, 2.0F, 13.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5533F));

        PartDefinition PCON3 = PCONS.addOrReplaceChild("PCON3", CubeListBuilder.create(), PartPose.offset(0.0F, -21.3333F, -2.0F));

        PartDefinition cube_r6 = PCON3.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(24, 67).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 13.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(48, 71).addBox(-1.0F, -13.0F, -1.0F, 2.0F, 13.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.6057F));

        PartDefinition POSTS = BOUNTY_BOARD.addOrReplaceChild("POSTS", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition POSTR = POSTS.addOrReplaceChild("POSTR", CubeListBuilder.create().texOffs(40, 12).addBox(-2.0F, -38.0F, -2.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 68).addBox(-2.0F, -36.0F, -2.0F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(16, 48).addBox(-2.0F, -32.0F, -2.0F, 4.0F, 16.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(32, 40).addBox(-2.0F, -16.0F, -2.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-14.0F, 0.0F, -2.0F));

        PartDefinition POSTL = POSTS.addOrReplaceChild("POSTL", CubeListBuilder.create().texOffs(52, 12).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(68, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(48, 40).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 16.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 48).addBox(-2.0F, 20.0F, -2.0F, 4.0F, 16.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(14.0F, -36.0F, -2.0F));

        PartDefinition BEAMS = BOUNTY_BOARD.addOrReplaceChild("BEAMS", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition BEAMT = BEAMS.addOrReplaceChild("BEAMT", CubeListBuilder.create().texOffs(36, 16).addBox(-36.0F, -6.0F, -2.0F, 12.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-24.0F, -6.0F, -2.0F, 16.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(36, 24).addBox(-8.0F, -6.0F, -2.0F, 12.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(16.0F, -34.0F, 0.0F));

        PartDefinition BEAMB = BEAMS.addOrReplaceChild("BEAMB", CubeListBuilder.create().texOffs(36, 32).addBox(-36.0F, -6.0F, -2.0F, 12.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 8).addBox(-24.0F, -6.0F, -2.0F, 16.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 40).addBox(-8.0F, -6.0F, -2.0F, 12.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(16.0F, -12.9275F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        BOUNTY_BOARD.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}