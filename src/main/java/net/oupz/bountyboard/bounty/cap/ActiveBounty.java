package net.oupz.bountyboard.bounty.cap;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

public class ActiveBounty {
    public enum State { NONE, ACCEPTED, SPAWNED, COMPLETED, FAILED }

    @Nullable
    private ResourceLocation bountyId;

    @Nullable
    private ResourceKey<Level> dimension;

    @Nullable
    private BlockPos anchorPos;

    private State state = State.NONE;

    @Nullable public ResourceLocation getBountyId() { return bountyId; }
    public void setBountyId(@Nullable ResourceLocation id) { this.bountyId = id; }

    @Nullable public ResourceKey<Level> getDimension() { return dimension; }
    public void setDimension(@Nullable ResourceKey<Level> dim) { this.dimension = dim; }

    @Nullable public BlockPos getAnchorPos() { return anchorPos; }
    public void setAnchorPos(@Nullable BlockPos pos) { this.anchorPos = pos; }

    public State getState() { return state; }
    public void setState(State s) { this.state = s; }

    private int tier = 0; // 0=Tier I, 1=Tier II, 2=Tier III
    private final List<UUID> spawnedMobIds = new ArrayList<>();

    public int getTier() { return tier; }
    public void setTier(int t) { this.tier = Math.max(0, Math.min(2, t)); }

    public List<UUID> getSpawnedMobIds() {return spawnedMobIds;}
    public void clearSpawnedMobIds() {spawnedMobIds.clear();}
    public void addSpawnedMobId(UUID id) {if (id != null) spawnedMobIds.add(id);}
    public void removeSpawnedMobId(UUID id) {spawnedMobIds.remove(id);}

    public void clear() {
        bountyId = null;
        dimension = null;
        anchorPos = null;
        state = State.NONE;
        tier = 0;
        spawnedMobIds.clear();
    }

    public boolean hasAccepted() {
        return state == State.ACCEPTED || state == State.SPAWNED;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (bountyId != null) tag.putString("BountyId", bountyId.toString());
        if (dimension != null) tag.putString("Dim", dimension.location().toString());
        if (anchorPos != null) {
            tag.putInt("Ax", anchorPos.getX());
            tag.putInt("Ay", anchorPos.getY());
            tag.putInt("Az", anchorPos.getZ());
        }
        tag.putString("State", state.name());
        tag.putInt("Tier", tier);

        var list = new net.minecraft.nbt.ListTag();
        for (UUID id : spawnedMobIds) {
            var t = new CompoundTag();
            t.putUUID("U", id);
            list.add(t);
        }
        tag.put("Spawned", list);
        return tag;

    }

    public void load(CompoundTag tag) {
        clear();
        if (tag.contains("BountyId")) bountyId = ResourceLocation.parse(tag.getString("BountyId"));
        if (tag.contains("Dim")) {
            dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dim")));
        }
        if (tag.contains("Ax")) {
            anchorPos = new BlockPos(tag.getInt("Ax"), tag.getInt("Ay"), tag.getInt("Az"));
        }
        try {
            state = State.valueOf(tag.getString("State"));
        } catch (Exception ignored) {
            state = State.NONE;
        }
        tier = tag.contains("Tier") ? Math.max(0, Math.min(2, tag.getInt("Tier"))) : 0;

        spawnedMobIds.clear();
        if (tag.contains("Spawned", Tag.TAG_LIST)) {
            var list = tag.getList("Spawned", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                var t = list.getCompound(i);
                if (t.hasUUID("U")) spawnedMobIds.add(t.getUUID("U"));
            }
        }
    }

    public void copyFrom(ActiveBounty other) {
        this.setBountyId(other.getBountyId());
        this.setDimension(other.getDimension());
        this.setAnchorPos(other.getAnchorPos());
        this.setTier(other.getTier());
        this.setState(other.getState());

        this.clearSpawnedMobIds();
        this.getSpawnedMobIds().addAll(other.getSpawnedMobIds());
    }

    public void resetAll() {
        setState(State.NONE);
        setBountyId(null);
        setAnchorPos(null);
        clearSpawnedMobIds();
        // if you store tier/dimension etc., clear as needed:
        setTier(0);
        setDimension(null);
    }
}