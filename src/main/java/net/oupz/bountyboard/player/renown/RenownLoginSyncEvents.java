package net.oupz.bountyboard.player.renown;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.init.ModNetworking;
import net.oupz.bountyboard.net.TopWantedS2C;
import net.oupz.bountyboard.wanted.WantedSavedData;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RenownLoginSyncEvents {

    // Push current total to client when they join the world
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        sp.getCapability(PlayerRenownProvider.CAPABILITY).ifPresent(cap -> {
            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new net.oupz.bountyboard.net.RenownSyncS2C(cap.getTotalRenown()),
                    sp.connection.getConnection()
            );

            int newTotal = RenownCapabilityEvents.get(sp).getTotalRenown();
            var wdata = WantedSavedData.get(sp.server.overworld());
            wdata.upsert(sp.getUUID(), sp.getGameProfile().getName(), newTotal);
            var top3 = wdata.topN(3);
            for (ServerPlayer other : sp.server.getPlayerList().getPlayers()) {
                ModNetworking.CHANNEL.send(
                        new TopWantedS2C(top3),
                        other.connection.getConnection()
                );
            }
        });
    }

    // Optional: also push after respawn (death), so UI stays accurate if you rely on clone
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        sp.getCapability(PlayerRenownProvider.CAPABILITY).ifPresent(cap -> {
            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new net.oupz.bountyboard.net.RenownSyncS2C(cap.getTotalRenown()),
                    sp.connection.getConnection()
            );

            int newTotal = RenownCapabilityEvents.get(sp).getTotalRenown();
            var wdata = WantedSavedData.get(sp.server.overworld());
            wdata.upsert(sp.getUUID(), sp.getGameProfile().getName(), newTotal);
            var top3 = wdata.topN(3);
            for (ServerPlayer other : sp.server.getPlayerList().getPlayers()) {
                ModNetworking.CHANNEL.send(
                        new TopWantedS2C(top3),
                        other.connection.getConnection()
                );
            }
        });
    }
}
