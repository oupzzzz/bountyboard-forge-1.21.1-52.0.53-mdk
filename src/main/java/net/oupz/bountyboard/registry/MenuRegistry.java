package net.oupz.bountyboard.registry;

import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.client.screen.BountyBoardMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = 
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, BountyBoard.MOD_ID);

    public static final RegistryObject<MenuType<BountyBoardMenu>> BOUNTY_BOARD_MENU =
        MENU_TYPES.register("bounty_board_menu",
            () -> IForgeMenuType.create(BountyBoardMenu::new));
    
    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}