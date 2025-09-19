package net.oupz.bountyboard.bounty;

import net.minecraft.world.entity.Entity;
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

    /** Call this on any mob you spawn for a bounty. */
    public static void tagNoDrops(Entity e) {
        if (e != null) {
            e.getPersistentData().putBoolean(NBT_FLAG, true);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var e = event.getEntity();
        if (e != null && e.getPersistentData().getBoolean(NBT_FLAG)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingXp(LivingExperienceDropEvent event) {
        var e = event.getEntity();
        if (e != null && e.getPersistentData().getBoolean(NBT_FLAG)) {
            event.setDroppedExperience(0);
        }
    }
}
