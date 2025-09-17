package net.oupz.bountyboard.player.renown;

import net.oupz.bountyboard.bounty.renown.CompletedBounty;

import java.util.List;

public interface PlayerRenown {
    int getTotalRenown();
    void setTotalRenown(int amount);
    default void addRenown(int delta) { setTotalRenown(getTotalRenown() + delta); }

    List<CompletedBounty> getHistory();
    void addCompleted(CompletedBounty entry);
    void clear();
}
