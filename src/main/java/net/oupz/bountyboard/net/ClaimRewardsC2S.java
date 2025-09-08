package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.block.ModBlocks;
import net.oupz.bountyboard.bounty.rewards.PendingRewards;

// Replace these with your actual items!
import net.minecraft.world.item.ItemStack;

public class ClaimRewardsC2S {

    public ClaimRewardsC2S() {}
    public static void encode(ClaimRewardsC2S msg, FriendlyByteBuf buf) {}
    public static ClaimRewardsC2S decode(FriendlyByteBuf buf) { return new ClaimRewardsC2S(); }

    public static void handle(ClaimRewardsC2S msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp == null) return;

            // Snapshot before claim (for optional client ping)
            int totalBefore = PendingRewards.total(sp);
            if (totalBefore <= 0) {
                // Still notify client to clear any stale badge
                net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                        new RewardsStatusS2C(0),
                        sp.connection.getConnection()
                );
                // (Optional) If you implemented RewardsClaimedS2C, send 0 as a no-op ping
                // net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                //         new RewardsClaimedS2C(0), sp.connection.getConnection());
                return;
            }

            // Pop & zero server-side pending counters
            int[] counts = PendingRewards.claimAll(sp);
            int t1 = counts[0], t2 = counts[1], t3 = counts[2];

            // Grant immediately; overflow drops at feet with no pickup delay.
            if (t1 > 0) giveOrDrop(sp, new ItemStack(ModBlocks.BOUNTY_BOX_1.get().asItem()), t1);
            if (t2 > 0) giveOrDrop(sp, new ItemStack(ModBlocks.BOUNTY_BOX_2.get().asItem()), t2);
            if (t3 > 0) giveOrDrop(sp, new ItemStack(ModBlocks.BOUNTY_BOX_3.get().asItem()), t3);

            // Force inventory sync while a screen is open
            sp.inventoryMenu.broadcastChanges();

            // Tell client badge = 0 now
            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new RewardsStatusS2C(PendingRewards.total(sp)),
                    sp.connection.getConnection()
            );

            // (Optional but recommended) If you added RewardsClaimedS2C, ping it so the GUI
            // can clear the badge instantly and play a small sound:
            // net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
            //         new RewardsClaimedS2C(t1 + t2 + t3),
            //         sp.connection.getConnection()
            // );
        });
        ctx.setPacketHandled(true);
    }

    private static void giveOrDrop(ServerPlayer sp, ItemStack baseStack, int count) {
        if (count <= 0) return;

        while (count > 0) {
            ItemStack stack = baseStack.copy();
            int give = Math.min(stack.getMaxStackSize(), count);
            stack.setCount(give);

            boolean added = sp.getInventory().add(stack);
            if (!added && !stack.isEmpty()) {
                ItemEntity drop = new ItemEntity(sp.serverLevel(), sp.getX(), sp.getY() + 0.5, sp.getZ(), stack);
                drop.setPickUpDelay(0); // instantly pickup-able
                drop.setThrower(sp);    // marks player as thrower/owner (for advancement triggers, etc.)
                sp.level().addFreshEntity(drop);
            }
            count -= give;
        }
    }
}
