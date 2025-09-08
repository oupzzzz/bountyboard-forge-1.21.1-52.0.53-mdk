package net.oupz.bountyboard.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public class SpectralMarkEffect extends MobEffect {
    public SpectralMarkEffect(MobEffectCategory pCategory, int pColor) {
        // Use a category and color that fits your design (color here is just an example)
        super(pCategory, pColor);
    }

    @Override
    public boolean applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        // This method is called every tick as long as the effect is active.
        if (!pLivingEntity.level().isClientSide()) {
            double offsetX = (pLivingEntity.level().getRandom().nextDouble() - 0.5) * pLivingEntity.getBbWidth();
            double offsetY = pLivingEntity.level().getRandom().nextDouble() * pLivingEntity.getBbHeight();
            double offsetZ = (pLivingEntity.level().getRandom().nextDouble() - 0.5) * pLivingEntity.getBbWidth();
            ((ServerLevel) pLivingEntity.level()).sendParticles(
                    ParticleTypes.SOUL,             // Particle type
                    pLivingEntity.getX() + offsetX,         // X coordinate with a random offset
                    pLivingEntity.getY() + offsetY,         // Y coordinate with a random offset
                    pLivingEntity.getZ() + offsetZ,         // Z coordinate with a random offset
                    1,                               // Number of particles to spawn
                    0, 0, 0,                       // No additional offset for the particle spread
                    0.0                            // Particle speed

            );
            return true;
        }
        return super.applyEffectTick(pLivingEntity, pAmplifier);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int pDuration, int pAmplifier) {
        return true;
    }
}
