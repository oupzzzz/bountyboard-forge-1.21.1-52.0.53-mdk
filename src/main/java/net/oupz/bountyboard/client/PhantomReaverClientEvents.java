// net/oupz/bountyboard/client/PhantomReaverClientEvents.java
package net.oupz.bountyboard.client;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.oupz.bountyboard.net.GhostWalkStatusS2C;
// If you kept the name GhostWalkStatusS2C, adjust the import:

@Mod.EventBusSubscriber(modid = "bountyboard", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PhantomReaverClientEvents {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player p = event.getEntity();
        if (GhostWalkStatusS2C.ClientGhostWalkState.isActive(p.getUUID())) {
            // Hide the entire player, including armor & held items
            event.setCanceled(true);
        }
    }
}
