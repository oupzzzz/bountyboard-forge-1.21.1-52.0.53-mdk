package net.oupz.bountyboard.bounty;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraft.world.entity.LivingEntity;
import net.oupz.bountyboard.BountyBoard;

@Mod.EventBusSubscriber(modid = BountyBoard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BountyNoDrops {
    private static final String NBT_FLAG = "bountyboard_nodrops";

    private BountyNoDrops() {}

    /** Remove all item drops from bounty-spawned mobs. */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (e == null) return;
        if (e.getPersistentData().getBoolean(NBT_FLAG)) {
            // nukes the drop event entirely (no items, no special banners, etc.)
            event.setCanceled(true);
        }
    }

    /** Remove all XP from bounty-spawned mobs. */
    @SubscribeEvent
    public static void onLivingXp(LivingExperienceDropEvent event) {
        LivingEntity e = event.getEntity();
        if (e == null) return;
        if (e.getPersistentData().getBoolean(NBT_FLAG)) {
            event.setDroppedExperience(0);
        }
    }
}
