package net.oupz.bountyboard.wanted;

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
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

    private static void giveNow(ServerPlayer sp, int count) {
        // Base item to grant
        ItemStack base = new ItemStack(ModBlocks.BOUNTY_BOX_4.get());
        // Award stacks up to max stack size, drop overflow if necessary
        while (count > 0) {
            ItemStack stack = base.copy();
            int give = Math.min(stack.getMaxStackSize(), count);
            stack.setCount(give);
            boolean added = sp.getInventory().add(stack);
            // If inventory was full or something remained, drop it at player's feet
            if (!added || !stack.isEmpty()) {
                ItemEntity drop = new ItemEntity(sp.level(), sp.getX(), sp.getY() + 0.5, sp.getZ(), stack);
                drop.setNoPickUpDelay(); // so it can be immediately picked up
                sp.level().addFreshEntity(drop);
            }
            count -= give;
        }
        // Force the client to refresh any open GUIs just like ClaimRewardsC2S
        sp.getInventory().setChanged();
        sp.inventoryMenu.broadcastChanges();
        sp.containerMenu.broadcastChanges();
        // Send full container snapshots so the stash appears while any GUI (like the bounty board) is open
        int invState  = sp.inventoryMenu.getStateId();
        int contState = sp.containerMenu.getStateId();
        sp.connection.send(new ClientboundContainerSetContentPacket(
                sp.inventoryMenu.containerId, invState,
                sp.inventoryMenu.getItems(), sp.inventoryMenu.getCarried()
        ));
        sp.connection.send(new ClientboundContainerSetContentPacket(
                sp.containerMenu.containerId, contState,
                sp.containerMenu.getItems(), sp.containerMenu.getCarried()
        ));
    }

}
