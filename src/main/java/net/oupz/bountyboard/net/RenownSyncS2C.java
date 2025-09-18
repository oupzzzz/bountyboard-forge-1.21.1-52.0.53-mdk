package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oupz.bountyboard.client.ClientRenown;

public record RenownSyncS2C(int total) {
    public static void encode(RenownSyncS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.total);
    }

    public static RenownSyncS2C decode(FriendlyByteBuf buf) {
        return new RenownSyncS2C(buf.readVarInt());
    }

    public static void handle(RenownSyncS2C msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> handleClient(msg));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(RenownSyncS2C msg) {
        ClientRenown.setTotal(msg.total);
    }
}

