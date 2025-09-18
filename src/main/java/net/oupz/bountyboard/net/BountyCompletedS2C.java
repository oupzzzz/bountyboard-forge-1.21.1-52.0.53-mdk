package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.client.ClientCompletedSnapshots;

import java.util.function.Supplier;

public record BountyCompletedS2C(ResourceLocation id, int finalRenown) {
    public static void encode(BountyCompletedS2C msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.id);
        buf.writeVarInt(msg.finalRenown);
    }

    public static BountyCompletedS2C decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        int val = buf.readVarInt();
        return new BountyCompletedS2C(id, val);
    }

    public static void handle(BountyCompletedS2C msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> handleClient(msg));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(BountyCompletedS2C msg) {
        ClientCompletedSnapshots.put(msg.id, msg.finalRenown);
        // you could also ping the screen to refresh if open; not strictly necessary
    }
}
