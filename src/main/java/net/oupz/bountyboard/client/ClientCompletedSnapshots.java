package net.oupz.bountyboard.client;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientCompletedSnapshots {
    private static final Map<ResourceLocation, Integer> SNAP = new ConcurrentHashMap<>();

    private ClientCompletedSnapshots() {}

    public static void put(ResourceLocation id, int finalRenown) {
        if (id != null) SNAP.put(id, finalRenown);
    }

    public static Integer get(ResourceLocation id) {
        return id == null ? null : SNAP.get(id);
    }

    public static boolean has(ResourceLocation id) {
        return id != null && SNAP.containsKey(id);
    }

    /** Optional: call this on daily reset S2C if you want to clear yesterday’s entries */
    public static void clear() { SNAP.clear(); }
}
