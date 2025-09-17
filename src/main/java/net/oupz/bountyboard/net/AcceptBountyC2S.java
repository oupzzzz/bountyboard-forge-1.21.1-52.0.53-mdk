package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.bounty.cap.ActiveBounty;
import net.oupz.bountyboard.bounty.cap.ActiveBountyProvider;

import net.oupz.bountyboard.init.ModNetworking;
import net.oupz.bountyboard.util.WorldAnchors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import net.oupz.bountyboard.bounty.Bounty;
import net.oupz.bountyboard.bounty.BountyRegistry;


public class AcceptBountyC2S {
    private final ResourceLocation bountyId;
    private final int tier;

    public AcceptBountyC2S(ResourceLocation bountyId, int tier) {
        this.bountyId = bountyId;
        this.tier = Math.max(0, Math.min(2, tier));
    }

    public ResourceLocation bountyId() { return bountyId; }
    public int tier() { return tier; }

    public static void encode(AcceptBountyC2S msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.bountyId);
        buf.writeVarInt(msg.tier);
    }

    public static AcceptBountyC2S decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        int tier = buf.readVarInt();
        return new AcceptBountyC2S(id, tier);
    }

    public static void handle(AcceptBountyC2S msg, net.minecraftforge.event.network.CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = ctx.getSender();
            if (sp == null) return;

            // --- DAILY GATES (do these first) ---
            if (net.oupz.bountyboard.bounty.limits.DailyLimit.hasReachedLimit(sp)) {
                int rem = net.oupz.bountyboard.bounty.limits.DailyLimit.remaining(sp);
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[bountyboard] Daily limit reached (" + rem + " remaining). Come back tomorrow."
                ));
                return; // bail before we modify any state or send waypoints
            }
            if (net.oupz.bountyboard.bounty.limits.DailyLimit.isCompletedToday(sp, msg.bountyId())) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[bountyboard] You've already completed that bounty today."
                ));
                return; // bail early
            }

            // --- Proceed with normal accept flow ---
            Bounty def = BountyRegistry.get(msg.bountyId());
            final int SEARCH_RADIUS = def != null ? def.searchRadius() : 128;

            ServerLevel level = sp.serverLevel();
            BlockPos center = sp.blockPosition();

            BlockPos anchorTmp = net.oupz.bountyboard.util.WorldAnchors
                    .randomSurfaceAnchorNear(level, center, SEARCH_RADIUS);
            if (anchorTmp == null) anchorTmp = center;
            final BlockPos anchor = anchorTmp.immutable();

            sp.getCapability(net.oupz.bountyboard.bounty.cap.ActiveBountyProvider.CAPABILITY).ifPresent(cap -> {
                cap.setBountyId(msg.bountyId());
                cap.setDimension(level.dimension());
                cap.setAnchorPos(anchor);
                cap.setTier(msg.tier());
                cap.setState(net.oupz.bountyboard.bounty.cap.ActiveBounty.State.ACCEPTED);
            });

            // send waypoint to this client only
            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new net.oupz.bountyboard.net.AnchorWaypointS2C(level.dimension(), anchor),
                    sp.connection.getConnection()
            );

            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "[bountyboard] Travel to "  + anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ() + " to start your tier " + (msg.tier() + 1) + " bounty!"
            ));
        });
        ctx.setPacketHandled(true);
    }
}

