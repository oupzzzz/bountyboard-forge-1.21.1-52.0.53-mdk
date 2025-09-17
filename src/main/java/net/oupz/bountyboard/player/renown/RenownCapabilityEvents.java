package net.oupz.bountyboard.player.renown;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RenownCapabilityEvents {

    @SubscribeEvent
    public static void onAttachCaps(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(PlayerRenownProvider.KEY, new PlayerRenownProvider());
        }
    }

    // Preserve data across death/respawn
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(PlayerRenownProvider.CAPABILITY).ifPresent(oldCap ->
                event.getEntity().getCapability(PlayerRenownProvider.CAPABILITY).ifPresent(newCap -> {
                    newCap.setTotalRenown(oldCap.getTotalRenown());
                    newCap.getHistory().clear();
                    newCap.getHistory().addAll(oldCap.getHistory());
                })
        );
        event.getOriginal().invalidateCaps();
    }

    // Helper
    public static PlayerRenown get(Player player) {
        return player.getCapability(PlayerRenownProvider.CAPABILITY)
                .orElseThrow(() -> new IllegalStateException(
                        "PlayerRenown capability missing on " + player.getGameProfile().getName()
                ));
    }
}
