package net.oupz.bountyboard.player.renown;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.oupz.bountyboard.bounty.renown.CompletedBounty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerRenownImpl implements PlayerRenown {
    private int totalRenown = 0;
    private final List<CompletedBounty> history = new ArrayList<>();

    @Override
    public int getTotalRenown() { return totalRenown; }

    @Override
    public void setTotalRenown(int amount) { this.totalRenown = Math.max(0, amount); }

    @Override
    public List<CompletedBounty> getHistory() { return history; }

    @Override
    public void addCompleted(CompletedBounty entry) { history.add(entry); }

    @Override
    public void clear() {
        totalRenown = 0;
        history.clear();
    }

    // ----- NBT -----
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Total", totalRenown);

        ListTag list = new ListTag();
        for (CompletedBounty b : history) {
            CompoundTag ct = new CompoundTag();
            ct.putUUID("BountyId", b.bountyId());
            ct.putInt("Base", b.baseRenown());
            ct.putInt("Tier", b.tierAtCompletion());
            ct.putFloat("Mult", b.tierMultiplierAtCompletion());
            ct.putInt("Final", b.finalRenown());
            ct.putLong("CompletedAt", b.completedAt());
            list.add(ct);
        }
        tag.put("History", list);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        clear();
        totalRenown = tag.getInt("Total");

        ListTag list = tag.getList("History", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag ct = (CompoundTag) t;
            history.add(new CompletedBounty(
                    ct.getUUID("BountyId"),
                    ct.getInt("Base"),
                    ct.getInt("Tier"),
                    ct.getFloat("Mult"),
                    ct.getInt("Final"),
                    ct.getLong("CompletedAt")
            ));
        }
    }
}
