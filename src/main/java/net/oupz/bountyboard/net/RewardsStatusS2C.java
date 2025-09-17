package net.oupz.bountyboard.net;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.client.ClientRewards;
import net.oupz.bountyboard.sounds.ModSounds;

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
        ctx.enqueueWork(() -> {
            int before = ClientRewards.pendingCount();
            ClientRewards.updateCount(msg.pending());
            int after = ClientRewards.pendingCount();

            // If the server says fewer pending rewards now, play a "success" sound
            if (after < before) {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    Minecraft.getInstance().player.playSound(ModSounds.REWARD_CLAIM.get(), 1.0F, 1.0F); // volume, pitch
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}

