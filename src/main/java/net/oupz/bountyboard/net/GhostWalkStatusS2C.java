package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.UUID;

public record GhostWalkStatusS2C(UUID playerId, boolean active) {

    public static void encode(GhostWalkStatusS2C msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeBoolean(msg.active);
    }

    public static GhostWalkStatusS2C decode(FriendlyByteBuf buf) {
        return new GhostWalkStatusS2C(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(GhostWalkStatusS2C msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientGhostWalkState.set(msg.playerId(), msg.active())
        ));
        ctx.setPacketHandled(true);
    }

    // --- CLIENT ONLY holder of ghost-walking UUIDs ---
    public static final class ClientGhostWalkState {
        private static final java.util.Set<UUID> ACTIVE = new java.util.HashSet<>();
        public static void set(UUID id, boolean active) {
            if (active) ACTIVE.add(id); else ACTIVE.remove(id);
        }
        public static boolean isActive(UUID id) { return ACTIVE.contains(id); }
    }
}


