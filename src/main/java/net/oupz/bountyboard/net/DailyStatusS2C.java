package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.client.ClientDailyStatus; // <-- add this import
import net.oupz.bountyboard.client.screen.BountyBoardScreen;

public class DailyStatusS2C {
    private final int completedToday;
    private final long secondsToReset;

    public DailyStatusS2C(int completedToday, long secondsToReset) {
        this.completedToday = completedToday;
        this.secondsToReset = secondsToReset;
    }

    public int completedToday() { return completedToday; }
    public long secondsToReset() { return secondsToReset; }

    public static void encode(DailyStatusS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.completedToday);
        buf.writeVarLong(msg.secondsToReset);
    }

    public static DailyStatusS2C decode(FriendlyByteBuf buf) {
        int c = buf.readVarInt();
        long s = buf.readVarLong();
        return new DailyStatusS2C(c, s);
    }

    /** CLIENT-SIDE handler */
    public static void handle(DailyStatusS2C msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // refresh the live cache
            ClientDailyStatus.update(msg.completedToday(), msg.secondsToReset());

            // tell the screen (if open) that a fresh status landed
            BountyBoardScreen.notifyDailyStatusRefreshed();
        });

        ctx.setPacketHandled(true);
    }
}
