package net.oupz.bountyboard.bounty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/** Definition for a bounty: search radius, trigger radius, and a simple mob pool. */
public final class Bounty {
    private final ResourceLocation id;
    private final int searchRadius;   // X: how far from player to place anchor
    private final int triggerRadius;  // r: distance to trigger spawn
    private final List<List<EntityType<? extends Mob>>> poolsByTier;

    /** Main constructor: explicit per-tier pools (index 0..2). */
    public Bounty(ResourceLocation id, int searchRadius, int triggerRadius,
                  List<List<EntityType<? extends Mob>>> poolsByTier) {
        if (poolsByTier == null || poolsByTier.isEmpty())
            throw new IllegalArgumentException("poolsByTier must contain at least one pool");
        this.id = id;
        this.searchRadius = searchRadius;
        this.triggerRadius = triggerRadius;
        this.poolsByTier = List.copyOf(poolsByTier);
    }

    /** Convenience factory: use the same pool for all three tiers. */
    public static Bounty uniform(ResourceLocation id, int searchRadius, int triggerRadius,
                                 List<EntityType<? extends Mob>> uniformPool) {
        return new Bounty(id, searchRadius, triggerRadius, List.of(uniformPool, uniformPool, uniformPool));
    }

    public ResourceLocation id() { return id; }
    public int searchRadius() { return searchRadius; }
    public int triggerRadius() { return triggerRadius; }

    /** Returns the pool for the given tier (clamped to available range). */
    public List<EntityType<? extends Mob>> poolForTier(int tier) {
        int idx = Math.max(0, Math.min(tier, poolsByTier.size() - 1));
        return poolsByTier.get(idx);
    }

    /** Picks a random mob from the given tier’s pool. */
    public EntityType<? extends Mob> pickMob(int tier) {
        var pool = poolForTier(tier);
        if (pool.isEmpty())
            throw new IllegalStateException("Empty mob pool for " + id + " at tier " + tier);
        int i = ThreadLocalRandom.current().nextInt(pool.size());
        return pool.get(i);
    }
}


