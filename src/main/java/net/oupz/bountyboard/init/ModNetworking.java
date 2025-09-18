package net.oupz.bountyboard.init;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.oupz.bountyboard.bounty.limits.DailyLimit;
import net.oupz.bountyboard.net.*;

public final class ModNetworking {
    private ModNetworking() {}

    private static final String MODID = "bountyboard";
    private static final int PROTOCOL = 1;

    public static final ResourceLocation CHANNEL_NAME =
            ResourceLocation.fromNamespaceAndPath(MODID, "main");

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(CHANNEL_NAME)
            .networkProtocolVersion(PROTOCOL)
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .simpleChannel();

    private static int NEXT_ID = 0;
    public static int nextId() { return NEXT_ID++; }

    public static void sendDailyStatusTo(ServerPlayer sp) {
        int used = DailyLimit.MAX_PER_DAY - DailyLimit.remaining(sp);
        used = Mth.clamp(used, 0, DailyLimit.MAX_PER_DAY);

        long seconds = DailyLimit.secondsUntilResetUtc();

        java.util.Set<String> ids = DailyLimit.completedIdStrings(sp);

        CHANNEL.send(
                new DailyStatusS2C(used, seconds, ids),
                sp.connection.getConnection()
        );
    }

    public static void broadcastGhostWalkStatus(ServerPlayer subject, boolean active) {
        GhostWalkStatusS2C msg = new GhostWalkStatusS2C(subject.getUUID(), active);
        // Send to every connected player (so all clients know to hide/show the subject)
        for (ServerPlayer viewer : subject.server.getPlayerList().getPlayers()) {
            CHANNEL.send(msg, viewer.connection.getConnection());
        }
    }


    public static void init() {
        CHANNEL.messageBuilder(AcceptBountyC2S.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(AcceptBountyC2S::encode)
                .decoder(AcceptBountyC2S::decode)
                .consumerMainThread(AcceptBountyC2S::handle)
                .add();

        CHANNEL.messageBuilder(AnchorWaypointS2C.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AnchorWaypointS2C::encode)
                .decoder(AnchorWaypointS2C::decode)
                .consumerMainThread(AnchorWaypointS2C::handle)
                .add();

        CHANNEL.messageBuilder(ClearWaypointS2C.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClearWaypointS2C::encode)
                .decoder(ClearWaypointS2C::decode)
                .consumerMainThread(ClearWaypointS2C::handle)
                .add();

        CHANNEL.messageBuilder(DailyStatusS2C.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DailyStatusS2C::encode)
                .decoder(DailyStatusS2C::decode)
                .consumerMainThread(DailyStatusS2C::handle)
                .add();

        CHANNEL.messageBuilder(RequestDailyStatusC2S.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestDailyStatusC2S::encode)
                .decoder(RequestDailyStatusC2S::decode)
                .consumerMainThread(RequestDailyStatusC2S::handle)
                .add();

        CHANNEL.messageBuilder(ClaimRewardsC2S.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClaimRewardsC2S::encode)
                .decoder(ClaimRewardsC2S::decode)
                .consumerMainThread(ClaimRewardsC2S::handle)
                .add();

        CHANNEL.messageBuilder(RewardsStatusS2C.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RewardsStatusS2C::encode)
                .decoder(RewardsStatusS2C::decode)
                .consumerMainThread(RewardsStatusS2C::handle)
                .add();

        // NEW: Ghost Walk render toggle (server -> client)
        CHANNEL.messageBuilder(GhostWalkStatusS2C.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GhostWalkStatusS2C::encode)
                .decoder(GhostWalkStatusS2C::decode)
                .consumerMainThread(GhostWalkStatusS2C::handle)
                .add();

        CHANNEL.messageBuilder(BountyCompletedS2C.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BountyCompletedS2C::encode)
                .decoder(BountyCompletedS2C::decode)
                .consumerMainThread(BountyCompletedS2C::handle)
                .add();

        CHANNEL.messageBuilder(RenownSyncS2C.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RenownSyncS2C::encode)
                .decoder(RenownSyncS2C::decode)
                .consumerMainThread(RenownSyncS2C::handle)
                .add();

        CHANNEL.messageBuilder(BiweeklyResetEpochS2C.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BiweeklyResetEpochS2C::encode)
                .decoder(BiweeklyResetEpochS2C::decode)
                .consumerMainThread(BiweeklyResetEpochS2C::handle)
                .add();

        CHANNEL.messageBuilder(RequestBiweeklyResetEpochC2S.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestBiweeklyResetEpochC2S::encode)
                .decoder(RequestBiweeklyResetEpochC2S::decode)
                .consumerMainThread(RequestBiweeklyResetEpochC2S::handle)
                .add();
    }

}
