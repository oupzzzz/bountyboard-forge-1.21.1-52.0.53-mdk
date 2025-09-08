package net.oupz.bountyboard.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.block.ModBlocks;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, BountyBoard.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        logBlock(ModBlocks.BOUNTY_BOX_1.get());
        logBlock(ModBlocks.BOUNTY_BOX_2.get());
        logBlock(ModBlocks.BOUNTY_BOX_3.get());

        blockItem(ModBlocks.BOUNTY_BOX_1);
        blockItem(ModBlocks.BOUNTY_BOX_2);
        blockItem(ModBlocks.BOUNTY_BOX_3);

        /*blockWithItemBottomTop(ModBlocks.BOUNTY_BOX_1);
        blockWithItemBottomTop(ModBlocks.BOUNTY_BOX_2);
        blockWithItemBottomTop(ModBlocks.BOUNTY_BOX_3);*/

        //blockAll(ModBlocks.BLOCK_ID);
    }


    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }

    private void blockItem(RegistryObject<? extends Block> blockRegistryObject) {
        simpleBlockItem(blockRegistryObject.get(), new ModelFile.UncheckedModelFile("bountyboard:block/" +
                ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get()).getPath()));
    }

    private void blockItem(RegistryObject<? extends Block> blockRegistryObject, String appendix) {
        simpleBlockItem(blockRegistryObject.get(), new ModelFile.UncheckedModelFile("bountyboard:block/" +
                ForgeRegistries.BLOCKS.getKey(blockRegistryObject.get()).getPath()));
    }
}
