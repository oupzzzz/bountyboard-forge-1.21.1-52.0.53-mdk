package net.oupz.bountyboard.wanted;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.checkerframework.checker.units.qual.C;

import java.util.*;
import java.util.stream.Collectors;

public class WantedSavedData extends SavedData {
    private static final String KEY = "bountyboard_wanted";

    private final Map<UUID, Integer> totals = new HashMap<>();
    private final Map<UUID, String>  lastKnownNames = new HashMap<>();

    // NEW: #1 wanted killed-by-player flag for the current cycle
    private final Set<UUID> killedThisCycle = new HashSet<>();

    public WantedSavedData() {}

    public static WantedSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WantedSavedData::new, WantedSavedData::load, (DataFixTypes) null),
                KEY
        );
    }

    public static WantedSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        WantedSavedData d = new WantedSavedData();

        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag e = (CompoundTag) t;
            UUID id = e.getUUID("Id");
            int renown = e.getInt("Renown");
            String name = e.getString("Name");
            d.totals.put(id, renown);
            if (!name.isEmpty()) d.lastKnownNames.put(id, name);
        }

        // NEW: load killed flags
        ListTag killed = tag.getList("Killed", Tag.TAG_INT_ARRAY); // UUID stored as int-array
        for (Tag t : killed) {
            CompoundTag wrap = (CompoundTag) t;
            // if you chose to store as UUID directly in a list of compounds:
            // UUID id = wrap.getUUID("Id");
            // d.killedThisCycle.add(id);
        }
        // If you prefer simpler storage, write & read as a ListTag of UUIDs directly:
        // (shown below in save(...), see matching load there)

        ListTag killedUuids = tag.getList("KilledUuids", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < killedUuids.size(); i++) {
            // Using CompoundTag#putUUID creates an int-array under the hood; ListTag of int-arrays is fine:
            // However ListTag doesn’t cast; easiest is store as a ListTag of CompoundTags (below in save).
        }

        // Better: store as List<CompoundTag> with "Id" UUID (see save below)
        ListTag killedList = tag.getList("KilledList", Tag.TAG_COMPOUND);
        for (int i = 0; i < killedList.size(); i++) {
            CompoundTag ct = killedList.getCompound(i);
            if (ct.contains("IdMost") && ct.contains("IdLeast")) {
                d.killedThisCycle.add(ct.getUUID("Id"));
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (var entry : totals.entrySet()) {
            UUID id = entry.getKey();
            int renown = entry.getValue();
            CompoundTag e = new CompoundTag();
            e.putUUID("Id", id);
            e.putInt("Renown", renown);
            String name = lastKnownNames.getOrDefault(id, "");
            if (!name.isEmpty()) e.putString("Name", name);
            list.add(e);
        }
        tag.put("Entries", list);

        // NEW: save killed flags as a list of compounds with UUIDs
        ListTag killedList = new ListTag();
        for (UUID id : killedThisCycle) {
            CompoundTag ct = new CompoundTag();
            ct.putUUID("Id", id);
            killedList.add(ct);
        }
        tag.put("KilledList", killedList);

        return tag;
    }

    public void upsert(UUID id, String name, int renown) {
        totals.put(id, renown);
        if (name != null && !name.isEmpty()) lastKnownNames.put(id, name);
        setDirty();
    }

    public List<TopEntry> topN(int n) {
        return totals.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(n)
                .map(e -> new TopEntry(e.getKey(), lastKnownNames.getOrDefault(e.getKey(), shortUuid(e.getKey())), e.getValue()))
                .collect(Collectors.toList());
    }

    public void clearAll() {
        totals.clear();
        lastKnownNames.clear();
        killedThisCycle.clear(); // NEW: also clear flags on reset
        setDirty();
    }

    // NEW: helpers for the flag
    public void markKilledThisCycle(UUID id) {
        if (id != null) {
            killedThisCycle.add(id);
            setDirty();
        }
    }

    public boolean wasKilledThisCycle(UUID id) {
        return id != null && killedThisCycle.contains(id);
    }

    public void clearKilledFlags() {
        killedThisCycle.clear();
        setDirty();
    }

    public record TopEntry(UUID id, String name, int renown) {}

    private static String shortUuid(UUID u) {
        String s = u.toString();
        int dash = s.indexOf('-');
        return dash > 0 ? s.substring(0, dash) : s;
    }
}
