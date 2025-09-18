package net.oupz.bountyboard.item.custom;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.block.ModBlocks;
import net.oupz.bountyboard.item.ModItems;
import net.oupz.bountyboard.registry.BlockRegistry;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BountyBoard.MOD_ID);

    public static final RegistryObject<CreativeModeTab> BOUNTY_BOARD_TAB = CREATIVE_MODE_TABS.register("bounty_board_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.BOUNTY_TOKEN.get()))
                    //.withTabsBefore(TAB_NAME.getID())
                    .title(Component.translatable("creativetab.bountyboard.bounty_board"))
                    .displayItems((itemDIsplayParameters, output) -> {
                        output.accept(ModItems.BOUNTY_TOKEN.get());
                        output.accept(ModItems.HEADHUNTERS_HATCHET.get());
                        output.accept(ModItems.PHANTOM_REAVER.get());
                        output.accept(ModItems.RAVAGER_WRECKER.get());
                        output.accept(ModBlocks.BOUNTY_BOX_1.get());
                        output.accept(ModBlocks.BOUNTY_BOX_2.get());
                        output.accept(ModBlocks.BOUNTY_BOX_3.get());
                        output.accept(ModBlocks.BOUNTY_BOX_4.get());
                        output.accept(BlockRegistry.BOUNTY_BOARD.get());
                        output.accept(ModBlocks.PLUSH_BLOCK_OUPZ.get());
                        output.accept(ModBlocks.PLUSH_BLOCK_KRIZTEN.get());
                        output.accept(ModBlocks.PLUSH_BLOCK_TFOLK.get());
                        output.accept(ModBlocks.PLUSH_BLOCK_ITZJASTER.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
