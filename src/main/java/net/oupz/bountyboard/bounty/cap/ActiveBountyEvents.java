package net.oupz.bountyboard.bounty.cap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oupz.bountyboard.BountyBoard;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID)
public class ActiveBountyEvents {

    private static final ResourceLocation KEY =
            ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "active_bounty");

    // Attach our provider to all players
    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(KEY, new ActiveBountyProvider());
        }
    }

    // Copy data on respawn (e.g., when player dies)
    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps(); // ensure caps are accessible
            event.getOriginal().getCapability(ActiveBountyProvider.CAPABILITY).ifPresent(oldCap -> {
                event.getEntity().getCapability(ActiveBountyProvider.CAPABILITY).ifPresent(newCap -> {
                    newCap.load(oldCap.save()); // simple copy via NBT
                });
            });
            event.getOriginal().invalidateCaps();
        }
    }
}
