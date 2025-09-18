package net.oupz.bountyboard.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.block.custom.ModFlammableRotatedPillarBlock;
import net.oupz.bountyboard.block.custom.PlushBlock;
import net.oupz.bountyboard.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BountyBoard.MOD_ID);

    public static final RegistryObject<RotatedPillarBlock> BOUNTY_BOX_1 = registerBlock("bounty_box_1",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    public static final RegistryObject<RotatedPillarBlock> BOUNTY_BOX_2 = registerBlock("bounty_box_2",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    public static final RegistryObject<RotatedPillarBlock> BOUNTY_BOX_3 = registerBlock("bounty_box_3",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    public static final RegistryObject<RotatedPillarBlock> BOUNTY_BOX_4 = registerBlock("bounty_box_4",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    public static final RegistryObject<PlushBlock> PLUSH_BLOCK_OUPZ = BLOCKS.register("plush_block_oupz",
            () -> new PlushBlock(BlockBehaviour.Properties.of()
                    .strength(1.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistryObject<PlushBlock> PLUSH_BLOCK_KRIZTEN = BLOCKS.register("plush_block_krizten",
            () -> new PlushBlock(BlockBehaviour.Properties.of()
                    .strength(1.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistryObject<PlushBlock> PLUSH_BLOCK_TFOLK = BLOCKS.register("plush_block_tfolk",
            () -> new PlushBlock(BlockBehaviour.Properties.of()
                    .strength(1.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistryObject<PlushBlock> PLUSH_BLOCK_ITZJASTER = BLOCKS.register("plush_block_itzjaster",
            () -> new PlushBlock(BlockBehaviour.Properties.of()
                    .strength(1.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
