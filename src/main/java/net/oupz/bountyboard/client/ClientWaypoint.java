package net.oupz.bountyboard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientWaypoint {
    private static final ResourceLocation WHITE_TEX =
            ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "textures/misc/white.png");
    private static ResourceKey<Level> DIM = null;
    private static BlockPos POS = null;

    private ClientWaypoint() {}

    public static void set(ResourceKey<Level> dim, BlockPos pos) {
        DIM = dim;
        POS = pos.immutable();
    }
    public static void clear() { DIM = null; POS = null; }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {

        if (POS == null) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (DIM == null || mc.level.dimension() != DIM) return;

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        var cam = mc.gameRenderer.getMainCamera();
        double ax = POS.getX() + 0.5;
        double ay = POS.getY();
        double az = POS.getZ() + 0.5;

        org.joml.Matrix4f mat = new org.joml.Matrix4f(event.getPoseStack());
        mat.translate(
                (float)(ax - cam.getPosition().x),
                (float)(ay - cam.getPosition().y),
                (float)(az - cam.getPosition().z)
        );

        // beam params
        final float H    = 128.0f;
        final float HALF = 0.35f;
        int r=255,g=255,b=85,a=255;
        float t = (mc.level.getGameTime() % 40) / 40.0f;

        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

        RenderType rt = RenderType.entityTranslucent(WHITE_TEX);

        quadY(mat, buf, rt, -HALF, 0f,  HALF, H, r,g,b,a);
        quadY_rev(mat, buf, rt, -HALF, 0f,  HALF, H, r,g,b,a);
        quadZ(mat, buf, rt, -HALF, 0f,  HALF, H, r,g,b,a);
        quadZ_rev(mat, buf, rt, -HALF, 0f,  HALF, H, r,g,b,a);

        buf.endBatch(rt); // ✅ correct call
    }

    // === helpers (unchanged) ===
    private static void quadY(org.joml.Matrix4f m, MultiBufferSource src, RenderType rt,
                              float x0,float y0,float x1,float h,
                              int r,int g,int b,int a) {
        VertexConsumer vc = src.getBuffer(rt);
        int light = 15728880;
        vc.addVertex(m, x0, y0,   0f).setColor(r,g,b,a).setUv(0f,0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
        vc.addVertex(m, x0, y0+h, 0f).setColor(r,g,b,a).setUv(0f,1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
        vc.addVertex(m, x1, y0+h, 0f).setColor(r,g,b,a).setUv(1f,1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
        vc.addVertex(m, x1, y0,   0f).setColor(r,g,b,a).setUv(1f,0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
    }
    private static void quadY_rev(org.joml.Matrix4f m, MultiBufferSource src, RenderType rt,
                                  float x0,float y0,float x1,float h,
                                  int r,int g,int b,int a) {
        VertexConsumer vc = src.getBuffer(rt);
        int light = 15728880;
        vc.addVertex(m, x1, y0,   0f).setColor(r,g,b,a).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0); // <-- added setOverlay
        vc.addVertex(m, x1, y0+h, 0f).setColor(r,g,b,a).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
        vc.addVertex(m, x0, y0+h, 0f).setColor(r,g,b,a).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
        vc.addVertex(m, x0, y0,   0f).setColor(r,g,b,a).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
    }

    private static void quadZ(org.joml.Matrix4f m, MultiBufferSource src, RenderType rt,
                              float z0,float y0,float z1,float h,
                              int r,int g,int b,int a) {
        VertexConsumer vc = src.getBuffer(rt);
        int light = 15728880;
        vc.addVertex(m, 0f, y0,   z0).setColor(r,g,b,a).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
        vc.addVertex(m, 0f, y0+h, z0).setColor(r,g,b,a).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
        vc.addVertex(m, 0f, y0+h, z1).setColor(r,g,b,a).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
        vc.addVertex(m, 0f, y0,   z1).setColor(r,g,b,a).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
    }
    private static void quadZ_rev(org.joml.Matrix4f m, MultiBufferSource src, RenderType rt,
                                  float z0,float y0,float z1,float h,
                                  int r,int g,int b,int a) {
        VertexConsumer vc = src.getBuffer(rt);
        int light = 15728880;
        vc.addVertex(m, 0f, y0,   z1).setColor(r,g,b,a).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
        vc.addVertex(m, 0f, y0+h, z1).setColor(r,g,b,a).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
        vc.addVertex(m, 0f, y0+h, z0).setColor(r,g,b,a).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
        vc.addVertex(m, 0f, y0,   z0).setColor(r,g,b,a).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0,1,0);
    }
}
