package net.oupz.bountyboard.bounty.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.bounty.Bounty;
import net.oupz.bountyboard.bounty.BountyNoDrops;
import net.oupz.bountyboard.bounty.BountyRegistry;
import net.oupz.bountyboard.bounty.cap.ActiveBounty;
import net.oupz.bountyboard.bounty.cap.ActiveBountyProvider;
import net.oupz.bountyboard.bounty.limits.DailyLimit;
import net.oupz.bountyboard.bounty.renown.CompletedBounty;
import net.oupz.bountyboard.bounty.renown.RenownHelper;
import net.oupz.bountyboard.bounty.rewards.PendingRewards;
import net.oupz.bountyboard.init.ModNetworking;
import net.oupz.bountyboard.net.*;
import net.oupz.bountyboard.player.renown.PlayerRenown;
import net.oupz.bountyboard.player.renown.RenownCapabilityEvents;
import net.oupz.bountyboard.wanted.WantedSavedData;

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

                // --- NEW: compute and award renown once (server-side) ---
                int tier = cap.getTier(); // 0..2 as you already store
                ResourceLocation bountyId = cap.getBountyId();

                int baseRenown = RenownHelper
                        .getBaseRenown(cap.getBountyId(), player.getUUID());
                float mult     = RenownHelper.getMultiplierForTier(tier);
                int finalRenown = Math.round(baseRenown * mult);



                // Add to player's renown capability + append history entry
                PlayerRenown capRenown =
                        RenownCapabilityEvents.get(player);
                capRenown.addRenown(finalRenown);

                // If your CompletedBounty uses ResourceLocation as the id:
                java.util.UUID bountyUuid = java.util.UUID.nameUUIDFromBytes(
                        (bountyId != null ? bountyId.toString() : "unknown")
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)
                );

                capRenown.addCompleted(new CompletedBounty(
                        bountyUuid,
                        baseRenown,
                        tier,
                        mult,
                        finalRenown,
                        System.currentTimeMillis()
                ));

                ModNetworking.CHANNEL.send(
                        new RenownSyncS2C(capRenown.getTotalRenown()),
                        player.connection.getConnection()
                );

                ModNetworking.CHANNEL.send(
                        new BountyCompletedS2C(bountyId, finalRenown),
                        player.connection.getConnection()
                );

                int newTotal = RenownCapabilityEvents.get(player).getTotalRenown();
                var wdata = WantedSavedData.get(player.server.overworld());
                wdata.upsert(player.getUUID(), player.getGameProfile().getName(), newTotal);
                var top3 = wdata.topN(3);
                for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
                    ModNetworking.CHANNEL.send(
                            new TopWantedS2C(top3),
                            other.connection.getConnection()
                    );
                }

                // --- existing pending reward update (keep) ---
                PendingRewards.add(player, tier, 1);

                // Tell this client its new pending count for the UI badge
                int pending = PendingRewards.total(player);
                ModNetworking.CHANNEL.send(
                        new RewardsStatusS2C(pending),
                        player.connection.getConnection()
                );

                // ▼▼ Keep your daily limit update ▼▼
                var id = cap.getBountyId();
                if (id != null) {
                    boolean counted = DailyLimit.markCompleted(player, id);
                    int rem = DailyLimit.remaining(player);
                    long sec = DailyLimit.secondsUntilResetUtc();

                    player.sendSystemMessage(Component.literal(
                            "[bountyboard] Bounty completed!  Remaining today: " + rem + "  |  Reset in: " + formatHms(sec)
                    ));
                } else {
                    player.sendSystemMessage(Component.literal(
                            "[bountyboard] Bounty completed!"
                    ));
                }
                // ▲▲ Keep ▲▲

                // clear waypoint beam (finished bounty)
                ModNetworking.CHANNEL.send(
                        new ClearWaypointS2C(),
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
            ModNetworking.CHANNEL.send(
                    new ClearWaypointS2C(),
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

        var rng   = java.util.concurrent.ThreadLocalRandom.current();
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

            // === NO-DROPS TAG & BELT-AND-SUSPENDERS ===
            // 1) Tag for your drop/xp event filters
            BountyNoDrops.tagNoDrops(mob);
            // 2) Prevent equipment from dropping and stop picking up loot
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                mob.setDropChance(slot, 0.0f);
            }
            mob.setCanPickUpLoot(false);
            // =========================================

            mob.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    Integer.MAX_VALUE,   // effectively infinite
                    0,                   // amplifier
                    true,                // ambient (quieter look)
                    false,               // show particles? (false = cleaner)
                    false                // show icon? (not needed for mobs)
            ));
            level.addFreshEntity(mob);

            cap.addSpawnedMobId(mob.getUUID()); // record for completion tracking
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