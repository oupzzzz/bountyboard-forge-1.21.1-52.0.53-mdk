package net.oupz.bountyboard.wanted;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PendingStashSavedData extends SavedData {
    private static final String KEY = "bountyboard_pending_stash";
    private final Map<UUID, Integer> pending = new HashMap<>();

    public PendingStashSavedData() {}

    public static PendingStashSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PendingStashSavedData::new, PendingStashSavedData::load, (DataFixTypes) null),
                KEY
        );
    }

    public static PendingStashSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PendingStashSavedData d = new PendingStashSavedData();
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            UUID id = e.getUUID("Id");
            int cnt = e.getInt("Count");
            d.pending.put(id, cnt);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (var entry : pending.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putUUID("Id", entry.getKey());
            e.putInt("Count", entry.getValue());
            list.add(e);
        }
        tag.put("Entries", list);
        return tag;
    }

    public void add(UUID id, int count) {
        pending.merge(id, count, Integer::sum);
        setDirty();
    }

    public int takeAll(UUID id) {
        Integer v = pending.remove(id);
        if (v != null) setDirty();
        return v == null ? 0 : v;
    }

    public int peek(UUID id) { return pending.getOrDefault(id, 0); }
}

