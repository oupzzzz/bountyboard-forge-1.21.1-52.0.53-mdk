package net.oupz.bountyboard.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.oupz.bountyboard.util.ModTags;

public class ModToolTiers {
    public static final Tier BOUNTYBOARD = new ForgeTier(3000, 15.0F, 6.0F, 15,
            ModTags.Blocks.NEEDS_BOUNTYBOARD_TOOL, () -> Ingredient.of(ModItems.HEADHUNTERS_HATCHET.get()),
            ModTags.Blocks.INCORRECT_FOR_BOUNTYBOARD_TOOL);
}
