package net.oupz.bountyboard.item.weapons;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.oupz.bountyboard.effect.ModEffects;
import net.oupz.bountyboard.util.ModEvents;

import java.util.List;

public class PhantomReaver extends SwordItem {


    public PhantomReaver(Tier pTier, Properties pProperties) {
        super(pTier, pProperties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && attacker instanceof Player) {
            if (attacker.getRandom().nextFloat() < 0.10f) {
                target.addEffect(new MobEffectInstance(
                        (Holder<MobEffect>) ModEffects.SPECTRAL_MARK_EFFECT.get(),
                        /* duration in ticks */ 6000,
                        /* amplifier */ 0,
                        /* ambient */ false,
                        /* showParticles */ true
                ));
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    // 2) Right‑click skill: consume marked entity → Spectator for 3s + smoke
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // find any marked living entity within 7 blocks
            List<LivingEntity> marked = world.getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(7),
                    e -> e.hasEffect((Holder<MobEffect>) ModEffects.SPECTRAL_MARK_EFFECT.get())
            );

            if (!marked.isEmpty()) {
                // remove the mark
                LivingEntity target = marked.get(0);
                target.removeEffect((Holder<MobEffect>) ModEffects.SPECTRAL_MARK_EFFECT.get());

                // stash previous gamemode in NBT
                CompoundTag data = serverPlayer.getPersistentData();
                data.putString(
                        "phantomReaverPrevMode",
                        serverPlayer.gameMode.getGameModeForPlayer().getName()
                );

                // switch into spectator and start 3s timer (60 ticks)
                serverPlayer.setGameMode(GameType.SPECTATOR);
                ModEvents.addSpectatorTimer(serverPlayer.getUUID(), 60);

                return InteractionResultHolder.success(player.getItemInHand(hand));
            }
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
