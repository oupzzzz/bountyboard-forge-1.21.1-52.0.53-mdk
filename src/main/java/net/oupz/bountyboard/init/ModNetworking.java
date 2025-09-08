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
        // We don't store "used" directly in API, but we can derive it from remaining().
        int used = DailyLimit.MAX_PER_DAY - DailyLimit.remaining(sp);
        used = Mth.clamp(used, 0, DailyLimit.MAX_PER_DAY);

        long seconds = DailyLimit.secondsUntilResetUtc();

        CHANNEL.send(new DailyStatusS2C(used, seconds), sp.connection.getConnection());
    }


    public static void init() {
        CHANNEL.messageBuilder(AcceptBountyC2S.class, nextId(),
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(net.oupz.bountyboard.net.AcceptBountyC2S::encode)
                .decoder(net.oupz.bountyboard.net.AcceptBountyC2S::decode)
                .consumerMainThread(net.oupz.bountyboard.net.AcceptBountyC2S::handle)
                .add();

        CHANNEL.messageBuilder(AnchorWaypointS2C.class, nextId(),
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(net.oupz.bountyboard.net.AnchorWaypointS2C::encode)
                .decoder(net.oupz.bountyboard.net.AnchorWaypointS2C::decode)
                .consumerMainThread(net.oupz.bountyboard.net.AnchorWaypointS2C::handle)
                .add();

        CHANNEL.messageBuilder(ClearWaypointS2C.class, nextId(),
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(net.oupz.bountyboard.net.ClearWaypointS2C::encode)
                .decoder(net.oupz.bountyboard.net.ClearWaypointS2C::decode)
                .consumerMainThread(net.oupz.bountyboard.net.ClearWaypointS2C::handle)
                .add();

        CHANNEL.messageBuilder(net.oupz.bountyboard.net.DailyStatusS2C.class, nextId(),
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(net.oupz.bountyboard.net.DailyStatusS2C::encode)
                .decoder(net.oupz.bountyboard.net.DailyStatusS2C::decode)
                .consumerMainThread(net.oupz.bountyboard.net.DailyStatusS2C::handle)
                .add();

        CHANNEL.messageBuilder(RequestDailyStatusC2S.class, nextId(),
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestDailyStatusC2S::encode)
                .decoder(RequestDailyStatusC2S::decode)
                .consumerMainThread(RequestDailyStatusC2S::handle)
                .add();

        CHANNEL.messageBuilder(net.oupz.bountyboard.net.ClaimRewardsC2S.class, nextId(),
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
                .encoder(net.oupz.bountyboard.net.ClaimRewardsC2S::encode)
                .decoder(net.oupz.bountyboard.net.ClaimRewardsC2S::decode)
                .consumerMainThread(net.oupz.bountyboard.net.ClaimRewardsC2S::handle)
                .add();

        CHANNEL.messageBuilder(net.oupz.bountyboard.net.RewardsStatusS2C.class, nextId(),
                        net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(net.oupz.bountyboard.net.RewardsStatusS2C::encode)
                .decoder(net.oupz.bountyboard.net.RewardsStatusS2C::decode)
                .consumerMainThread(net.oupz.bountyboard.net.RewardsStatusS2C::handle)
                .add();
    }
}
