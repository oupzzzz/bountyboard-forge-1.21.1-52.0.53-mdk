package net.oupz.bountyboard.bounty.cap;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ActiveBountyProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final Capability<ActiveBounty> CAPABILITY =
            CapabilityManager.get(new CapabilityToken<>() {});

    private final ActiveBounty backend = new ActiveBounty();
    private final LazyOptional<ActiveBounty> optional = LazyOptional.of(() -> backend);

    // --- ICapabilityProvider ---
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == CAPABILITY ? optional.cast() : LazyOptional.empty();
    }

    // --- INBTSerializable ---
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
        return backend.save();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag nbt) {
        backend.load(nbt);
    }

    // Helper
    public static ActiveBounty get(Player player) {
        return player.getCapability(CAPABILITY).orElseThrow(
                () -> new IllegalStateException("ActiveBounty capability missing on player"));
    }
}
