package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
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

            int totalBefore = PendingRewards.total(sp);
            if (totalBefore <= 0) {
                net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                        new RewardsStatusS2C(0),
                        sp.connection.getConnection()
                );
                return;
            }

            int[] counts = PendingRewards.claimAll(sp);
            int t1 = counts[0], t2 = counts[1], t3 = counts[2];

            if (t1 > 0) giveOrDrop(sp, new ItemStack(ModBlocks.BOUNTY_BOX_1.get()), t1);
            if (t2 > 0) giveOrDrop(sp, new ItemStack(ModBlocks.BOUNTY_BOX_2.get()), t2);
            if (t3 > 0) giveOrDrop(sp, new ItemStack(ModBlocks.BOUNTY_BOX_3.get()), t3);

            // mark dirty & broadcast
            sp.getInventory().setChanged();
            sp.inventoryMenu.broadcastChanges();
            sp.containerMenu.broadcastChanges();

            // send full content snapshots with the correct stateId
            final int invState  = sp.inventoryMenu.getStateId();
            final int contState = sp.containerMenu.getStateId();

            sp.connection.send(new ClientboundContainerSetContentPacket(
                    sp.inventoryMenu.containerId,
                    invState,
                    sp.inventoryMenu.getItems(),
                    sp.inventoryMenu.getCarried()
            ));
            sp.connection.send(new ClientboundContainerSetContentPacket(
                    sp.containerMenu.containerId,
                    contState,
                    sp.containerMenu.getItems(),
                    sp.containerMenu.getCarried()
            ));

            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new RewardsStatusS2C(PendingRewards.total(sp)),
                    sp.connection.getConnection()
            );
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
