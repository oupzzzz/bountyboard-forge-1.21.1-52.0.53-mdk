package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.bounty.limits.DailyLimit;

public class RequestDailyStatusC2S {

    public RequestDailyStatusC2S() {}

    public static void encode(RequestDailyStatusC2S msg, FriendlyByteBuf buf) {
        // no body
    }

    public static RequestDailyStatusC2S decode(FriendlyByteBuf buf) {
        return new RequestDailyStatusC2S();
    }

    public static void handle(RequestDailyStatusC2S msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp == null) return;

            int completedToday = DailyLimit.MAX_PER_DAY - DailyLimit.remaining(sp);
            if (completedToday < 0) completedToday = 0;

            long secs = DailyLimit.secondsUntilResetUtc();
            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new DailyStatusS2C(completedToday, secs),
                    sp.connection.getConnection()
            );
        });
        ctx.setPacketHandled(true);
    }
}
