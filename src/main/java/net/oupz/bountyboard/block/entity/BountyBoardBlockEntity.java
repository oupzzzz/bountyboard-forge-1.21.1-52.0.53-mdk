package net.oupz.bountyboard.block.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.oupz.bountyboard.client.screen.BountyBoardMenu;
import net.oupz.bountyboard.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BountyBoardBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1); // For storing bounty tokens
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    // Bounty data storage
    private Map<UUID, ActiveBounty> activeBounties = new HashMap<>();
    private Map<UUID, Integer> playerRenown = new HashMap<>();
    private List<BountyTemplate> availableBounties = new ArrayList<>();
    private long lastRenownReset = System.currentTimeMillis();

    // Constants
    private static final long RENOWN_RESET_PERIOD = 14L * 24L * 60L * 60L * 1000L; // 2 weeks in milliseconds

    public BountyBoardBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.BOUNTY_BOARD_BE.get(), pos, state);
        initializeBountyTemplates();
    }

    public void handleAcceptBounty(ServerPlayer player, String bountyId, int tier) {
        // For now, just send a chat message back to the player and log to console:
        player.sendSystemMessage(
                Component.literal("Accepted bounty '" + bountyId + "' at tier " + (tier + 1))
        );
        System.out.println("[BountyBoard] Player " + player.getName().getString()
                + " accepted bountyId=" + bountyId + " with tier=" + tier);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.yourmod.bounty_board");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BountyBoardMenu(containerId, playerInventory, this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider p_327783_) {
        tag.put("inventory", itemHandler.serializeNBT(p_327783_));

        // Save active bounties
        CompoundTag bountiesTag = new CompoundTag();
        for (Map.Entry<UUID, ActiveBounty> entry : activeBounties.entrySet()) {
            CompoundTag bountyTag = new CompoundTag();
            entry.getValue().save(bountyTag);
            bountiesTag.put(entry.getKey().toString(), bountyTag);
        }
        tag.put("activeBounties", bountiesTag);

        // Save player renown
        CompoundTag renownTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : playerRenown.entrySet()) {
            renownTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("playerRenown", renownTag);

        tag.putLong("lastRenownReset", lastRenownReset);
        super.saveAdditional(tag, p_327783_);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider p_333170_) {
        super.loadAdditional(tag, p_333170_);
        itemHandler.deserializeNBT(p_333170_, tag.getCompound("inventory"));

        // Load active bounties
        activeBounties.clear();
        CompoundTag bountiesTag = tag.getCompound("activeBounties");
        for (String key : bountiesTag.getAllKeys()) {
            UUID playerId = UUID.fromString(key);
            ActiveBounty bounty = new ActiveBounty();
            bounty.load(bountiesTag.getCompound(key));
            activeBounties.put(playerId, bounty);
        }

        // Load player renown
        playerRenown.clear();
        CompoundTag renownTag = tag.getCompound("playerRenown");
        for (String key : renownTag.getAllKeys()) {
            UUID playerId = UUID.fromString(key);
            playerRenown.put(playerId, renownTag.getInt(key));
        }

        lastRenownReset = tag.getLong("lastRenownReset");

        // Check if renown should be reset
        checkRenownReset();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BountyBoardBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.checkRenownReset();
        }
    }

    private void checkRenownReset() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRenownReset >= RENOWN_RESET_PERIOD) {
            resetRenown();
            lastRenownReset = currentTime;
            setChanged();
        }
    }

    private void resetRenown() {
        playerRenown.clear();
    }

    public boolean acceptBounty(Player player, String bountyId, int tier) {
        UUID playerId = player.getUUID();

        // Check if player already has an active bounty
        if (activeBounties.containsKey(playerId)) {
            return false;
        }

        // Find the bounty template
        BountyTemplate template = findBountyTemplate(bountyId, tier);
        if (template == null) {
            return false;
        }

        // Create active bounty
        ActiveBounty activeBounty = new ActiveBounty(template, tier);
        activeBounties.put(playerId, activeBounty);

        setChanged();
        return true;
    }

    public boolean completeBounty(Player player) {
        UUID playerId = player.getUUID();
        ActiveBounty bounty = activeBounties.get(playerId);

        if (bounty == null || !bounty.isCompleted()) {
            return false;
        }

        // Award renown
        int currentRenown = playerRenown.getOrDefault(playerId, 0);
        playerRenown.put(playerId, currentRenown + bounty.getRenownReward());

        // Remove active bounty
        activeBounties.remove(playerId);

        setChanged();
        return true;
    }

    public List<Map.Entry<String, Integer>> getTopPlayers(int count) {
        List<Map.Entry<String, Integer>> topPlayers = new ArrayList<>();

        // Convert UUIDs to player names and sort by renown
        playerRenown.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(count)
                .forEach(entry -> {
                    String playerName = getPlayerName(entry.getKey());
                    topPlayers.add(new AbstractMap.SimpleEntry<>(playerName, entry.getValue()));
                });

        return topPlayers;
    }

    private String getPlayerName(UUID playerId) {
        // TODO: Implement proper player name lookup
        // This would typically involve checking online players or cached data
        return "Player_" + playerId.toString().substring(0, 8);
    }

    private BountyTemplate findBountyTemplate(String id, int tier) {
        return availableBounties.stream()
                .filter(b -> b.id.equals(id) && b.tier == tier)
                .findFirst()
                .orElse(null);
    }

    private void initializeBountyTemplates() {
        // Tier 1 bounties
        availableBounties.add(new BountyTemplate("tier1_1", "Pillager Patrol", 0, "minecraft:pillager", 5, 100));
        availableBounties.add(new BountyTemplate("tier1_2", "Vindicator Hunt", 0, "minecraft:vindicator", 3, 150));
        availableBounties.add(new BountyTemplate("tier1_3", "Crossbow Confiscation", 0, "minecraft:pillager", 10, 200));

        // Tier 2 bounties
        availableBounties.add(new BountyTemplate("tier2_1", "Raid Prevention", 1, "minecraft:pillager", 15, 300));
        availableBounties.add(new BountyTemplate("tier2_2", "Evoker Elimination", 1, "minecraft:evoker", 2, 400));
        availableBounties.add(new BountyTemplate("tier2_3", "Outpost Assault", 1, "minecraft:illager", 20, 500));

        // Tier 3 bounties
        availableBounties.add(new BountyTemplate("tier3_1", "Captain's Demise", 2, "minecraft:pillager", 5, 750));
        availableBounties.add(new BountyTemplate("tier3_2", "Mansion Massacre", 2, "minecraft:illager", 50, 1000));
        availableBounties.add(new BountyTemplate("tier3_3", "Illager Beast Hunt", 2, "minecraft:ravager", 3, 1200));
    }

    public List<BountyTemplate> getBountiesForTier(int tier) {
        return availableBounties.stream()
                .filter(b -> b.tier == tier)
                .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }

    public int getPlayerRenown(UUID playerId) {
        return playerRenown.getOrDefault(playerId, 0);
    }

    // Data classes
    public static class BountyTemplate {
        public String id;
        public String name;
        public int tier;
        public String targetEntity;
        public int targetCount;
        public int renownReward;

        public BountyTemplate(String id, String name, int tier, String targetEntity, int targetCount, int renownReward) {
            this.id = id;
            this.name = name;
            this.tier = tier;
            this.targetEntity = targetEntity;
            this.targetCount = targetCount;
            this.renownReward = renownReward;
        }
    }

    public static class ActiveBounty {
        private String templateId;
        private int tier;
        private int progress;
        private int targetCount;
        private int renownReward;

        public ActiveBounty() {}

        public ActiveBounty(BountyTemplate template, int tier) {
            this.templateId = template.id;
            this.tier = tier;
            this.progress = 0;
            this.targetCount = template.targetCount;
            this.renownReward = template.renownReward;
        }

        public void incrementProgress() {
            progress = Math.min(progress + 1, targetCount);
        }

        public boolean isCompleted() {
            return progress >= targetCount;
        }

        public int getRenownReward() {
            return renownReward;
        }

        public void save(CompoundTag tag) {
            tag.putString("templateId", templateId);
            tag.putInt("tier", tier);
            tag.putInt("progress", progress);
            tag.putInt("targetCount", targetCount);
            tag.putInt("renownReward", renownReward);
        }

        public void load(CompoundTag tag) {
            templateId = tag.getString("templateId");
            tier = tag.getInt("tier");
            progress = tag.getInt("progress");
            targetCount = tag.getInt("targetCount");
            renownReward = tag.getInt("renownReward");
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(2); // Adjust to fit your multiblock size
    }
}
