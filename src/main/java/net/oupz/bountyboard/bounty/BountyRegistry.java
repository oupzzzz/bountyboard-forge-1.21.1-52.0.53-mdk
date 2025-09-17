package net.oupz.bountyboard.bounty;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BountyRegistry {
    private BountyRegistry() {}

    private static final Map<ResourceLocation, Bounty> MAP = new HashMap<>();

    /** Call once during mod init to populate the in-memory registry. */
    public static void bootstrap(String modid) {
        MAP.clear();

        // --- Per-tier pools ---
        // Tier 1: 5 vindicators, 3 pillagers
        var T1 = List.<EntityType<? extends Mob>>of(
                EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.VINDICATOR,
                EntityType.PILLAGER,   EntityType.PILLAGER,   EntityType.PILLAGER
        );

        // Tier 2: 5 vindicators, 5 pillagers, 2 witch
        var T2 = List.<EntityType<? extends Mob>>of(
                EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.VINDICATOR,
                EntityType.PILLAGER,   EntityType.PILLAGER,   EntityType.PILLAGER,   EntityType.PILLAGER,   EntityType.PILLAGER,
                EntityType.WITCH,     EntityType.WITCH
        );

        // Tier 3: 4 vindicators, 4 pillagers, 3 evokers, 3 vexes, 2 ravagers, 1 witch
        var T3 = List.<EntityType<? extends Mob>>of(
                EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.VINDICATOR, EntityType.VINDICATOR,
                EntityType.PILLAGER,   EntityType.PILLAGER,   EntityType.PILLAGER, EntityType.PILLAGER,
                EntityType.EVOKER,     EntityType.EVOKER,     EntityType.EVOKER,
                EntityType.RAVAGER,    EntityType.RAVAGER,
                EntityType.WITCH
        );

        var POOLS = List.of(T1, T2, T3);

        // Register all your bounties with the same tiered pools for now
        add(modid, "illager_patrol_t1", 256, 15, POOLS);
        add(modid, "base_1",            128, 15, POOLS);
        add(modid, "base_2",            128, 15, POOLS);
        add(modid, "base_3",            128, 15, POOLS);
        add(modid, "base_4",            128, 15, POOLS);
        add(modid, "base_5",            128, 15, POOLS);
    }

    /** Helper to create and register a bounty. */
    private static void add(String modid, String path, int searchRadius, int triggerRadius,
                            List<List<EntityType<? extends Mob>>> pools) {
        var id = ResourceLocation.fromNamespaceAndPath(modid, path);
        var b  = new Bounty(id, searchRadius, triggerRadius, pools);
        MAP.put(id, b);
    }

    public static Bounty get(ResourceLocation id) {
        return MAP.get(id);
    }

    public static boolean exists(ResourceLocation id) {
        return MAP.containsKey(id);
    }
}
