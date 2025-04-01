package net.oupz.bountyboard.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.oupz.bountyboard.util.ModTags;

public class ModToolTiers {
    public static final Tier HEADHUNTER_TIER = new ForgeTier(2500, 12.0F, 6.0F, 50,
            ModTags.Blocks.NEEDS_HEADHUNTER_TOOL, () -> Ingredient.EMPTY,
            ModTags.Blocks.INCORRECT_FOR_HEADHUNTER_TOOL);

    public static final Tier PHANTOM_TIER = new ForgeTier(2500, 12.0F, 6.0F, 50,
            ModTags.Blocks.NEEDS_PHANTOM_TOOL, () -> Ingredient.EMPTY,
            ModTags.Blocks.INCORRECT_FOR_PHANTOM_TOOL);
}
