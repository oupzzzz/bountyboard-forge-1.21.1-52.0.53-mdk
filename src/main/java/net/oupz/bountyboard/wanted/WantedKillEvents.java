package net.oupz.bountyboard.wanted;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.block.ModBlocks;
import net.oupz.bountyboard.init.ModNetworking;
import net.oupz.bountyboard.net.RenownSyncS2C;
import net.oupz.bountyboard.net.TopWantedS2C;
import net.oupz.bountyboard.player.renown.RenownCapabilityEvents;

import java.util.List;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WantedKillEvents {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        // Must be killed by ANOTHER player
        var src = event.getSource().getEntity();
        if (!(src instanceof ServerPlayer killer)) return;
        if (killer.getUUID().equals(victim.getUUID())) return;

        ServerLevel level = victim.server.overworld();
        WantedSavedData wdata = WantedSavedData.get(level);

        // Is the victim currently on the Wanted board (top 3)?
        List<WantedSavedData.TopEntry> top3 = wdata.topN(3);
        boolean victimIsWanted = top3.stream().anyMatch(e -> e.id().equals(victim.getUUID()));
        if (!victimIsWanted) return; // not wanted -> no special effects

        // Renown transfer/loss (victim current total)
        var vCap = RenownCapabilityEvents.get(victim);
        var kCap = RenownCapabilityEvents.get(killer);

        int vBefore = vCap.getTotalRenown();
        if (vBefore <= 0) return; // nothing to move/lose

        int stolen  = (int)Math.floor(vBefore * 0.25);  // killer gains 25%
        int penalty = (int)Math.floor(vBefore * 0.50);  // victim loses 50%

        // Apply changes
        vCap.setTotalRenown(Math.max(0, vBefore - penalty)); // victim -50%
        kCap.setTotalRenown(kCap.getTotalRenown() + stolen); // killer +25%

        // Sync both clients’ totals
        ModNetworking.CHANNEL.send(
                new RenownSyncS2C(vCap.getTotalRenown()),
                victim.connection.getConnection()
        );
        ModNetworking.CHANNEL.send(
                new RenownSyncS2C(kCap.getTotalRenown()),
                killer.connection.getConnection()
        );

        // Update Wanted totals + broadcast fresh Top 3
        wdata.upsert(victim.getUUID(), victim.getGameProfile().getName(), vCap.getTotalRenown());
        wdata.upsert(killer.getUUID(), killer.getGameProfile().getName(), kCap.getTotalRenown());
        var freshTop3 = wdata.topN(3);
        for (ServerPlayer p : victim.server.getPlayerList().getPlayers()) {
            ModNetworking.CHANNEL.send(
                    new TopWantedS2C(freshTop3),
                    p.connection.getConnection()
            );
        }

        // If the victim was current #1, mark disqualified for Sentinel Stash
        var top1 = wdata.topN(1);
        if (!top1.isEmpty() && top1.get(0).id().equals(victim.getUUID())) {
            wdata.markKilledThisCycle(victim.getUUID());
        }

        giveOrDropAndSync(
                killer,
                new ItemStack(
                        ModBlocks.BOUNTY_BOX_4.get().asItem()
                )
        );

        // Optional: broadcast a flavorful message
        var msg = Component.literal("[bountyboard] " + victim.getGameProfile().getName()
                + " (WANTED) was slain by " + killer.getGameProfile().getName()
                + ". Killer +25%, victim -50%.");
        victim.server.getPlayerList().broadcastSystemMessage(msg, false);
    }

    private static void giveOrDropAndSync(ServerPlayer sp,
                                          ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        ItemStack copy = stack.copy();

        // Try to add to inventory; if anything remains, drop it
        boolean added = sp.getInventory().add(copy);
        if (!added || !copy.isEmpty()) {
            ItemEntity drop =
                    new ItemEntity(
                            sp.level(), sp.getX(), sp.getY() + 0.5, sp.getZ(), copy);
            drop.setNoPickUpDelay();
            sp.level().addFreshEntity(drop);
        }

        // Force client sync — mirrors your ClaimRewards flow
        sp.getInventory().setChanged();
        sp.inventoryMenu.broadcastChanges();
        sp.containerMenu.broadcastChanges();

        int invState  = sp.inventoryMenu.getStateId();
        sp.connection.send(new ClientboundContainerSetContentPacket(
                sp.inventoryMenu.containerId, invState,
                sp.inventoryMenu.getItems(), sp.inventoryMenu.getCarried()
        ));

        int contState = sp.containerMenu.getStateId();
        sp.connection.send(new ClientboundContainerSetContentPacket(
                sp.containerMenu.containerId, contState,
                sp.containerMenu.getItems(), sp.containerMenu.getCarried()
        ));
    }

}