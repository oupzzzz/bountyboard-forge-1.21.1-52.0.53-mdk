package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.bounty.limits.DailyLimit;

import java.util.Set;

public class RequestDailyStatusC2S {

    public RequestDailyStatusC2S() {}

    public static void encode(RequestDailyStatusC2S msg, FriendlyByteBuf buf) {}
    public static RequestDailyStatusC2S decode(FriendlyByteBuf buf) { return new RequestDailyStatusC2S(); }

    public static void handle(RequestDailyStatusC2S msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp == null) return;

            // server’s authoritative snapshot
            int used   = DailyLimit.MAX_PER_DAY - DailyLimit.remaining(sp);
            long secs  = DailyLimit.secondsUntilResetUtc();
            Set<String> ids = DailyLimit.completedIdStrings(sp);

            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new DailyStatusS2C(used, secs, ids),
                    sp.connection.getConnection()
            );
        });
        ctx.setPacketHandled(true);
    }
}
