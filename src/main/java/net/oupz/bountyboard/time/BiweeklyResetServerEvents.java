package net.oupz.bountyboard.time;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.player.renown.RenownCapabilityEvents;
import net.oupz.bountyboard.world.ResetScheduleSavedData;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BiweeklyResetServerEvents {

    // Check roughly every 5 seconds (100 ticks)
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !level.dimension().equals(ServerLevel.OVERWORLD)) return;

        ResetScheduleSavedData data = ResetScheduleSavedData.get(level);
        long expected = BiweeklyReset.nextResetEpochSeconds();
        // Snap to expected if missing or wildly off (same 2-day threshold)
        if (data.getNextResetEpoch() <= 0L || Math.abs(expected - data.getNextResetEpoch()) > 172_800L) {
            data.setNextResetEpoch(expected);
        }
        broadcastNextResetToAll(level.getServer(), data.getNextResetEpoch());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (++tickCounter < 100) return; // ~5 seconds
        tickCounter = 0;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerLevel level = server.overworld();
        if (level == null) return;

        ResetScheduleSavedData data = ResetScheduleSavedData.get(level);
        long storedNext = data.getNextResetEpoch();
        long nowEpoch   = java.time.Instant.now().getEpochSecond();

        // --- NEW: realign if the stored epoch drifts too far from what the schedule expects right now ---
        long expectedNext = BiweeklyReset.nextResetEpochSeconds();
        // If off by more than 2 days (172800s), assume a manual clock jump and snap back to expected
        if (storedNext <= 0L || Math.abs(expectedNext - storedNext) > 172_800L) {
            data.setNextResetEpoch(expectedNext);
            broadcastNextResetToAll(server, expectedNext);
            storedNext = expectedNext;
        }

        // Normal firing: when we cross the boundary, perform the reset and schedule the next
        if (storedNext > 0L && nowEpoch >= storedNext) {
            // Zero all online players' renown
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                var cap = net.oupz.bountyboard.player.renown.RenownCapabilityEvents.get(p);
                cap.clear();
                net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                        new net.oupz.bountyboard.net.RenownSyncS2C(0),
                        p.connection.getConnection()
                );
            }

            long next = net.oupz.bountyboard.time.BiweeklyReset.nextResetEpochSeconds();
            data.setNextResetEpoch(next);
            broadcastNextResetToAll(server, next);
        }
    }

    private static void broadcastNextResetToAll(MinecraftServer server, long epoch) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new net.oupz.bountyboard.net.BiweeklyResetEpochS2C(epoch),
                    p.connection.getConnection()
            );
        }
    }
}
