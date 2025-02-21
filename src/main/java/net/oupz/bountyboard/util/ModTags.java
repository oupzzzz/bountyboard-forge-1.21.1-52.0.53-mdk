package net.oupz.bountyboard.util;

import jdk.javadoc.doclet.Taglet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.oupz.bountyboard.BountyBoard;

public class ModTags {
    public static class Blocks{
        public static final TagKey<Block> NEEDS_BOUNTYBOARD_TOOL = createTag("needs_bountyboard_tool");
        public static final TagKey<Block> INCORRECT_FOR_BOUNTYBOARD_TOOL = createTag("incorrect_for_bountyboard_tool");

        public static final TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> TRANSFORMABLE = null;
    }
}
