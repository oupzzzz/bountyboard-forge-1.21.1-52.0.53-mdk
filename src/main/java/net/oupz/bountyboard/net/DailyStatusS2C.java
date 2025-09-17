package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.client.ClientDailyStatus;
import net.oupz.bountyboard.client.screen.BountyBoardScreen;

import java.util.HashSet;
import java.util.Set;

public class DailyStatusS2C {
    private final int completedToday;
    private final long secondsToReset;
    /** Fully-qualified ids completed today (e.g., "bountyboard:base_3") */
    private final Set<String> completedIds;

    public DailyStatusS2C(int completedToday, long secondsToReset, Set<String> completedIds) {
        this.completedToday = completedToday;
        this.secondsToReset = secondsToReset;
        this.completedIds = (completedIds == null) ? Set.of() : new HashSet<>(completedIds);
    }

    public int completedToday() { return completedToday; }
    public long secondsToReset() { return secondsToReset; }
    public Set<String> completedIds() { return completedIds; }

    public static void encode(DailyStatusS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.completedToday);
        buf.writeVarLong(msg.secondsToReset);

        // write size + each id
        buf.writeVarInt(msg.completedIds.size());
        for (String s : msg.completedIds) {
            buf.writeUtf(s);
        }
    }

    public static DailyStatusS2C decode(FriendlyByteBuf buf) {
        int c = buf.readVarInt();
        long s = buf.readVarLong();

        int n = buf.readVarInt();
        Set<String> ids = new HashSet<>(Math.max(0, n));
        for (int i = 0; i < n; i++) {
            ids.add(buf.readUtf());
        }

        return new DailyStatusS2C(c, s, ids);
    }

    /** CLIENT-SIDE handler */
    public static void handle(DailyStatusS2C msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Update the live cache (make sure ClientDailyStatus has this overload)
            ClientDailyStatus.update(msg.completedToday(), msg.secondsToReset(), msg.completedIds());

            // Tell the screen (if open) that a fresh status landed
            BountyBoardScreen.notifyDailyStatusRefreshed();
        });
        ctx.setPacketHandled(true);
    }
}