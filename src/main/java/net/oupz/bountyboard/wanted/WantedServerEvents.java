package net.oupz.bountyboard.wanted;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.player.renown.RenownCapabilityEvents;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WantedServerEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;

        int total = RenownCapabilityEvents.get(sp).getTotalRenown();
        var data = WantedSavedData.get(sp.server.overworld());
        data.upsert(sp.getUUID(), sp.getGameProfile().getName(), total);

        // send top 3 to this player
        var top3 = data.topN(3);
        net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                new net.oupz.bountyboard.net.TopWantedS2C(top3),
                sp.connection.getConnection()
        );
    }
}
