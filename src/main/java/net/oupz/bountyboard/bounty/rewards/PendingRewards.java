package net.oupz.bountyboard.bounty.rewards;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class PendingRewards {
    private PendingRewards() {}

    private static final String ROOT = "bountyboard_rewards";
    private static final String K_T1 = "t1";
    private static final String K_T2 = "t2";
    private static final String K_T3 = "t3";

    private static CompoundTag tag(ServerPlayer sp) {
        var root = sp.getPersistentData();
        if (!root.contains(ROOT)) root.put(ROOT, new CompoundTag());
        return root.getCompound(ROOT);
    }

    public static void add(ServerPlayer sp, int tier, int count) {
        if (count <= 0) return;
        CompoundTag t = tag(sp);
        switch (tier) {
            case 0 -> t.putInt(K_T1, t.getInt(K_T1) + count);
            case 1 -> t.putInt(K_T2, t.getInt(K_T2) + count);
            case 2 -> t.putInt(K_T3, t.getInt(K_T3) + count);
            default -> {}
        }
    }

    public static int[] peekCounts(ServerPlayer sp) {
        CompoundTag t = tag(sp);
        return new int[]{ t.getInt(K_T1), t.getInt(K_T2), t.getInt(K_T3) };
    }

    /** Zeroes stored counts and returns the previous values. */
    public static int[] claimAll(ServerPlayer sp) {
        CompoundTag t = tag(sp);
        int c1 = t.getInt(K_T1), c2 = t.getInt(K_T2), c3 = t.getInt(K_T3);
        t.putInt(K_T1, 0); t.putInt(K_T2, 0); t.putInt(K_T3, 0);
        return new int[]{ c1, c2, c3 };
    }

    public static int total(ServerPlayer sp) {
        int[] c = peekCounts(sp);
        return c[0] + c[1] + c[2];
    }
}
