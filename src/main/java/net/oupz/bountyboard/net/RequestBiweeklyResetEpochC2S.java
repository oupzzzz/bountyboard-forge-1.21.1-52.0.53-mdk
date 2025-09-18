package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.world.ResetScheduleSavedData;

import java.util.function.Supplier;

public record RequestBiweeklyResetEpochC2S() {
    public static void encode(RequestBiweeklyResetEpochC2S m, FriendlyByteBuf b) {}
    public static RequestBiweeklyResetEpochC2S decode(FriendlyByteBuf b) { return new RequestBiweeklyResetEpochC2S(); }
    public static void handle(RequestBiweeklyResetEpochC2S m, CustomPayloadEvent.Context ctx) {
        ServerPlayer sp = ctx.getSender();
        if (sp != null) {
            var level = sp.server.overworld();
            var data = ResetScheduleSavedData.get(level);
            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new BiweeklyResetEpochS2C(data.getNextResetEpoch()),
                    sp.connection.getConnection()
            );
        }
        ctx.setPacketHandled(true);
    }
}

