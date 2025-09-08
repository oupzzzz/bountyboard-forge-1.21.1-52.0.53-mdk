package net.oupz.bountyboard.util;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.oupz.bountyboard.effect.ModEffects;
import net.oupz.bountyboard.item.ModItems;
import net.oupz.bountyboard.item.weapons.HeadhuntersHatchet;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModEvents {

    public ModEvents() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static final Map<UUID, Integer> spectatorTimers = new HashMap<>();

    public static void addSpectatorTimer(UUID playerId, int ticks) {
        spectatorTimers.put(playerId, ticks);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            Integer timeLeft = spectatorTimers.get(id);
            if (timeLeft != null) {
                // ---- OLD: this only exists on ServerLevel, not Level ----
                // ((ServerLevel)player.level()).sendParticles(
                //     ParticleTypes.SMOKE,
                //     player.getX(), player.getY(), player.getZ(),
                //     8,    // count
                //     0.5, 0.5, 0.5,  // spread
                //     0.01  // speed
                // );

                // ---- NEW: always‑visible particles (ignores client "minimal particles" setting) ----
                Level world = player.level();
                if (!world.isClientSide) {
                    // spawn 8 always‑visible smoke puffs
                    for (int i = 0; i < 8; i++) {
                        world.addAlwaysVisibleParticle(
                                ParticleTypes.SMOKE,
                                player.getX(), player.getY(), player.getZ(),
                                0.5, 0.5, 0.5   // velocity/spread
                        );
                    }
                }

                // tick down…
                timeLeft--;
                if (timeLeft <= 0) {
                    CompoundTag data = player.getPersistentData();
                    String prev = data.getString("phantomReaverPrevMode");
                    GameType oldType = GameType.byName(prev);
                    player.setGameMode(oldType);
                    spectatorTimers.remove(id);
                } else {
                    spectatorTimers.put(id, timeLeft);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        HeadhuntersHatchet.handleKill(event);
    }


    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.player.level().isClientSide() && event.phase == TickEvent.Phase.END) {
            HeadhuntersHatchet.handleTick(event.player);
        }

        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        boolean holdingMain = player.getMainHandItem().getItem() == ModItems.RAVAGER_WRECKER.get();
        boolean holdingOff  = player.getOffhandItem().getItem() == ModItems.RAVAGER_WRECKER.get();

        if (holdingMain || holdingOff) {
            // refresh slowness
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    10,    // ticks
                    1,    // amp
                    true, // ambient
                    false // no particles
            ));
        } else {
            // not holding it: remove any leftover slowness immediately
            if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }
    }

    @SubscribeEvent
    public void onEntityHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack heldStack = player.getMainHandItem();

            if (heldStack.getItem() == ModItems.RAVAGER_WRECKER.get()) {
                LivingEntity target = (LivingEntity) event.getEntity();
                double dx = player.getX() - target.getX();
                double dz = player.getZ() - target.getZ();
                float knockbackMultiplier = 1.5f;
                target.knockback(knockbackMultiplier, dx, dz);
            }
        }
    }

    private static final float KNOCKBACK_MULTIPLIER = 3.0f;

    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource src = target.getLastDamageSource();
        if (src == null) return;
        Entity attackerEntity = src.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;
        if (attacker.getMainHandItem().getItem() != ModItems.RAVAGER_WRECKER.get())
            return;

        // cancel the vanilla knockback
        event.setCanceled(true);

        // compute direction
        double dx = target.getX() - attacker.getX();
        double dz = target.getZ() - attacker.getZ();
        float strength = event.getStrength() * KNOCKBACK_MULTIPLIER;

        // apply your custom knockback
        target.knockback(strength, dx, dz);
    }
}
