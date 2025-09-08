package net.oupz.bountyboard.registry;

import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.item.BountyBoardItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(ForgeRegistries.ITEMS, BountyBoard.MOD_ID);

    public static final RegistryObject<Item> BOUNTY_BOARD_ITEM = ITEMS.register("bounty_board",
            () -> new BountyBoardItem(BlockRegistry.BOUNTY_BOARD.get(), new Item.Properties()));
    
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}