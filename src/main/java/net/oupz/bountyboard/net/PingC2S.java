package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PingC2S {
    public PingC2S() {}

    public static void encode(PingC2S msg, FriendlyByteBuf buf) {}

    public static PingC2S decode(FriendlyByteBuf buf) {
        return new PingC2S();
    }

    // NOTE: takes Context directly (no Supplier)
    public static void handle(PingC2S msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp != null) {
                sp.sendSystemMessage(Component.literal("[bountyboard] Server received PingC2S"));
            }
        });
        ctx.setPacketHandled(true);
    }
}