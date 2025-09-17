package net.oupz.bountyboard.item.weapons;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.oupz.bountyboard.effect.ModEffects;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

// === NO-FALL GRACE ===
import net.minecraftforge.event.entity.living.LivingFallEvent;

import java.util.HashMap;
import java.util.HashSet; // NEW
import java.util.Map;
import java.util.Set;   // NEW
import java.util.UUID;

import net.oupz.bountyboard.init.ModNetworking;

public class PhantomReaver extends SwordItem {

    public PhantomReaver(Tier pTier, Properties pProperties) {
        super(pTier, pProperties);
        MinecraftForge.EVENT_BUS.register(PhantomReaver.class);
    }

    // ===== Variables =====
    private double markChancePercent = 20/* % */;
    private int markDurationSeconds = 10;
    private double diameter = 8;
    private int walkDurationSeconds = 2;
    private int walkCooldownSeconds = 30;

    // ===== Helpers =====
    private int markDuration() { return markDurationSeconds * 20; }
    private double radius() { return diameter / 2.0; }
    private int walkDuration() { return walkDurationSeconds * 20; }
    private int walkCooldown() { return walkCooldownSeconds * 20; }

    // ===== Per-player timers/flags =====
    private static final Map<UUID, Integer> ghostWalkTimers = new HashMap<>();
    private static final Set<UUID> noFallAfterGhost = new HashSet<>();

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player && !attacker.level().isClientSide) {
            if (attacker.getRandom().nextDouble() < markChancePercent / 100.0) {
                target.addEffect(new MobEffectInstance(
                        ModEffects.SPECTRAL_MARK_EFFECT.getHolder().get(),
                        markDuration(),
                        0
                ));
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Built-in cooldown gate (shows white shrinking box on the item icon)
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND && stack.getItem() == this) {
            var aabb = player.getBoundingBox().inflate(radius());
            var marked = level.getEntitiesOfClass(
                    LivingEntity.class, aabb,
                    e -> e != player && e.hasEffect(ModEffects.SPECTRAL_MARK_EFFECT.getHolder().get())
            );

            if (!marked.isEmpty()) {
                LivingEntity target = marked.get(0);
                target.removeEffect(ModEffects.SPECTRAL_MARK_EFFECT.getHolder().get());

                if (player instanceof ServerPlayer sp) {
                    startGhostWalk(sp);
                }
            }
        }

        return InteractionResultHolder.success(stack);
    }

    private static void startGhostWalk(ServerPlayer player) {
        UUID id = player.getUUID();

        int ticks = 6 * 20;
        int cooldown = 30 * 20;
        if (player.getMainHandItem().getItem() instanceof PhantomReaver pr) {
            ticks = pr.walkDuration();
            cooldown = pr.walkCooldown();
        }

        // Invisibility (no particles / icon)
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false));

        // Grant + instantly enable flight
        var ab = player.getAbilities();
        ab.mayfly = true;
        ab.flying = true;
        player.onUpdateAbilities();
        player.fallDistance = 0.0F;

        // Arm Ghost Walk timer
        ghostWalkTimers.put(id, ticks);

        // >>> Built-in cooldown with visual UI bar <<<
        // Use the actual item in hand so the correct icon shows the cooldown
        player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), cooldown);

        // Tell clients to hide this player's render (armor + weapon) during Ghost Walk
        ModNetworking.broadcastGhostWalkStatus(player, true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            UUID id = sp.getUUID();

            // No-fall grace: until they touch ground after Ghost Walk
            if (noFallAfterGhost.contains(id)) {
                sp.fallDistance = 0.0F;
                if (sp.onGround()) {
                    noFallAfterGhost.remove(id);
                }
            }

            Integer timeLeft = ghostWalkTimers.get(id);
            if (timeLeft == null) continue;

            ServerLevel sLevel = sp.serverLevel();

            // Keep/force flight while ghost walking
            var ab = sp.getAbilities();
            if (!ab.mayfly) {
                ab.mayfly = true; sp.onUpdateAbilities();
            }
            if (!ab.flying) {
                ab.flying = true; sp.onUpdateAbilities();
            }
            sp.fallDistance = 0.0F;

            // Server-broadcast smoke
            sLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    sp.getX(), sp.getY() + 1.0, sp.getZ(),
                    12,
                    0.4, 0.8, 0.4,
                    0.02
            );

            // Tick down Ghost Walk
            timeLeft--;
            if (timeLeft <= 0) {
                endGhostWalk(sp);
                ghostWalkTimers.remove(id);
            } else {
                ghostWalkTimers.put(id, timeLeft);
            }
        }
    }


    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (ghostWalkTimers.containsKey(sp.getUUID())) {
            event.setCanceled(true);
        }
    }

    // === NO-FALL GRACE === cancel fall damage until first ground touch after Ghost Walk
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        UUID id = sp.getUUID();
        if (ghostWalkTimers.containsKey(id) || noFallAfterGhost.contains(id)) {
            event.setCanceled(true);
        }
    }

    private static void endGhostWalk(ServerPlayer player) {
        player.removeEffect(MobEffects.INVISIBILITY);

        if (!player.isCreative() && !player.isSpectator()) {
            var ab = player.getAbilities();
            ab.flying = false;
            ab.mayfly = false;
            player.onUpdateAbilities();
        }

        // Start grace until first ground touch
        noFallAfterGhost.add(player.getUUID());
        player.fallDistance = 0.0F;

        // Tell clients to show this player's render again
        ModNetworking.broadcastGhostWalkStatus(player, false);
    }
}
