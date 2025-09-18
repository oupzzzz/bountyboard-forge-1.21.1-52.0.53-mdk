package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.client.ClientResetClock;


public record BiweeklyResetEpochS2C(long epochSeconds) {
    public static void encode(BiweeklyResetEpochS2C m, FriendlyByteBuf b) { b.writeVarLong(m.epochSeconds); }
    public static BiweeklyResetEpochS2C decode(FriendlyByteBuf b) { return new BiweeklyResetEpochS2C(b.readVarLong()); }
    public static void handle(BiweeklyResetEpochS2C m, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> handleClient(m));
        ctx.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static void handleClient(BiweeklyResetEpochS2C m) { ClientResetClock.setNextEpoch(m.epochSeconds); }
}
