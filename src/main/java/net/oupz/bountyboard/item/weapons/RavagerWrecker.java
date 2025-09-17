package net.oupz.bountyboard.item.weapons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Mod.EventBusSubscriber(modid = "bountyboard")
public class RavagerWrecker extends SwordItem {

    public RavagerWrecker(Tier tier, Properties props) {
        super(tier, props);
        MinecraftForge.EVENT_BUS.register(RavagerWrecker.class);
    }

    // =======================
    // Balance knobs
    // =======================
    private int  holdSlownessAmplifier      = 1;
    private int  holdSlownessRefreshTicks   = 10;
    private boolean applyHoldSlowness       = true;

    private float knockbackMultiplier       = 3f;   // (used in LivingKnockBackEvent scaler)
    private float minBaseKnockback          = 1f;   // ensures some KB even when vanilla gives 0

    private double slamRadiusBlocks         = 10.0;   // true radius of slam
    private double slamLaunchHeightBlocks   = 20.0;   // target vertical height
    private double slamHorizontalSpeed      = 2;   // outward shove speed added to targets
    private int    slamCooldownSeconds      = 15;      // cooldown bar after slam
    private int    slamChargeSeconds        = 2;      // 0 = immediate; >0 = charge before slam

    private int onHitSlownessSeconds   = 4;  // Slowness II for 4s
    private int onHitSlownessAmplifier = 4;  // 0 = Slowness I

    // Charge-up particle knobs
    private int    chargeParticlesPerTick = 8;     // how many per tick
    private double chargeParticleRadius   = 1.5;  // ring radius around feet
    private double chargeParticleYOffset  = 0.05;  // how high above ground
    private double chargeParticleSpeed    = 0.01;

    private double ring1Radius       = 1;  // small ring (instant)
    private double ring2Radius       = 2;  // medium ring (mid-charge)
    private double ring3Radius       = 3;  // large ring (right before slam)

    private double ring2AtFraction   = 0.2;  // when to spawn ring 2 (0..1 of charge)
    private double ring3AtFraction   = 0.7;  // when to spawn ring 3 (0..1 of charge)

    private static final int PASSIVE_SLOW_AMP = 1;


    private static final ResourceLocation ROOT_ID =
            ResourceLocation.fromNamespaceAndPath("bountyboard", "rr_charge_root");
    private static final AttributeModifier ROOT_MOD =
            new AttributeModifier(ROOT_ID, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private int onHitSlowTicks() { return onHitSlownessSeconds * 20; }
    private void spawnChargeParticles(ServerLevel sl, Player p) {
        // spawn a small ring around the feet
        for (int i = 0; i < chargeParticlesPerTick; i++) {
            double angle = sl.random.nextDouble() * Math.PI * 2.0;
            double r = chargeParticleRadius + sl.random.nextDouble() * 0.1; // slight jitter
            double x = p.getX() + Math.cos(angle) * r;
            double z = p.getZ() + Math.sin(angle) * r;
            double y = Math.floor(p.getY()) + chargeParticleYOffset;

            // one particle at a precise point; tiny vertical speed
            sl.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.0, 0.0, 0.0, chargeParticleSpeed);
        }
    }


    // Track who is charging and who just slammed (to avoid double-fire on release)
    private static final ConcurrentMap<UUID, Boolean> CHARGING = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Boolean> JUST_SLAMMED = new ConcurrentHashMap<>();

    // =======================
    // Helpers
    // =======================
    private static RavagerWrecker getHeld(Player p) {
        ItemStack main = p.getMainHandItem();
        if (main.getItem() instanceof RavagerWrecker rw) return rw;
        ItemStack off = p.getOffhandItem();
        if (off.getItem() instanceof RavagerWrecker rw) return rw;
        return null;
    }

    private int onHitSlownessTicks() {
        return onHitSlownessAmplifier * 20;
    }

    private static double launchVelY(double heightBlocks) {
        final double MC_GRAVITY = 0.08D;
        return Math.sqrt(2.0D * MC_GRAVITY * heightBlocks);
    }

    private void applyRoot(Player p) {
        AttributeInstance inst = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst != null && inst.getModifier(ROOT_ID) == null) {
            inst.addTransientModifier(ROOT_MOD);
        }
        p.setSprinting(false);
        p.setDeltaMovement(0, p.getDeltaMovement().y, 0); // zero horizontal momentum
    }

    private void clearRoot(Player p) {
        AttributeInstance inst = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst != null && inst.getModifier(ROOT_ID) != null) {
            inst.removeModifier(ROOT_ID);
        }
        p.setSprinting(false);
        p.setDeltaMovement(0, p.getDeltaMovement().y, 0);
    }

    private int chargeTicks() { return Math.max(1, slamChargeSeconds * 20); }
    private int cooldownTicks() { return slamCooldownSeconds * 20; }

    private static BlockState groundState(ServerLevel sl, Player p) {
        BlockPos on = p.getOnPos();
        BlockState state = sl.getBlockState(on);
        if (state.isAir()) {
            BlockState below = sl.getBlockState(on.below());
            state = below.isAir() ? Blocks.STONE.defaultBlockState() : below;
        }
        return state;
    }

    private void spawnRing(ServerLevel sl, Player p, BlockState state, double radius) {
        double baseY = p.getOnPos().getY() + 1.0 + chargeParticleYOffset;
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, state);

        for (int i = 0; i < chargeParticlesPerTick; i++) {
            double angle = sl.random.nextDouble() * Math.PI * 2.0;
            double x = p.getX() + Math.cos(angle) * radius;
            double z = p.getZ() + Math.sin(angle) * radius;

            sl.sendParticles(
                    debris,
                    x, baseY, z,
                    1,
                    0.02, 0.01, 0.02,   // tiny spread
                    chargeParticleSpeed  // your speed knob
            );
        }
    }


    // =======================
    // Use (hold to charge)
    // =======================
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        player.startUsingItem(hand);

        // Set CHARGING on BOTH sides so client can do local freeze/fov behavior
        CHARGING.put(player.getUUID(), Boolean.TRUE);

        if (!world.isClientSide) {
            applyRoot(player);                           // root via attribute (server)
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Server-only: apply a short slowness burst to the hit enemy
        if (!attacker.level().isClientSide) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    80,
                    onHitSlownessTicks(),
                    false,   // ambient
                    false     // show particles for easy verification; set false later if you prefer
            ));
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        // Only when damage actually goes through
        if (event.isCanceled() || event.getAmount() <= 0) return;

        // Attacker must be a player holding RavagerWrecker
        Entity src = event.getSource().getEntity();
        if (!(src instanceof Player attacker)) return;

        RavagerWrecker rw = getHeld(attacker);
        if (rw == null) return;

        LivingEntity target = event.getEntity();

        // Server-side: apply the on-hit slowness
        if (!attacker.level().isClientSide) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    80,
                    onHitSlownessTicks(),
                    false,   // ambient
                    false     // show particles for easy verification; set false later if you prefer
            ));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return chargeTicks();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE; // no bow/eat animation (and related FOV)
    }

    // Called every tick while holding right-click
    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int countRemaining) {
        if (!(living instanceof Player p)) return;

        // Keep the player frozen while charging
        if (Boolean.TRUE.equals(CHARGING.get(p.getUUID()))) {
            if (!level.isClientSide) {
                applyRoot(p); // keep root active on server

                if (level instanceof ServerLevel sl) {
                    // progress through charge
                    int total = chargeTicks();
                    int elapsed = Math.max(0, total - countRemaining);
                    double frac = total <= 0 ? 1.0 : (elapsed / (double) total);

                    // ground state once per tick
                    BlockState state = groundState(sl, p);

                    // Ring 1: ALWAYS while charging
                    spawnRing(sl, p, state, ring1Radius);

                    // Ring 2: from mid-charge onward
                    if (frac >= ring2AtFraction) {
                        spawnRing(sl, p, state, ring2Radius);
                    }

                    // Ring 3: from late-charge onward
                    if (frac >= ring3AtFraction) {
                        spawnRing(sl, p, state, ring3Radius);
                    }
                }
            } else {
                // client: zero horizontal to avoid drift/FOV jitter
                p.setSprinting(false);
                p.setDeltaMovement(0, p.getDeltaMovement().y, 0);
            }
        }

        // Charge completes
        if (countRemaining <= 1) {
            if (!level.isClientSide && p instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
                doSlam(sl, sp, this);
                JUST_SLAMMED.put(p.getUUID(), Boolean.TRUE);
            }

            // clear charge + root on BOTH sides, then stop using
            CHARGING.remove(p.getUUID());
            clearRoot(p);
            p.stopUsingItem();
        }
    }

    // Called when the player releases right-click OR when use is stopped
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!(living instanceof Player p)) return;

        // Always clear root + charging state (both sides) when use ends
        CHARGING.remove(p.getUUID());
        clearRoot(p);

        // If we already slammed in onUseTick, just exit
        if (Boolean.TRUE.equals(JUST_SLAMMED.remove(p.getUUID()))) {
            return;
        }
        // Else: early release -> no slam
    }

    @SubscribeEvent
    public static void onPlayerTickChargeWatch(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player p = event.player;
        UUID id = p.getUUID();

        // If we *think* we're charging but the player isn't actually using the item anymore,
        // clean up the root right away.
        if (Boolean.TRUE.equals(CHARGING.get(id))) {
            boolean stillUsing = p.isUsingItem() && p.getUseItem().getItem() instanceof RavagerWrecker;
            if (!stillUsing) {
                CHARGING.remove(id);
                // clear root on both sides
                AttributeInstance inst = p.getAttribute(Attributes.MOVEMENT_SPEED);
                if (inst != null) {
                    // Remove our modifier if present (matches your current ROOT_UUID setup)
                    if (inst.getModifier(ROOT_ID) != null) {
                        inst.removeModifier(ROOT_ID);
                    }
                }
                p.setSprinting(false);
                p.setDeltaMovement(0, p.getDeltaMovement().y, 0);
            }
        }
    }

    // =======================
    // The slam itself: up + outward push, then cooldown
    // =======================
    private static void doSlam(ServerLevel level, ServerPlayer player, RavagerWrecker rw) {
        double r  = rw.slamRadiusBlocks;
        double vy = launchVelY(rw.slamLaunchHeightBlocks);
        double hs = rw.slamHorizontalSpeed;

        var pos = player.position();
        var box = new net.minecraft.world.phys.AABB(
                pos.x - r, pos.y - r, pos.z - r,
                pos.x + r, pos.y + r, pos.z + r
        );

        var targets = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e != player && e.distanceTo(player) <= r
        );

        for (var e : targets) {
            // Direction from player -> entity on XZ (unit vector)
            double dx = e.getX() - player.getX();
            double dz = e.getZ() - player.getZ();
            double len2 = dx * dx + dz * dz;

            double dirX, dirZ;
            if (len2 < 1.0E-6) {
                double yaw = Math.toRadians(player.getYRot());
                dirX = -Math.sin(yaw);
                dirZ =  Math.cos(yaw);
            } else {
                double invLen = 1.0 / Math.sqrt(len2);
                dirX = dx * invLen;
                dirZ = dz * invLen;
            }

            if (e instanceof ServerPlayer otherSp) {
                // Use vanilla knockback for horizontal sync (players honor this)
                // knockback() subtracts the normalized vector * strength; to push away from the player,
                // pass (player - target) on XZ:
                otherSp.knockback((float) hs, player.getX() - otherSp.getX(), player.getZ() - otherSp.getZ());

                // Now set the vertical launch to our slam height
                var vel = otherSp.getDeltaMovement();
                otherSp.setDeltaMovement(vel.x, vy, vel.z);
                otherSp.hasImpulse = true;

                // Force velocity sync to the player's client
                otherSp.connection.send(new ClientboundSetEntityMotionPacket(otherSp));
            } else {
                // Non-player entities: direct impulse works fine
                var old = e.getDeltaMovement();
                e.setDeltaMovement(old.x + dirX * hs, vy, old.z + dirZ * hs);
                e.hasImpulse = true;
            }
        }

        // Start visual cooldown on the held instance
        ItemStack main = player.getMainHandItem();
        ItemStack off  = player.getOffhandItem();
        if (main.getItem() instanceof RavagerWrecker) {
            player.getCooldowns().addCooldown(main.getItem(), rw.cooldownTicks());
        } else if (off.getItem() instanceof RavagerWrecker) {
            player.getCooldowns().addCooldown(off.getItem(),  rw.cooldownTicks());
        }
    }

    // =======================
    // Passive slowness while held
    // =======================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        RavagerWrecker rw = getHeld(player);

        if (rw != null && rw.applyHoldSlowness) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    rw.holdSlownessRefreshTicks,
                    PASSIVE_SLOW_AMP,
                    true,  // ambient
                    false
            ));
        } else {
            var eff = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (eff != null && eff.isAmbient() && eff.getAmplifier() == PASSIVE_SLOW_AMP) {
                player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }
    }

    // =======================
    // Knockback scaler (your fixed version)
    // =======================
    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();

        LivingEntity attacker = target.getLastHurtByMob();
        if (attacker == null) return;

        RavagerWrecker rw = null;
        if (attacker.getMainHandItem().getItem() instanceof RavagerWrecker r1) rw = r1;
        else if (attacker.getOffhandItem().getItem() instanceof RavagerWrecker r2) rw = r2;
        if (rw == null) return;

        float scaled = event.getStrength() * rw.knockbackMultiplier;
        float finalStrength = Math.max(rw.minBaseKnockback, scaled);
        event.setStrength(finalStrength);
        // keep vanilla direction
    }
}
