package net.oupz.bountyboard.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oupz.bountyboard.BountyBoard;

import java.util.List;
import java.util.logging.Level;


public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BountyBoard.MOD_ID);

    public static final RegistryObject<Item> BOUNTY_TOKEN = ITEMS.register("bounty_token",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> HEADHUNTERS_HATCHET = ITEMS.register("headhunters_hatchet",
            () -> new AxeItem(ModToolTiers.BOUNTYBOARD, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModToolTiers.BOUNTYBOARD, 5.0f, -3.0f))) {

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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}