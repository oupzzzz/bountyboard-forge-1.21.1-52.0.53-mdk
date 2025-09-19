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
import net.oupz.bountyboard.init.ModNetworking;
import net.oupz.bountyboard.net.TopWantedS2C;
import net.oupz.bountyboard.player.renown.RenownCapabilityEvents;
import net.oupz.bountyboard.wanted.SentinelStashRewarder;
import net.oupz.bountyboard.wanted.WantedSavedData;
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

        // 1) If we are due (or past due), RUN THE RESET NOW (do NOT realign first)
        if (storedNext > 0L && nowEpoch >= storedNext) {

            // --- Award Sentinel Stash to #1 if eligible (not killed this cycle) ---
            {
                var wdata = net.oupz.bountyboard.wanted.WantedSavedData.get(level);
                var top1  = wdata.topN(1);
                if (!top1.isEmpty()) {
                    var entry = top1.get(0);
                    boolean disqualified = wdata.wasKilledThisCycle(entry.id());
                    if (!disqualified) {
                        SentinelStashRewarder.awardOne(server, entry.id());
                    }
                }
                // Clear wanted data for the new cycle (also clears killed flags)
                wdata.clearAll();
            }

            // --- Zero all online players' renown and push GUI sync ---
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                var cap = net.oupz.bountyboard.player.renown.RenownCapabilityEvents.get(p);
                cap.clear(); // zero total + clear history

                net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                        new net.oupz.bountyboard.net.RenownSyncS2C(0),
                        p.connection.getConnection()
                );
            }

            // --- Broadcast fresh (now empty) Top 3 to everyone ---
            {
                var wdata = net.oupz.bountyboard.wanted.WantedSavedData.get(level);
                var top3  = wdata.topN(3);
                for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                    net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                            new net.oupz.bountyboard.net.TopWantedS2C(top3),
                            other.connection.getConnection()
                    );
                }
            }

            // Schedule next reset and broadcast epoch to clients
            long next = net.oupz.bountyboard.time.BiweeklyReset.nextResetEpochSeconds();
            data.setNextResetEpoch(next);
            broadcastNextResetToAll(server, next);
            return; // done this tick
        }

        // 2) Not due yet → optionally realign if drifted (protects against clock jumps in prod)
        long expectedNext = net.oupz.bountyboard.time.BiweeklyReset.nextResetEpochSeconds();
        if (storedNext <= 0L || Math.abs(expectedNext - storedNext) > 172_800L) {
            data.setNextResetEpoch(expectedNext);
            broadcastNextResetToAll(server, expectedNext);
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
