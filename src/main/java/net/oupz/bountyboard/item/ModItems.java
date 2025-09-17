package net.oupz.bountyboard.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.block.ModBlocks;
import net.oupz.bountyboard.item.custom.PlushBlockItem;
import net.oupz.bountyboard.item.weapons.HeadhuntersHatchet;
import net.oupz.bountyboard.item.weapons.PhantomReaver;
import net.oupz.bountyboard.item.weapons.RavagerWrecker;

import java.util.List;


public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BountyBoard.MOD_ID);

    public static final RegistryObject<Item> BOUNTY_TOKEN = ITEMS.register("bounty_token",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLUSH_BLOCK_ITEM_OUPZ = ITEMS.register("plush_block_oupz",
            () -> new PlushBlockItem(ModBlocks.PLUSH_BLOCK_OUPZ.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PLUSH_BLOCK_ITEM_KRIZTEN = ITEMS.register("plush_block_krizten",
            () -> new PlushBlockItem(ModBlocks.PLUSH_BLOCK_KRIZTEN.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PLUSH_BLOCK_ITEM_TFOLK = ITEMS.register("plush_block_tfolk",
            () -> new PlushBlockItem(ModBlocks.PLUSH_BLOCK_TFOLK.get(), new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> HEADHUNTERS_HATCHET = ITEMS.register("headhunters_hatchet",
            () -> new HeadhuntersHatchet(ModToolTiers.HEADHUNTER_TIER, new Item.Properties()
                    .attributes(HeadhuntersHatchet.createAttributes(ModToolTiers.HEADHUNTER_TIER, 3.0f, 1.6f))) {

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
                    .attributes(AxeItem.createAttributes(ModToolTiers.HEADHUNTER_TIER, 1.0f, 3f))) {

                @Override
                public Component getName(ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack)).withStyle(ChatFormatting.BLUE);
                }

                @Override
                public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
                    if(Screen.hasShiftDown()) {
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.phantom_reaver.shift_down"));
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.phantom_reaver.shift_down1"));
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.phantom_reaver.shift_down2"));
                    } else {
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.phantom_reaver"));
                    }
                    super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
                }
            }
    );

    public static final RegistryObject<Item> RAVAGER_WRECKER = ITEMS.register("ravager_wrecker",
            () -> new RavagerWrecker(ModToolTiers.HEADHUNTER_TIER, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModToolTiers.HEADHUNTER_TIER, 6.0f, -3.35f))) {

                @Override
                public Component getName(ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack)).withStyle(ChatFormatting.GOLD);
                }

                @Override
                public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
                    if(Screen.hasShiftDown()) {
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.ravager_wrecker.shift_down"));
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.ravager_wrecker.shift_down1"));
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.ravager_wrecker.shift_down2"));
                    } else {
                        pTooltipComponents.add(Component.translatable("tooltip.bountyboard.ravager_wrecker"));
                    }
                    super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
                }
            }
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}