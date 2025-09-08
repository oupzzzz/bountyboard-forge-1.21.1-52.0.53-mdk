package net.oupz.bountyboard.item.weapons;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.oupz.bountyboard.item.ModItems;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeadhuntersHatchet extends AxeItem {
    private static final Map<UUID, Integer> berserkPlayers = new ConcurrentHashMap<>();

    public HeadhuntersHatchet(Tier pTier, Properties pProperties) {
        super(pTier, pProperties);
    }

    public static void handleTick(Player player) {
        UUID uuid = player.getUUID();
        if (!player.isAlive()) {
            berserkPlayers.remove(uuid);
            return;
        }

        if (berserkPlayers.containsKey(uuid)) {
            int expireTick = berserkPlayers.get(uuid);
            if (player.tickCount < expireTick) {
                double tick = player.tickCount;
                double angle = tick * 0.18;
                double radius = 0.5;

                player.level().addParticle(ParticleTypes.FLAME,
                        player.getX() - (Math.cos(angle) * radius),
                        player.getY() + 0.5 + (Math.sin(tick * 0.4) * 0.5),
                        player.getZ() - (Math.sin(angle) * radius),
                        0, 0.1, 0);

                player.level().addParticle(ParticleTypes.FLAME,
                        player.getX() + (Math.cos(angle) * radius),
                        player.getY() + 0.5 + (Math.cos(tick * 0.4) * 0.5),
                        player.getZ() + (Math.sin(angle) * radius),
                        0, 0.1, 0);
            } else {
                berserkPlayers.remove(uuid);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getCooldowns().isOnCooldown(this)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 3, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3, false, false, false));

            player. getCooldowns().addCooldown(this, 200); //10 seconds (20 ticks a second)

            level.playSound(null, player.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);

            if (!level.isClientSide()) {
                ((ServerLevel) level).sendParticles(
                        ParticleTypes.FLAME,
                        player.getX(),
                        player.getY() + player.getBbHeight() * 0.5,
                        player.getZ(),
                        100,
                        0.05, 0.5, 0.05,
                        0.03
                );
                berserkPlayers.put(player.getUUID(), player.tickCount + 100);
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    public static void handleKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack heldStack = player.getMainHandItem();
            if (heldStack.getItem() == ModItems.HEADHUNTERS_HATCHET.get()) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, true, false, false));
            }
        }
    }
}
