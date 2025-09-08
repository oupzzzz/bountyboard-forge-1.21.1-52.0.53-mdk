package net.oupz.bountyboard.registry;

import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.block.entity.BountyBoardBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BountyBoard.MOD_ID);
    
    public static final RegistryObject<BlockEntityType<BountyBoardBlockEntity>> BOUNTY_BOARD_BE =
        BLOCK_ENTITIES.register("bounty_board", 
            () -> BlockEntityType.Builder.of(BountyBoardBlockEntity::new, BlockRegistry.BOUNTY_BOARD.get())
                .build(null));
    
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}