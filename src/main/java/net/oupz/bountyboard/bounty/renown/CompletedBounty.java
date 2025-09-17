package net.oupz.bountyboard.bounty.renown;

import java.util.UUID;

public record CompletedBounty(
        UUID bountyId,
        int baseRenown,
        int tierAtCompletion,
        float tierMultiplierAtCompletion,
        int finalRenown,
        long completedAt // System.currentTimeMillis()
) {}
