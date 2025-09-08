package net.oupz.bountyboard.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oupz.bountyboard.BountyBoard;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, BountyBoard.MOD_ID);

    public static final RegistryObject<MobEffect> SPECTRAL_MARK_EFFECT = MOB_EFFECTS.register("spectral_mark",
            () -> new SpectralMarkEffect(MobEffectCategory.NEUTRAL, 0x36ebab));


    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}