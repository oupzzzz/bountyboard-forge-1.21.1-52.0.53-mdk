package net.oupz.bountyboard.bounty;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.bounty.cap.ActiveBounty;
import net.oupz.bountyboard.bounty.cap.ActiveBountyProvider;
import net.oupz.bountyboard.bounty.limits.DailyLimit;
import net.oupz.bountyboard.init.ModNetworking;
import net.oupz.bountyboard.net.ClearWaypointS2C;
import net.oupz.bountyboard.net.DailyStatusS2C;

import java.util.ArrayList;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BountyDeathHooks {
    private BountyDeathHooks() {}

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        sp.getCapability(ActiveBountyProvider.CAPABILITY).ifPresent(cap -> {
            if (cap.getState() != ActiveBounty.State.SPAWNED) return; // only fail if pack spawned

            // 1) Consume one daily attempt (no “completed” credit)
            DailyLimit.consumeOne(sp);

            // 2) Despawn tracked mobs
            ServerLevel level = sp.serverLevel();
            despawnTrackedMobs(level, cap);

            // 3) Clear the active bounty + waypoint
            tryClearWaypoint(sp);
            // either helper:
            if (hasResetAll(cap)) {
                cap.resetAll();
            } else {
                cap.setState(ActiveBounty.State.NONE);
                cap.setBountyId(null);
                cap.setAnchorPos(null);
                cap.clearSpawnedMobIds();
                cap.setTier(0);
                cap.setDimension(null);
            }

            // 4) Let the player know
            sp.sendSystemMessage(Component.literal("[bountyboard] You died. Bounty failed and one daily attempt was used."));

            // 5) Push fresh daily status to refresh UI immediately
            int used = DailyLimit.MAX_PER_DAY - DailyLimit.remaining(sp);
            long secs = DailyLimit.secondsUntilResetUtc();
            ModNetworking.CHANNEL.send(new DailyStatusS2C(used, secs), sp.connection.getConnection());
        });
    }

    private static boolean hasResetAll(ActiveBounty cap) {
        try {
            ActiveBounty.class.getMethod("resetAll");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static void tryClearWaypoint(ServerPlayer sp) {
        ModNetworking.CHANNEL.send(new ClearWaypointS2C(), sp.connection.getConnection());
    }

    private static void despawnTrackedMobs(ServerLevel level, ActiveBounty cap) {
        // copy to avoid concurrent modification
        var ids = new ArrayList<UUID>(cap.getSpawnedMobIds());
        for (UUID id : ids) {
            Entity e = level.getEntity(id);
            if (e != null) e.discard(); // removes entity server-side
        }
        cap.clearSpawnedMobIds();
    }
}
