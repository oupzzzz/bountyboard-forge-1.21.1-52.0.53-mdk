package net.oupz.bountyboard.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.oupz.bountyboard.client.ClientWantedTop;
import net.oupz.bountyboard.wanted.WantedSavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record TopWantedS2C(List<WantedSavedData.TopEntry> entries) {
    public static void encode(TopWantedS2C m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.entries.size());
        for (var e : m.entries) {
            buf.writeUUID(e.id());
            buf.writeUtf(e.name(), 64);
            buf.writeVarInt(e.renown());
        }
    }

    public static TopWantedS2C decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<WantedSavedData.TopEntry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf(64);
            int renown = buf.readVarInt();
            list.add(new WantedSavedData.TopEntry(id, name, renown));
        }
        return new TopWantedS2C(list);
    }

    public static void handle(TopWantedS2C msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> handleClient(msg));
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(TopWantedS2C msg) {
        ClientWantedTop.set(msg.entries);
    }
}
