package net.oupz.bountyboard.util;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.oupz.bountyboard.item.ModItems;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModEvents {

    public ModEvents() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack heldStack = player.getMainHandItem();
            if (heldStack.getItem() == ModItems.HEADHUNTERS_HATCHET.get()) {

                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, true, false, false));
            }
        }
    }

    private static final Map<UUID, Integer> berserkPlayers=new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onHatchetRightClick(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if(stack.getItem() == ModItems.HEADHUNTERS_HATCHET.get()) {
            if(!event.getEntity().getCooldowns().isOnCooldown(stack.getItem())) {
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false, false));
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 3, false, false, false));
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3, false, false, false));

                event.getEntity().getCooldowns().addCooldown(stack.getItem(), 000);

                event.getLevel().playSound(null, event.getEntity().blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F);

                if(!event.getLevel().isClientSide()) {
                    ((net.minecraft.server.level.ServerLevel) event.getLevel()).sendParticles(
                            ParticleTypes.FLAME,
                            event.getEntity().getX(),
                            event.getEntity().getY() + event.getEntity().getBbHeight() * 0.5,
                            event.getEntity().getZ(),
                            100,
                            0.05, 0.5, 0.05,
                            0.03
                    );
                    berserkPlayers.put(event.getEntity().getUUID(), event.getEntity().tickCount + 100);
                }
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.player.level().isClientSide() && event.phase == TickEvent.Phase.END) {
            if (!event.player.isAlive()) {
                berserkPlayers.remove(event.player.getUUID());
                return;
            }


            UUID uuid = event.player.getUUID();
            if(berserkPlayers.containsKey(uuid)) {
                int expireTick = berserkPlayers.get(uuid);
                if(event.player.tickCount < expireTick) {

                    double tick = event.player.tickCount;
                    double angle = tick * 0.18;
                    double radius = 0.5;

                    event.player.level().addParticle(ParticleTypes.FLAME,
                            event.player.getX() - (Math.cos(angle) * radius),
                            event.player.getY() + 0.5 + (Math.sin(tick * 0.4) * 0.5),
                            event.player.getZ() - (Math.sin(angle) * radius),
                            0, 0.1, 0);

                    event.player.level().addParticle(ParticleTypes.FLAME,
                            event.player.getX() + (Math.cos(angle) * radius),
                            event.player.getY() + 0.5 + (Math.cos(tick * 0.4) * 0.5),
                            event.player.getZ() + (Math.sin(angle) * radius),
                            0, 0.1, 0);
                } else {
                    berserkPlayers.remove(uuid);
                }
            }
        }
    }


}
