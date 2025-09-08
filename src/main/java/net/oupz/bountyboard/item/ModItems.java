package net.oupz.bountyboard.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.block.ModBlocks;
import net.oupz.bountyboard.item.custom.PlushBlockItem;
import net.oupz.bountyboard.item.weapons.HeadhuntersHatchet;
import net.oupz.bountyboard.item.weapons.PhantomReaver;

import java.util.List;


public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BountyBoard.MOD_ID);

    public static final RegistryObject<Item> BOUNTY_TOKEN = ITEMS.register("bounty_token",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLUSH_BLOCK_ITEM = ITEMS.register("plush_block",
            () -> new PlushBlockItem(ModBlocks.PLUSH_BLOCK.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HEADHUNTERS_HATCHET = ITEMS.register("headhunters_hatchet",
            () -> new HeadhuntersHatchet(ModToolTiers.HEADHUNTER_TIER, new Item.Properties()
                    .attributes(HeadhuntersHatchet.createAttributes(ModToolTiers.HEADHUNTER_TIER, 5.0f, 1.6f))) {

                @Override
                public Component getName(ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack)).withStyle(ChatFormatting.DARK_RED);
                }

                @Override
                public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
                    if(Screen.hasShiftDown()) {
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.headhunters_hatchet.shift_down"));
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.headhunters_hatchet.shift_down1"));
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.headhunters_hatchet.shift_down2"));
                    } else {
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.headhunters_hatchet"));
                    }
                    super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
                }
            }
    );

    public static final RegistryObject<Item> PHANTOM_REAVER = ITEMS.register("phantom_reaver",
            () -> new PhantomReaver(ModToolTiers.HEADHUNTER_TIER, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModToolTiers.HEADHUNTER_TIER, 3.0f, -2.4f))) {

                @Override
                public Component getName(ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack)).withStyle(ChatFormatting.BLUE);
                }

                @Override
                public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
                    if(Screen.hasShiftDown()) {
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.phantom_reaver.shift_down"));
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.phantom_reaver.shift_down1"));
                    } else {
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.phantom_reaver"));
                    }
                    super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
                }
            }
    );

    public static final RegistryObject<Item> RAVAGER_WRECKER = ITEMS.register("ravager_wrecker",
            () -> new SwordItem(ModToolTiers.HEADHUNTER_TIER, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModToolTiers.HEADHUNTER_TIER, -3.0f, -2.4f))) {

                @Override
                public Component getName(ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack)).withStyle(ChatFormatting.GOLD);
                }

                @Override
                public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
                    if(Screen.hasShiftDown()) {
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.ravager_wrecker.shift_down"));
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.ravager_wrecker.shift_down1"));
                    } else {
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.ravager_wrecker"));
                    }
                    super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
                }

                @Override
                public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
                    if (!world.isClientSide) {
                        Vec3 pos = player.position();
                        AABB box = new AABB(
                                pos.x - 10, pos.y - 10, pos.z - 10,
                                pos.x + 10, pos.y + 10, pos.z + 10
                        );
                        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, box,
                                e -> e != player && e.distanceTo(player) <= 10
                        );
                        double v = Math.sqrt(2 * 0.08 * 15);
                        for (LivingEntity e : targets) {
                            Vec3 old = e.getDeltaMovement();
                            e.setDeltaMovement(new Vec3(old.x, v, old.z));
                            e.hasImpulse = true;
                        }
                        world.playSound((Entity) null,player.blockPosition(), (SoundEvent) SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1, 1);
                    }
                    return super.use(world, player, hand);
                }
            }
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}