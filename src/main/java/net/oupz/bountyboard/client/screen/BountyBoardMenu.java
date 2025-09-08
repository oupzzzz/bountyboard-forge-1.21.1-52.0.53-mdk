package net.oupz.bountyboard.client.screen;

import net.oupz.bountyboard.block.entity.BountyBoardBlockEntity;
import net.oupz.bountyboard.registry.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BountyBoardMenu extends AbstractContainerMenu {
    private final BountyBoardBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    // Client constructor
    public BountyBoardMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // Server constructor
    public BountyBoardMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(MenuRegistry.BOUNTY_BOARD_MENU.get(), containerId);
        this.blockEntity = (BountyBoardBlockEntity) blockEntity;
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        // --- SEND DAILY STATUS TO THIS PLAYER (server-side only) ---
        if (playerInventory.player instanceof net.minecraft.server.level.ServerPlayer sp) {
            // compute completed today from remaining()
            int completed = net.oupz.bountyboard.bounty.limits.DailyLimit.MAX_PER_DAY
                    - net.oupz.bountyboard.bounty.limits.DailyLimit.remaining(sp);

            // despite the name, this uses America/New_York in your class
            long seconds  = net.oupz.bountyboard.bounty.limits.DailyLimit.secondsUntilResetUtc();

            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new net.oupz.bountyboard.net.DailyStatusS2C(completed, seconds),
                    sp.connection.getConnection()
            );
        }

        // If you later add slots, keep them below this point
        // this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
        //     this.addSlot(new SlotItemHandler(handler, 0, 8, 142));
        // });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index == 36) {
                if (!this.moveItemStackTo(stack, 0, 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 36) {
                if (!this.moveItemStackTo(stack, 36, 37, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return itemstack;
    }

//    @Override
//    public boolean stillValid(Player player) {
//        return stillValid(this.levelAccess, player, BlockRegistry.BOUNTY_BOARD.get());
//    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return true;
    }

    public BountyBoardBlockEntity getBlockEntity() {
        return blockEntity;
    }
}