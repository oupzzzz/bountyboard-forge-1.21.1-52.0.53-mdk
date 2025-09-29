package net.oupz.bountyboard.wanted;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;

import java.util.List;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WantedKillEvents {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        // Must be killed by ANOTHER player
        var src = event.getSource().getEntity();
        if (!(src instanceof ServerPlayer killer)) return;
        if (killer.getUUID().equals(victim.getUUID())) return;

        ServerLevel level = victim.server.overworld();
        WantedSavedData data = WantedSavedData.get(level);

        // Determine current #1 wanted RIGHT NOW
        List<WantedSavedData.TopEntry> top = data.topN(1);
        if (top.isEmpty()) return;

        var top1 = top.get(0);
        if (!top1.id().equals(victim.getUUID())) {
            // Victim is not the current #1 wanted → do nothing for this flag
            return;
        }

        // Mark the #1 as killed this cycle
        data.markKilledThisCycle(victim.getUUID());

        // (Optional) broadcast a message or trigger S2C here if you want immediate UI feedback later
        // victim.server.getPlayerList().broadcastSystemMessage(Component.literal(victim.getName().getString() + " has fallen!"), false);
    }
}
