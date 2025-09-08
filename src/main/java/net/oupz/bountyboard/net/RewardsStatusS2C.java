package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.client.ClientRewards;

public class RewardsStatusS2C {
    private final int pending;

    public RewardsStatusS2C(int pending) { this.pending = pending; }
    public int pending() { return pending; }

    public static void encode(RewardsStatusS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.pending);
    }
    public static RewardsStatusS2C decode(FriendlyByteBuf buf) {
        return new RewardsStatusS2C(buf.readVarInt());
    }
    public static void handle(RewardsStatusS2C msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> ClientRewards.setPending(msg.pending()));
        ctx.setPacketHandled(true);
    }
}

