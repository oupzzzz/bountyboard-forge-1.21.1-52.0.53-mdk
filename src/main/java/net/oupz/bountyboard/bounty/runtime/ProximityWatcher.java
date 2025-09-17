package net.oupz.bountyboard.bounty.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.bounty.Bounty;
import net.oupz.bountyboard.bounty.BountyRegistry;
import net.oupz.bountyboard.bounty.cap.ActiveBounty;
import net.oupz.bountyboard.bounty.cap.ActiveBountyProvider;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID)
public final class ProximityWatcher {
    private ProximityWatcher() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER || event.phase != TickEvent.Phase.END) return;

        ServerPlayer player = (ServerPlayer) event.player;
        var capOpt = player.getCapability(ActiveBountyProvider.CAPABILITY);
        if (!capOpt.isPresent()) return;

        ActiveBounty cap = capOpt.orElse(null);
        if (cap == null) return;
        if (cap.getDimension() == null) return;

        ServerLevel level = player.server.getLevel(cap.getDimension());
        if (level == null || player.level() != level) return;

        // --- If pack already spawned, prune dead mobs and check for completion ---
        if (cap.getState() == ActiveBounty.State.SPAWNED) {
            boolean anyAlive = pruneAndCheckAlive(level, cap);
            if (!anyAlive) {
                cap.setState(ActiveBounty.State.COMPLETED);

                int tier = cap.getTier(); // 0..2 as you already store
                net.oupz.bountyboard.bounty.rewards.PendingRewards.add(player, tier, 1);

                // Tell this client its new pending count for the UI badge
                int pending = net.oupz.bountyboard.bounty.rewards.PendingRewards.total(player);
                net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                        new net.oupz.bountyboard.net.RewardsStatusS2C(pending),
                        player.connection.getConnection()
                );

                // ▼▼ NEW: mark daily completion on the SERVER (UTC-based) ▼▼
                var id = cap.getBountyId();
                if (id != null) {
                    boolean counted = net.oupz.bountyboard.bounty.limits.DailyLimit.markCompleted(player, id);
                    int rem = net.oupz.bountyboard.bounty.limits.DailyLimit.remaining(player);
                    long sec = net.oupz.bountyboard.bounty.limits.DailyLimit.secondsUntilResetUtc();

                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "[bountyboard] Bounty completed!  Remaining today: " + rem + "  |  Reset in: " + formatHms(sec)
                    ));
                } else {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "[bountyboard] Bounty completed!"
                    ));
                }
                // ▲▲ NEW ▲▲

                // clear waypoint beam (finished bounty)
                net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                        new net.oupz.bountyboard.net.ClearWaypointS2C(),
                        player.connection.getConnection()
                );
            }
            return; // no further work this tick
        }

        // --- If not yet spawned, only proceed when ACCEPTED & we have an anchor ---
        if (cap.getState() != ActiveBounty.State.ACCEPTED) return;
        if (cap.getAnchorPos() == null) return;

        var bountyId = cap.getBountyId();
        Bounty def = bountyId != null ? BountyRegistry.get(bountyId) : null;
        if (def == null) return; // unknown bounty id; nothing to do

        // Proximity check against registry radius
        int r = def.triggerRadius();
        int r2 = r * r;

        var pos = player.blockPosition();
        var anchor = cap.getAnchorPos();
        int dx = pos.getX() - anchor.getX();
        int dy = pos.getY() - anchor.getY();
        int dz = pos.getZ() - anchor.getZ();
        int distSq = dx*dx + dy*dy + dz*dz;

        if (distSq <= r2) {
            int tier = cap.getTier(); // now using the tier stored on the capability
            spawnFromBounty(level, anchor, def, tier, cap); // record UUIDs as we spawn
            cap.setState(ActiveBounty.State.SPAWNED);

// clear waypoint beam (player reached it)
            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new net.oupz.bountyboard.net.ClearWaypointS2C(),
                    player.connection.getConnection()
            );

            player.sendSystemMessage(Component.literal(
                    "[bountyboard] Proximity trigger: spawned pack for " + bountyId +
                            " (r=" + r + ", tier=" + (tier + 1) + ")"
            ));
        }
    }

    /** Spawns every entry in the tier pool and records their UUIDs on the capability. */
    private static void spawnFromBounty(ServerLevel level, BlockPos anchor, Bounty def, int tier, ActiveBounty cap) {
        // Skip if Peaceful (hostiles will be removed immediately)
        if (level.getDifficulty().getId() == 0) return;

        var rng   = ThreadLocalRandom.current();
        var pool  = def.poolForTier(tier);   // full list (duplicates == multiple spawns)
        int ringR = 4;                       // scatter radius start
        int ringW = 6;                       // extra random spread

        cap.clearSpawnedMobIds();            // reset any stale IDs before we add new ones

        for (var type : pool) {
            int dx = rng.nextInt(-ringR, ringR + ringW);
            int dz = rng.nextInt(-ringR, ringR + ringW);
            BlockPos column = anchor.offset(dx, 0, dz);

            // Place on top of world surface and stand above it
            BlockPos surface  = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, column);
            BlockPos spawnPos = surface.above();

            Mob mob = type.create(level);
            if (mob == null) continue;

            // Nudge big mobs like ravagers up one extra block for safety
            if (type == EntityType.RAVAGER) {
                spawnPos = spawnPos.above(1);
            }

            mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    rng.nextFloat() * 360f, 0f);

            DifficultyInstance diff = level.getCurrentDifficultyAt(spawnPos);
            mob.finalizeSpawn(level, diff, MobSpawnType.EVENT, (SpawnGroupData) null);
            mob.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    Integer.MAX_VALUE,   // effectively infinite
                    0,                   // amplifier
                    true,                // ambient (quieter look)
                    false,               // show particles? (false = cleaner)
                    false                // show icon? (not needed for mobs)
            ));
            level.addFreshEntity(mob);

            cap.addSpawnedMobId(mob.getUUID()); // <-- record for completion tracking
        }
    }

    /** Remove dead/missing mobs from the tracked list and return whether any are still alive. */
    private static boolean pruneAndCheckAlive(ServerLevel level, ActiveBounty cap) {
        boolean anyAlive = false;
        var ids = cap.getSpawnedMobIds();

        // Iterate and remove invalid/dead entities
        for (Iterator<java.util.UUID> it = ids.iterator(); it.hasNext(); ) {
            java.util.UUID id = it.next();
            Entity e = level.getEntity(id);
            if (e == null || !e.isAlive()) {
                it.remove();
            } else {
                anyAlive = true;
            }
        }
        return anyAlive;
    }

    private static String formatHms(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}