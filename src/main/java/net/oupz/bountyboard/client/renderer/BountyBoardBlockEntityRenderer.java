package net.oupz.bountyboard.client.renderer;

import net.minecraft.world.phys.AABB;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.block.BountyBoardBlock;
import net.oupz.bountyboard.block.entity.BountyBoardBlockEntity;
import net.oupz.bountyboard.client.model.BountyBoardModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BountyBoardBlockEntityRenderer implements BlockEntityRenderer<BountyBoardBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "textures/entity/bounty_board.png");
    private final BountyBoardModel model;

    public BountyBoardBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new BountyBoardModel(context.bakeLayer(BountyBoardModel.LAYER_LOCATION));
    }

    @Override
    public void render(BountyBoardBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState blockState = blockEntity.getBlockState();

        // Only render on the origin block (0,0)
        if (blockState.getValue(BountyBoardBlock.X_POS) != 0 || blockState.getValue(BountyBoardBlock.Y_POS) != 0) {
            return;
        }

        poseStack.pushPose();

        // Translate to center of the block at ground level
        poseStack.translate(0.5D, 0.0D, 0.5D);

        // Rotate 180 degrees to flip the model right-side up
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));

        // The model's origin is at Y=24 in the model file (from PartPose.offset(0.0F, 24.0F, 0.0F))
        // We need to offset it properly so it sits on the ground
        poseStack.translate(0.0D, -1.5D, 0.0D);

        // Rotate based on facing direction
        Direction facing = blockState.getValue(BountyBoardBlock.FACING);
        float rotation = switch (facing) {
            case NORTH -> 0.0F;    // When facing north, model faces north
            case SOUTH -> 180.0F;  // When facing south, rotate 180
            case WEST -> 270.0F;   // When facing west, rotate 270
            case EAST -> 90.0F;    // When facing east, rotate 90
            default -> 0.0F;
        };

        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // The model spans from -36 to +20 units (56 units total = 3.5 blocks)
        // We have a 3x3 block structure, so we need to center the 3.5-block-wide model over 3 blocks
        // Center of model: (-36 + 20) / 2 = -8 units
        // Center of 3 blocks: 1.5 blocks = 24 units
        // Offset needed: 24 - (-8) = 32 units = 2 blocks

        switch (facing) {
            case NORTH:
                // When facing north, blocks extend east, so shift model east by 1 block
                poseStack.translate(1.0D, 0.0D, 0.0D);
                break;
            case SOUTH:
                // When facing south, blocks extend west, so shift model west by 1 block
                poseStack.translate(1.0D, 0.0D, 0.0D);
                break;
            case EAST:
                // When facing east, blocks extend south, so shift model south by 1 block
                poseStack.translate(1.0D, 0.0D, 0.0D);
                break;
            case WEST:
                // When facing west, blocks extend north, so shift model north by 1 block
                poseStack.translate(1.0D, 0.0D, 0.0D);
                break;
        }

        // Scale if needed (use negative scale if model appears inverted)
        // poseStack.scale(1.0F, -1.0F, -1.0F);

        // Get the render buffer
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        // Render the model
        model.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, 654311423);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BountyBoardBlockEntity entity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
