package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.oupz.bountyboard.client.ClientWaypoint;

public record ClearWaypointS2C() {
    public static void encode(ClearWaypointS2C msg, FriendlyByteBuf buf) {}
    public static ClearWaypointS2C decode(FriendlyByteBuf buf) { return new ClearWaypointS2C(); }

    public static void handle(ClearWaypointS2C msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> ClientWaypoint::clear
        ));
        ctx.setPacketHandled(true);
    }
}
