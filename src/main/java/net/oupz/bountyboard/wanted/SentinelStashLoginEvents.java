package net.oupz.bountyboard.wanted;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;
import net.minecraft.server.level.ServerPlayer;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SentinelStashLoginEvents {
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            SentinelStashRewarder.deliverPending(sp);
        }
    }
}
