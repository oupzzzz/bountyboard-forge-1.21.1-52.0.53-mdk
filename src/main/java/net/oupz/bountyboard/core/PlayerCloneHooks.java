package net.oupz.bountyboard.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.bounty.cap.ActiveBountyProvider;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID)
public final class PlayerCloneHooks {
    private PlayerCloneHooks() {}

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        ServerPlayer oldPlayer = (ServerPlayer) event.getOriginal();

        // ---- Copy your DAILY LIMIT persistent tag ----
        // Must match the ROOT constant used in DailyLimit
        final String ROOT = "bountyboard_daily";
        var oldDaily = oldPlayer.getPersistentData().getCompound(ROOT);
        if (!oldDaily.isEmpty()) {
            newPlayer.getPersistentData().put(ROOT, oldDaily.copy());
        }

        // ---- OPTIONAL: copy your capability, if you want bounties to persist after death ----
        // (Skip this block if you WANT active bounties to reset on death.)
        oldPlayer.reviveCaps(); // allow reading caps from the old player
        newPlayer.getCapability(ActiveBountyProvider.CAPABILITY).ifPresent(newCap ->
                oldPlayer.getCapability(ActiveBountyProvider.CAPABILITY).ifPresent(oldCap -> {
                    // implement copyFrom(...) on your capability impl (see below)
                    newCap.copyFrom(oldCap);
                })
        );
        oldPlayer.invalidateCaps();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            net.oupz.bountyboard.init.ModNetworking.sendDailyStatusTo(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            net.oupz.bountyboard.init.ModNetworking.sendDailyStatusTo(sp);
        }
    }
}
