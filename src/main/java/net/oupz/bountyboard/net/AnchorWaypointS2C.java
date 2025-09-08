package net.oupz.bountyboard.net;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.oupz.bountyboard.client.ClientWaypoint;

public record AnchorWaypointS2C(ResourceKey<Level> dim, BlockPos pos) {
    public static void encode(AnchorWaypointS2C msg, FriendlyByteBuf buf) {
        buf.writeResourceKey(msg.dim);
        buf.writeBlockPos(msg.pos);
    }

    public static AnchorWaypointS2C decode(FriendlyByteBuf buf) {
        ResourceKey<Level> dim = buf.readResourceKey(Registries.DIMENSION);
        BlockPos pos = buf.readBlockPos();
        return new AnchorWaypointS2C(dim, pos);
    }

    public static void handle(AnchorWaypointS2C msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientWaypoint.set(msg.dim(), msg.pos())
        ));
        ctx.setPacketHandled(true);
    }
}
