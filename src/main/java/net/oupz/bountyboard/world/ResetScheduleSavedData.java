package net.oupz.bountyboard.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class ResetScheduleSavedData extends SavedData {
    private static final String KEY = "bountyboard_reset_schedule";
    private long nextResetEpoch = 0L;

    public ResetScheduleSavedData() {}

    // 1.21: use computeIfAbsent(Factory, String)
    public static ResetScheduleSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        ResetScheduleSavedData::new,          // supplier: when file doesn't exist
                        ResetScheduleSavedData::load,         // loader: (tag, provider) -> data
                        (DataFixTypes) null                   // no DFU for custom data
                ),
                KEY
        );
    }

    // Loader must accept (CompoundTag, HolderLookup.Provider)
    public static ResetScheduleSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ResetScheduleSavedData d = new ResetScheduleSavedData();
        d.nextResetEpoch = tag.getLong("NextResetEpoch");
        return d;
    }

    // 1.21 override includes HolderLookup.Provider
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("NextResetEpoch", nextResetEpoch);
        return tag;
    }

    public long getNextResetEpoch() { return nextResetEpoch; }

    public void setNextResetEpoch(long epoch) {
        this.nextResetEpoch = epoch;
        this.setDirty();
    }
}
