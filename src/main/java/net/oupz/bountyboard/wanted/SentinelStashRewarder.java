package net.oupz.bountyboard.wanted;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.oupz.bountyboard.block.ModBlocks;

import java.util.UUID;

public final class SentinelStashRewarder {
    private SentinelStashRewarder() {}

    /** Give 1 Sentinel Stash to this UUID; if offline, store pending to deliver on login. */
    public static void awardOne(MinecraftServer server, UUID winnerId) {
        if (server == null || winnerId == null) return;

        ServerPlayer online = server.getPlayerList().getPlayer(winnerId);
        if (online != null) {
            giveNow(online, 1);
        } else {
            ServerLevel level = server.overworld();
            PendingStashSavedData.get(level).add(winnerId, 1);
        }
    }

    /** Deliver any pending stashes to this player (call on login). */
    public static void deliverPending(ServerPlayer player) {
        if (player == null) return;
        ServerLevel level = player.server.overworld();
        int count = PendingStashSavedData.get(level).takeAll(player.getUUID());
        if (count > 0) giveNow(player, count);
    }

    private static void giveNow(ServerPlayer player, int count) {
        ItemStack stack = new ItemStack(ModBlocks.BOUNTY_BOX_4.get(), count);
        // try inventory first, then drop if full
        boolean added = player.getInventory().add(stack);
        if (!added || !stack.isEmpty()) {
            player.drop(stack, false);
        }
        // optional: feedback
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "[bountyboard] You received " + count + " Sentinel Stash"));
    }
}
