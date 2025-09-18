package net.oupz.bountyboard.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.bounty.renown.RenownHelper;
import net.oupz.bountyboard.client.ClientDailyStatus;
import net.oupz.bountyboard.client.ClientRenown;
import net.oupz.bountyboard.client.ClientResetClock;
import net.oupz.bountyboard.client.ClientRewards;
import net.oupz.bountyboard.init.ModNetworking;
import net.oupz.bountyboard.net.AcceptBountyC2S;
import net.oupz.bountyboard.util.RenderUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class BountyBoardScreen extends AbstractContainerScreen<BountyBoardMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "textures/gui/bounty_board.png");
    private static final ResourceLocation BUTTON_TOP_UNSELECTED = ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "textures/gui/button_top_unselected.png");
    private static final ResourceLocation BUTTON_TOP_SELECTED = ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "textures/gui/button_top_selected.png");

    // Use vanilla Minecraft scroller sprites like in MerchantScreen
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "textures/gui/custom_scroller.png");
    private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "textures/gui/custom_scroller_disabled.png");

    private static final ResourceLocation REWARD_BADGE_TEX =
            ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "textures/gui/reward_badge.png");
    private static final ResourceLocation REWARD_BADGE_TEX_HOVER =
            ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "textures/gui/reward_badge_hover.png");

    private static final int REWARD_BADGE_W = 16; // matches your PNG
    private static final int REWARD_BADGE_H = 16;

    private int rewardBadgeX = 0;
    private int rewardBadgeY = 0;

    private boolean isOverRewardBadge(int mx, int my) {
        return mx >= rewardBadgeX && mx < rewardBadgeX + REWARD_BADGE_W
                && my >= rewardBadgeY && my < rewardBadgeY + REWARD_BADGE_H;
    }


    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 188;

    // List view area coordinates (relative to container texture)
    private static final int LIST_START_X = 27;
    private static final int LIST_START_Y = 51;
    private static final int LIST_WIDTH = 195;
    private static final int LIST_HEIGHT = 100;
    private static final int LIST_END_X = LIST_START_X + LIST_WIDTH;
    private static final int LIST_END_Y = LIST_START_Y + LIST_HEIGHT;

    // Scroll bar coordinates (relative to container texture)
    private static final int SCROLL_BAR_X = 223;
    private static final int SCROLL_BAR_Y = LIST_START_Y;
    private static final int SCROLL_BAR_WIDTH = 6;
    private static final int SCROLL_BAR_HEIGHT = LIST_HEIGHT;

    // List item constants - increased padding for better spacing
    private static final int ITEM_HEIGHT = 36; // Increased from 32 for more padding
    private static final int ITEM_PADDING = 4; // Padding between items
    private static final int ITEM_CONTENT_HEIGHT = ITEM_HEIGHT - ITEM_PADDING; // Actual content area
    private static final int MAX_VISIBLE_ITEMS = 3; // About 3 items fit in 100px height

    // Bottom buttons positioning (relative to container texture)
    private static final int BOTTOM_BUTTONS_Y = 160; // Height within container (160 out of 188)
    private static final int BUTTON_HEIGHT = 20;
    private static final int TIER_BUTTON_WIDTH = 50;
    private static final int ACCEPT_BUTTON_WIDTH = 30;

    // === Daily status (populated by a small S2C packet) ===
    private static final int DAILY_LIMIT = 5;
    // These are updated by DailyStatusS2C::handle on the client.
    public static volatile int LAST_COMPLETED = 0;
    public static volatile long LAST_SECONDS_TO_RESET = 0L;

    private final Map<ResourceLocation, Integer> acceptedRewardSnapshots = new HashMap<>();

    private static volatile boolean REFRESH_PING = false; // set true by DailyStatusS2C when new data arrives

    public static void notifyDailyStatusRefreshed() {
        REFRESH_PING = true;
    }

    private boolean awaitingDailyRefresh = false;
    private long lastDailyPollMs = 0L;

    // View modes
    private enum ViewMode {
        BOUNTIES,
        PLAYER
    }

    private ViewMode currentView = ViewMode.BOUNTIES;
    private int selectedTier = 0; // 0 = Tier I, 1 = Tier II, 2 = Tier III
    private final List<BountyData> currentBounties = new ArrayList<>();
    private final List<PlayerHistoryEntry> playerHistory = new ArrayList<>();
    private final List<WantedPlayer> wantedPlayers = new ArrayList<>();

    // Scrolling - now supports fluid scrolling
    private float scrollOffset = 0.0f; // Changed to float for smooth scrolling
    private boolean isDraggingScrollbar = false;

    // Button references
    private CustomViewButton bountiesViewButton;
    private CustomViewButton playerViewButton;
    private Button tier1Button;
    private Button tier2Button;
    private Button tier3Button;
    private CustomTickButton acceptButton;
    private int selectedBountyIndex = -1;

    private static final ResourceLocation REWARD_ICON =
            ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, "textures/gui/reward_badge.png"); // 16x16 png
    // clickable bounds for the icon (computed each frame)
    private int claimIconX = 0, claimIconY = 0, claimIconW = 16, claimIconH = 16;

    public BountyBoardScreen(BountyBoardMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;
        this.inventoryLabelY = -20; // Move inventory label off screen

        // Initialize with temporary data
        initializeTempData();
    }

    private static String formatDdHhMm(long totalSeconds) {
        long s = Math.max(0L, totalSeconds);
        long days = s / 86400;
        long hours = (s % 86400) / 3600;
        long mins = (s % 3600) / 60;
        return String.format("%02d Day(s) %02d Hour(s) %02d Minute(s)", days, hours, mins);
    }

    @Override
    protected void init() {
        super.init();

        int containerX = this.leftPos;
        int containerY = this.topPos;

        // Top view selection buttons (positioned relative to container)
        bountiesViewButton = addRenderableWidget(new CustomViewButton(
                containerX + 28, containerY, 100, 29,
                Component.literal("Bounties"),
                button -> setView(ViewMode.BOUNTIES),
                BUTTON_TOP_SELECTED, BUTTON_TOP_UNSELECTED,
                currentView == ViewMode.BOUNTIES, this));

        playerViewButton = addRenderableWidget(new CustomViewButton(
                containerX + 129, containerY, 100, 29,
                Component.literal("Player"),
                button -> setView(ViewMode.PLAYER),
                BUTTON_TOP_SELECTED, BUTTON_TOP_UNSELECTED,
                currentView == ViewMode.PLAYER, this));

        // Bottom buttons - positioned relative to container texture coordinates
        // Start from LIST_START_X and space evenly across LIST_WIDTH
        int buttonStartX = containerX + LIST_START_X;
        int totalButtonWidth = 3 * TIER_BUTTON_WIDTH + ACCEPT_BUTTON_WIDTH;
        int totalSpacing = LIST_WIDTH - totalButtonWidth;
        int buttonSpacing = totalSpacing / 4; // 4 gaps (before each button)

        tier1Button = addRenderableWidget(Button.builder(
                        Component.literal("Tier I"),
                        button -> selectTier(0))
                .bounds(buttonStartX + buttonSpacing, containerY + BOTTOM_BUTTONS_Y, TIER_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        tier2Button = addRenderableWidget(Button.builder(
                        Component.literal("Tier II"),
                        button -> selectTier(1))
                .bounds(buttonStartX + buttonSpacing + TIER_BUTTON_WIDTH + buttonSpacing,
                        containerY + BOTTOM_BUTTONS_Y, TIER_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        tier3Button = addRenderableWidget(Button.builder(
                        Component.literal("Tier III"),
                        button -> selectTier(2))
                .bounds(buttonStartX + buttonSpacing + 2 * (TIER_BUTTON_WIDTH + buttonSpacing),
                        containerY + BOTTOM_BUTTONS_Y, TIER_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        // Accept button (aligned with tier buttons)
        acceptButton = addRenderableWidget(new CustomTickButton(
                buttonStartX + buttonSpacing + 3 * (TIER_BUTTON_WIDTH + buttonSpacing),
                containerY + BOTTOM_BUTTONS_Y, ACCEPT_BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.literal("✓"),
                button -> acceptSelectedBounty(),
                this));

        net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                new net.oupz.bountyboard.net.RequestBiweeklyResetEpochC2S(),
                net.minecraftforge.network.PacketDistributor.SERVER.noArg()
        );

        updateButtonVisibility();
        updateButtonStates();
    }

    // Custom button class for view selection
    private static class CustomViewButton extends Button {
        private final ResourceLocation selectedTexture;
        private final ResourceLocation unselectedTexture;
        private boolean isSelected;
        private final BountyBoardScreen parent;

        public CustomViewButton(int x, int y, int width, int height, Component message, OnPress onPress,
                                ResourceLocation selectedTexture, ResourceLocation unselectedTexture,
                                boolean isSelected, BountyBoardScreen parent) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.selectedTexture = selectedTexture;
            this.unselectedTexture = unselectedTexture;
            this.isSelected = isSelected;
            this.parent = parent;
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            ResourceLocation texture = isSelected ? selectedTexture : unselectedTexture;

            // Render button background
            graphics.blit(texture, getX(), getY(), 0, 0, width, height, 100, 29);

            // Render text
            int textColor = isSelected ? 0xFFFFFF : 0xEFEFEF;
            graphics.drawCenteredString(parent.font, getMessage(),
                    getX() + width / 2, getY() + (height - 8) / 2, textColor);
        }
    }

    // Custom button class for accept/confirm actions
    private static class CustomTickButton extends Button {
        private final BountyBoardScreen parent;

        public CustomTickButton(int x, int y, int width, int height, Component message, OnPress onPress, BountyBoardScreen parent) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.parent = parent;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Draw button background
            int bgColor = this.isHovered() ? 0xFF4CAF50 : 0xFF388E3C; // Green colors
            if (!this.active) {
                bgColor = 0xFF888888; // “disabled gray”
            } else if (this.isHovered()) {
                bgColor = 0xFF4CAF50; // bright green when hovered
            } else {
                bgColor = 0xFF388E3C; // normal green
            }
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

            // Draw border (always black)
            graphics.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF000000);
            graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFF000000);
            graphics.fill(getX(), getY(), getX() + 1, getY() + height, 0xFF000000);
            graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, 0xFF000000);

            // Draw the tick symbol. If inactive, you can also dim the text color if you like:
            int textColor = this.active ? 0xFFFFFF : 0xDDDDDD;
            graphics.drawCenteredString(parent.font, "✓",
                    getX() + width / 2, getY() + (height - 8) / 2, textColor);
        }
    }

    private static String formatHms(long totalSeconds) {
        long s = Math.max(0L, totalSeconds);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02d:%02d:%02d", h, m, sec);
    }

    private void setView(ViewMode view) {
        this.currentView = view;
        this.scrollOffset = 0.0f;
        this.selectedBountyIndex = -1;

        // Update button selected states
        bountiesViewButton.setSelected(currentView == ViewMode.BOUNTIES);
        playerViewButton.setSelected(currentView == ViewMode.PLAYER);

        updateButtonVisibility();

        // NEW: when entering the Player tab, ask the server for the next reset epoch
        if (currentView == ViewMode.PLAYER) {
            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new net.oupz.bountyboard.net.RequestBiweeklyResetEpochC2S(),
                    net.minecraftforge.network.PacketDistributor.SERVER.noArg()
            );
        }
    }

    private void selectTier(int tier) {
        this.selectedTier = tier;
        this.scrollOffset = 0.0f;
        this.selectedBountyIndex = -1;

        updateButtonStates();
        updateButtonVisibility();

        // TODO: Send packet to server to request bounties for this tier
        // PacketHandler.sendToServer(new RequestBountiesPacket(tier));
    }

    private void updateButtonVisibility() {
        boolean showTierButtons = (currentView == ViewMode.BOUNTIES);
        tier1Button.visible = showTierButtons;
        tier2Button.visible = showTierButtons;
        tier3Button.visible = showTierButtons;

        // Always show the accept button in BOUNTIES view...
        acceptButton.visible = showTierButtons;

        // ...but disable if nothing selected OR the selected bounty is already completed today
        boolean alreadyCompleted = false;
        if (showTierButtons && selectedBountyIndex >= 0 && selectedBountyIndex < currentBounties.size()) {
            net.minecraft.resources.ResourceLocation rl = currentBounties.get(selectedBountyIndex).id;
            alreadyCompleted = net.oupz.bountyboard.client.ClientDailyStatus.isCompletedToday(rl.toString());
        }

        acceptButton.active = showTierButtons && (selectedBountyIndex >= 0) && !alreadyCompleted;
    }


    private void updateButtonStates() {
        // Update tier button states
        tier1Button.active = selectedTier != 0;
        tier2Button.active = selectedTier != 1;
        tier3Button.active = selectedTier != 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.pose().pushPose();
        RenderUtil.bind(TEXTURE);
        RenderUtil.blitWithBlend(graphics.pose(), x, y, 0, 0, 256, 188, 256, 188, 0, 1);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw the title at the correct position
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // --- Top row info (outside the scissor) ---
        if (currentView == ViewMode.BOUNTIES) {
            final int infoLeftX  = this.leftPos + LIST_START_X;
            final int infoY      = this.topPos + 36;

            final int  completed = ClientDailyStatus.completedToday();
            final long secs      = ClientDailyStatus.remainingSeconds();

            // Left: "Bounties: x/5"
            final String leftText = "Bounties: " + completed + "/" + DAILY_LIMIT;
            graphics.drawString(this.font, leftText, infoLeftX, infoY, 0xFFFFFF, false);

            // Right: "Reset: hh:mm:ss"
            final int scrollBarLeft  = this.leftPos + SCROLL_BAR_X;
            final String rightText   = "Reset: " + formatHms(secs);
            final int rightTextW     = this.font.width(rightText);
            final int rightTextX     = scrollBarLeft - 6 - rightTextW;
            graphics.drawString(this.font, rightText, rightTextX, infoY, 0xAAAAAA, false);

            // ===== Reward claim icon =====
            final int scrollBarRight = scrollBarLeft + SCROLL_BAR_WIDTH;
            final int gapAfterBar = 6;
            claimIconW = 16; claimIconH = 16;
            claimIconX = scrollBarRight + gapAfterBar;
            claimIconY = infoY - 3;

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, REWARD_ICON);
            graphics.blit(REWARD_ICON, claimIconX, claimIconY, 0, 0, claimIconW, claimIconH, claimIconW, claimIconH);

            int pending = ClientRewards.pendingCount();
            if (pending > 0) {
                int badgeW = Math.max(10, this.font.width(String.valueOf(pending)) + 6);
                int badgeH = 9;
                int bx = claimIconX + claimIconW - badgeW + 2;
                int by = claimIconY - 2;

                graphics.fill(bx - 1, by - 1, bx + badgeW + 1, by + badgeH + 1, 0xFF000000);
                graphics.fill(bx, by, bx + badgeW, by + badgeH, 0xFFE0A000);
                graphics.drawCenteredString(this.font, String.valueOf(pending), bx + badgeW / 2, by + 1, 0xFFFFFFFF);
            }

            long nowMs = System.currentTimeMillis();
            if (secs <= 0) {
                if (!awaitingDailyRefresh || (nowMs - lastDailyPollMs) > 2000L) {
                    awaitingDailyRefresh = true;
                    lastDailyPollMs = nowMs;
                    net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                            new net.oupz.bountyboard.net.RequestDailyStatusC2S(),
                            net.minecraftforge.network.PacketDistributor.SERVER.noArg()
                    );
                }
            }
            if (REFRESH_PING) {
                REFRESH_PING = false;
                awaitingDailyRefresh = false;
            }
        } else if (currentView == ViewMode.PLAYER) {
            // Player tab: Biweekly reset countdown DD:HH:MM (outside scissor, same row)
            long secs = ClientResetClock.secondsRemaining();
            String ddhhmm = formatDdHhMm(secs);

            final int infoY = this.topPos + 36;
            int centerX = this.leftPos + (this.imageWidth / 2);
            int textWidth = this.font.width(ddhhmm);
            int textX = centerX - (textWidth / 2);

            graphics.drawString(this.font, ddhhmm, textX, infoY, 0xAAAAAA, false);
        }

        // List scissor + content
        graphics.enableScissor(
                this.leftPos + LIST_START_X,
                this.topPos + LIST_START_Y,
                this.leftPos + LIST_END_X,
                this.topPos + LIST_END_Y
        );

        if (currentView == ViewMode.BOUNTIES) {
            renderBountiesView(graphics, this.leftPos, this.topPos, mouseX, mouseY);
        } else {
            renderPlayerView(graphics, this.leftPos, this.topPos, mouseX, mouseY);
        }

        graphics.disableScissor();

        // Scrollbar and row-tooltips
        renderScrollbar(graphics, this.leftPos, this.topPos);
        renderTooltip(graphics, mouseX, mouseY);

        if (currentView == ViewMode.BOUNTIES &&
                mouseX >= claimIconX && mouseX < claimIconX + claimIconW &&
                mouseY >= claimIconY && mouseY < claimIconY + claimIconH) {

            int pending = ClientRewards.pendingCount();
            java.util.List<net.minecraft.network.chat.Component> tip = java.util.List.of(
                    net.minecraft.network.chat.Component.literal(
                            pending > 0 ? ("Click to claim " + pending + " reward(s)") : "No rewards to claim"
                    )
            );

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            graphics.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    private void renderBountiesView(GuiGraphics graphics, int containerX, int containerY, int mouseX, int mouseY) {
        // Calculate which items to render based on fluid scroll offset
        float itemsFromTop = scrollOffset;
        int firstVisibleIndex = (int) Math.floor(itemsFromTop);
        float partialOffset = itemsFromTop - firstVisibleIndex;

        // Render items that are visible (including partially visible ones)
        for (int i = firstVisibleIndex; i < currentBounties.size(); i++) {
            int relativeIndex = i - firstVisibleIndex;
            float itemY = containerY + LIST_START_Y + (relativeIndex * ITEM_HEIGHT) - (partialOffset * ITEM_HEIGHT);

            // Stop rendering if we're past the visible area
            if (itemY > containerY + LIST_END_Y) {
                break;
            }

            // Only render if at least partially visible
            if (itemY + ITEM_CONTENT_HEIGHT > containerY + LIST_START_Y) {
                boolean isHovered = mouseX >= containerX + LIST_START_X && mouseX <= containerX + LIST_END_X &&
                        mouseY >= itemY && mouseY <= itemY + ITEM_CONTENT_HEIGHT;
                boolean isSelected = i == selectedBountyIndex;

                renderBountyEntry(graphics, currentBounties.get(i), containerX + LIST_START_X, (int) itemY, isSelected, isHovered);
            }
        }
    }

    private void renderPlayerView(GuiGraphics graphics, int containerX, int containerY, int mouseX, int mouseY) {
        // --- Player's current renown ---
        int playerRenown = getPlayerRenown();
        graphics.drawString(font, "Your Renown: " + playerRenown,
                containerX + LIST_START_X + 5, containerY + LIST_START_Y + 5, 0xCCCCCC, false);
    }


    private void renderBountyEntry(GuiGraphics graphics, BountyData bounty, int x, int y, boolean isSelected, boolean isHovered) {
        boolean alreadyCompleted = net.oupz.bountyboard.client.ClientDailyStatus.isCompletedToday(bounty.id.toString());

        // Row background/highlight
        if (alreadyCompleted) {
            graphics.fill(x, y, x + LIST_WIDTH, y + ITEM_CONTENT_HEIGHT, 0x55000000);
        } else if (isSelected) {
            graphics.fill(x, y, x + LIST_WIDTH, y + ITEM_CONTENT_HEIGHT, 0xCC00AA00);
        } else if (isHovered) {
            graphics.fill(x, y, x + LIST_WIDTH, y + ITEM_CONTENT_HEIGHT, 0x40000000);
        }

        final int mainTextColor   = alreadyCompleted ? 0xFFAAAAAA : 0xFFFFFFFF;
        final int rewardTextColor = alreadyCompleted ? 0xFF888888 : 0xFFFFD700;

        // Title/description
        graphics.drawString(font,
                alreadyCompleted ? (bounty.description + " (completed)") : bounty.description,
                x + 5, y + 3, mainTextColor, false);

        // Decide which reward to show:
        // - If it's completed today AND we have a snapshot from when it was accepted, show that fixed value.
        // - Else show the dynamic reward for the currently selected tier.
        int displayReward;
        Integer completedSnap = net.oupz.bountyboard.client.ClientCompletedSnapshots.get(bounty.id);
        if (completedSnap != null) {
            // Completed: show the fixed snapshot received from server
            displayReward = completedSnap;
        } else {
            // Not completed (or no snapshot yet): show dynamic reward
            displayReward = bounty.getScaledReward(selectedTier);
        }

        graphics.drawString(font,
                "Reward: " + displayReward + " renown",
                x + 5, y + 13, rewardTextColor, false);
    }


    private void renderHistoryEntry(GuiGraphics graphics, PlayerHistoryEntry entry, int x, int y, boolean isHovered) {
        // Draw background highlight with better contrast
        if (isHovered) {
            // Subtle dark highlight that improves text readability
            graphics.fill(x, y, x + LIST_WIDTH, y + ITEM_CONTENT_HEIGHT, 0x40000000);
        }

        // Draw history info with proper spacing and better text colors
        int mainTextColor = isHovered ? 0xFFFFFF : 0xFFFFFF;
        int dateTextColor = isHovered ? 0xF0F0F0 : 0xCCCCCC;
        int rewardTextColor = isHovered ? 0xFFD700 : 0xFFD700;

        graphics.drawString(font, entry.bountyName, x + 5, y + 3, mainTextColor, false);
        graphics.drawString(font, "Completed: " + entry.dateCompleted, x + 5, y + 13, dateTextColor, false);
        graphics.drawString(font, "Earned: " + entry.renownEarned + " renown", x + 5, y + 23, rewardTextColor, false);
    }

    private void renderScrollbar(GuiGraphics graphics, int containerX, int containerY) {
        int totalItems = (currentView == ViewMode.BOUNTIES) ? currentBounties.size() : playerHistory.size();
        float maxVisibleItems = (currentView == ViewMode.PLAYER)
                ? (LIST_HEIGHT - 32) / (float) ITEM_HEIGHT
                : LIST_HEIGHT / (float) ITEM_HEIGHT;

        if (totalItems > maxVisibleItems) {
            float scrollableItems = totalItems - maxVisibleItems;

            // Height of your custom scroller PNG
            int scrollerHeight = 27;
            int trackHeight   = SCROLL_BAR_HEIGHT - scrollerHeight;
            int scrollerPosition = 0;
            if (scrollableItems > 0) {
                scrollerPosition = Math.min(trackHeight,
                        Math.round(scrollOffset * trackHeight / scrollableItems));
            }

            // Bind your custom PNG, then draw it
            RenderSystem.setShaderTexture(0, SCROLLER_SPRITE);
            graphics.blit(
                    /* matrix */   SCROLLER_SPRITE,
                    /* x */        containerX + SCROLL_BAR_X,
                    /* y */        containerY + SCROLL_BAR_Y + scrollerPosition,
                    /* u */        0,
                    /* v */        0,
                    /* width */    SCROLL_BAR_WIDTH,
                    /* height */   scrollerHeight,
                    /* texWidth */ SCROLL_BAR_WIDTH,
                    /* texHeight */ scrollerHeight
            );
        } else {
            // When nothing to scroll, show your “disabled” PNG instead
            RenderSystem.setShaderTexture(0, SCROLLER_DISABLED_SPRITE);
            graphics.blit(
                    SCROLLER_DISABLED_SPRITE,
                    containerX + SCROLL_BAR_X,
                    containerY + SCROLL_BAR_Y,
                    0,
                    0,
                    SCROLL_BAR_WIDTH,
                    27,      // height of disabled sprite
                    SCROLL_BAR_WIDTH,
                    27
            );
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Check if mouse is over list area
        if (mouseX >= this.leftPos + LIST_START_X && mouseX <= this.leftPos + LIST_END_X &&
                mouseY >= this.topPos + LIST_START_Y && mouseY <= this.topPos + LIST_END_Y) {

            int totalItems = (currentView == ViewMode.BOUNTIES) ? currentBounties.size() : playerHistory.size();
            float maxVisibleItems = (currentView == ViewMode.PLAYER) ?
                    (LIST_HEIGHT - 32) / (float) ITEM_HEIGHT : // Account for header space in player view
                    LIST_HEIGHT / (float) ITEM_HEIGHT;

            if (scrollY > 0) {
                scrollOffset = Math.max(0.0f, scrollOffset - 0.5f);
            } else if (scrollY < 0) {
                scrollOffset = Math.min(Math.max(0.0f, totalItems - maxVisibleItems), scrollOffset + 0.5f);
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            int totalItems = (currentView == ViewMode.BOUNTIES) ? currentBounties.size() : playerHistory.size();
            float maxVisibleItems = (currentView == ViewMode.PLAYER) ?
                    (LIST_HEIGHT - 32) / (float) ITEM_HEIGHT : // Account for header space in player view
                    LIST_HEIGHT / (float) ITEM_HEIGHT;

            if (totalItems > maxVisibleItems) {
                float scrollableItems = totalItems - maxVisibleItems;
                int trackHeight = SCROLL_BAR_HEIGHT - 27; // 27 is scroller height

                float percentage = ((float) mouseY - (this.topPos + SCROLL_BAR_Y)) / trackHeight;
                percentage = Mth.clamp(percentage, 0.0F, 1.0F);

                scrollOffset = percentage * scrollableItems;
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        // Check scrollbar click
        if (mouseX >= this.leftPos + SCROLL_BAR_X && mouseX <= this.leftPos + SCROLL_BAR_X + SCROLL_BAR_WIDTH &&
                mouseY >= this.topPos + SCROLL_BAR_Y && mouseY <= this.topPos + SCROLL_BAR_Y + SCROLL_BAR_HEIGHT) {
            isDraggingScrollbar = true;
            return true;
        }

        // Check list item clicks (only for bounties view)
        if (currentView == ViewMode.BOUNTIES &&
                mouseX >= this.leftPos + LIST_START_X && mouseX <= this.leftPos + LIST_END_X &&
                mouseY >= this.topPos + LIST_START_Y && mouseY <= this.topPos + LIST_END_Y) {

            // Calculate which item was clicked with fluid scrolling
            float relativeY = (float) mouseY - (this.topPos + LIST_START_Y);
            int clickedIndex = (int) Math.floor((relativeY + scrollOffset * ITEM_HEIGHT) / ITEM_HEIGHT);

            if (clickedIndex < currentBounties.size()) {
                selectedBountyIndex = clickedIndex;
                updateButtonVisibility(); // Update accept button visibility
                return true;
            }
        }
        // Claim icon click
        if (mouseX >= claimIconX && mouseX < claimIconX + claimIconW &&
                mouseY >= claimIconY && mouseY < claimIconY + claimIconH) {
            if (ClientRewards.pendingCount() > 0) {
                // send a simple claim request; server will grant boxes and reply with updated counts
                net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                        new net.oupz.bountyboard.net.ClaimRewardsC2S(),
                        net.minecraftforge.network.PacketDistributor.SERVER.noArg()
                );
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void acceptSelectedBounty() {
        if (selectedBountyIndex >= 0 && selectedBountyIndex < currentBounties.size()) {
            BountyData bounty = currentBounties.get(selectedBountyIndex);

            // We already have a proper ResourceLocation on BountyData now
            net.minecraft.resources.ResourceLocation rl = bounty.id;

            // Compute and store the snapshot (base from RenownHelper with player UUID + tier multiplier)
            var mc = net.minecraft.client.Minecraft.getInstance();
            java.util.UUID pid = (mc.player != null) ? mc.player.getUUID() : new java.util.UUID(0L, 0L);
            int base  = net.oupz.bountyboard.bounty.renown.RenownHelper.getBaseRenown(rl, pid);
            float mult = net.oupz.bountyboard.bounty.renown.RenownHelper.getMultiplierForTier(selectedTier);
            int finalReward = Math.round(base * mult);
            acceptedRewardSnapshots.put(rl, finalReward);

            // Send the real accept packet to the server
            net.oupz.bountyboard.init.ModNetworking.CHANNEL.send(
                    new net.oupz.bountyboard.net.AcceptBountyC2S(rl, selectedTier),
                    net.minecraftforge.network.PacketDistributor.SERVER.noArg()
            );

            // Optional local log
            System.out.println("Accepted bounty: " + bounty.description + " (" + rl + ") snapshot=" + finalReward);

            // Reset selection after accepting
            selectedBountyIndex = -1;
            updateButtonVisibility();
        }
    }

    private void initializeTempData() {
        // Initialize bounties for current tier
        loadBaseBounties();

        // Initialize wanted players
        wantedPlayers.clear();
        wantedPlayers.add(new WantedPlayer("Steve", 1500));
        wantedPlayers.add(new WantedPlayer("Alex", 1200));
        wantedPlayers.add(new WantedPlayer("Notch", 900));
    }

    private void loadBaseBounties() {
        currentBounties.clear();

        // Only ids + descriptions here. Base renown comes from RenownHelper.
        currentBounties.add(new BountyData("base_1", "Pillager Patrol"));
        currentBounties.add(new BountyData("base_2", "Vindicator Hunt"));
        currentBounties.add(new BountyData("base_3", "Crossbow Confiscation"));
        currentBounties.add(new BountyData("base_4", "Scout Elimination"));
        currentBounties.add(new BountyData("base_5", "Banner Destruction"));
    }

    private int getPlayerRenown() {
        return ClientRenown.getTotal();
    }

    // Data classes
    private static class BountyData {
        ResourceLocation id;
        String description;

        // pass just the path like "base_3"; we’ll namespace with your mod id
        BountyData(String path, String description) {
            this.id = ResourceLocation.fromNamespaceAndPath(BountyBoard.MOD_ID, path);
            this.description = description;
        }

        int getScaledReward(int tier) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            java.util.UUID pid = (mc.player != null) ? mc.player.getUUID() : new java.util.UUID(0L, 0L);
            int base = net.oupz.bountyboard.bounty.renown.RenownHelper.getBaseRenown(id, pid);
            float mult = net.oupz.bountyboard.bounty.renown.RenownHelper.getMultiplierForTier(tier);
            return Math.round(base * mult);
        }
    }


    private static class WantedPlayer {
        String name;
        int renown;

        WantedPlayer(String name, int renown) {
            this.name = name;
            this.renown = renown;
        }
    }

    private static class PlayerHistoryEntry {
        String bountyName;
        String dateCompleted;
        int renownEarned;

        PlayerHistoryEntry(String bountyName, String dateCompleted, int renownEarned) {
            this.bountyName = bountyName;
            this.dateCompleted = dateCompleted;
            this.renownEarned = renownEarned;
        }
    }
}
