package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.wanted.WantedSavedData;


public record RequestTopWantedC2S() {
    public static void encode(RequestTopWantedC2S m, FriendlyByteBuf b) {}
    public static RequestTopWantedC2S decode(FriendlyByteBuf b) { return new RequestTopWantedC2S(); }

    public static void handle(RequestTopWantedC2S m, CustomPayloadEvent.Context ctx) {
        ServerPlayer sp = ctx.getSender();
        if (sp != null) {
            var level = sp.server.overworld();
            var data  = WantedSavedData.get(level);
            var top3  = data.topN(3);
            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new TopWantedS2C(top3),
                    sp.connection.getConnection()
            );
        }
        ctx.setPacketHandled(true);
    }
}
