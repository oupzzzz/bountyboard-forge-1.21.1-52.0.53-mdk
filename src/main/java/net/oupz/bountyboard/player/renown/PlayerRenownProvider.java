package net.oupz.bountyboard.player.renown;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.oupz.bountyboard.BountyBoard; // your @Mod class with MODID

public class PlayerRenownProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final ResourceLocation KEY =
            ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "player_renown");

    public static final Capability<PlayerRenown> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>(){});

    private final PlayerRenownImpl backend = new PlayerRenownImpl();
    private final LazyOptional<PlayerRenown> optional = LazyOptional.of(() -> backend);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    // NEW signatures in 1.21.x:
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return backend.serializeNBT();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        backend.deserializeNBT(nbt);
    }
}

