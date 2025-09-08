package net.oupz.bountyboard.block;

import net.oupz.bountyboard.BountyBoard;
import net.oupz.bountyboard.block.entity.BountyBoardBlockEntity;
import net.oupz.bountyboard.client.screen.BountyBoardMenu;
import net.oupz.bountyboard.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BountyBoardBlock extends BaseEntityBlock {
    public static final MapCodec<BountyBoardBlock> CODEC = simpleCodec(BountyBoardBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    // X and Y position within the 3x3 grid (0-2 for both)
    public static final IntegerProperty X_POS = IntegerProperty.create("x_pos", 0, 2);
    public static final IntegerProperty Y_POS = IntegerProperty.create("y_pos", 0, 2);

    // Full block shape - we want full blocks, not thin slices
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0, 6, 16, 16, 12);
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 4, 16, 16, 10);
    private static final VoxelShape SHAPE_EAST  = Block.box(6, 0, 0, 12, 16, 16);
    private static final VoxelShape SHAPE_WEST  = Block.box(4, 0, 0, 10, 16, 16);

    public BountyBoardBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(X_POS, 0)
                .setValue(Y_POS, 0));
    }

    public BountyBoardBlock() {
        this(Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(-1.0f, 3_600_000.0f)
                .sound(SoundType.WOOD)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false));

    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, X_POS, Y_POS);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        switch (state.getValue(FACING)) {
            case SOUTH: return SHAPE_SOUTH;
            case NORTH: return SHAPE_NORTH;
            case EAST:  return SHAPE_EAST;
            case WEST:  return SHAPE_WEST;
            default:    return SHAPE_SOUTH;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_48689_) {
        return this.defaultBlockState().setValue(FACING, p_48689_.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos bottomLeft = getOriginPos(pos, state);

            // Place all blocks in the 3x3 grid
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    BlockPos placePos = getRelativePos(bottomLeft, facing, x, y);
                    // Don't replace the block we just placed
                    if (!placePos.equals(pos)) {
                        level.setBlock(placePos, this.defaultBlockState()
                                .setValue(FACING, facing)
                                .setValue(X_POS, x)
                                .setValue(Y_POS, y), 3);
                    }
                }
            }
        }
    }

    private BlockPos getRelativePos(BlockPos bottomLeft, Direction facing, int x, int y) {
        // Calculate position based on facing direction
        // x goes to the right when looking at the board
        // y goes up
        return switch (facing) {
            case NORTH -> bottomLeft.east(x).above(y);
            case SOUTH -> bottomLeft.west(x).above(y);
            case EAST -> bottomLeft.south(x).above(y);
            case WEST -> bottomLeft.north(x).above(y);
            default -> bottomLeft;
        };
    }

    private BlockPos getOriginPos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        int x = state.getValue(X_POS);
        int y = state.getValue(Y_POS);

        // Work backwards to find the origin (0,0) position
        return switch (facing) {
            case NORTH -> pos.west(x).below(y);
            case SOUTH -> pos.east(x).below(y);
            case EAST -> pos.north(x).below(y);
            case WEST -> pos.south(x).below(y);
            default -> pos;
        };
    }

    @Override
    protected BlockState rotate(BlockState p_48722_, Rotation p_48723_) {
        return p_48722_.setValue(FACING, p_48723_.rotate(p_48722_.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState p_48719_, Mirror p_48720_) {
        return p_48719_.rotate(p_48720_.getRotation(p_48719_.getValue(FACING)));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative()) {
            // Only drop from origin block
            if (state.getValue(X_POS) == 0 && state.getValue(Y_POS) == 0) {
                Block.dropResources(state, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem());
            }
        }

        // Remove all blocks when any block is broken
        BlockPos origin = getOriginPos(pos, state);
        Direction facing = state.getValue(FACING);

        // Remove all blocks in the 3x3 grid
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                BlockPos removePos = getRelativePos(origin, facing, x, y);
                if (!removePos.equals(pos)) { // Don't remove the block being broken (it's already handled)
                    BlockState removeState = level.getBlockState(removePos);
                    if (removeState.is(this)) {
                        level.setBlock(removePos, Blocks.AIR.defaultBlockState(), 35);
                        level.levelEvent(player, 2001, removePos, getId(removeState));
                    }
                }
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Find the origin block
        BlockPos originPos = getOriginPos(pos, state);
        BlockEntity blockEntity = level.getBlockEntity(originPos);

        if (blockEntity instanceof BountyBoardBlockEntity && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider((containerId, playerInventory, p) ->
                    new BountyBoardMenu(containerId, playerInventory, blockEntity),
                    Component.empty()), originPos); //Component.translatable("menu.title." + BountyBox.MODID + ".bounty_board_menu"))
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Check if origin block still exists
        if (state.getValue(X_POS) != 0 || state.getValue(Y_POS) != 0) {
            BlockPos originPos = getOriginPos(pos, state);
            BlockState originState = level.getBlockState(originPos);

            if (!originState.is(this) || originState.getValue(X_POS) != 0 || originState.getValue(Y_POS) != 0) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Only create block entity for origin block (0,0)
        return (state.getValue(X_POS) == 0 && state.getValue(Y_POS) == 0) ?
                new BountyBoardBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide && state.getValue(X_POS) == 0 && state.getValue(Y_POS) == 0) {
            return createTickerHelper(type, BlockEntityRegistry.BOUNTY_BOARD_BE.get(), BountyBoardBlockEntity::tick);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> serverType, BlockEntityType<E> clientType, BlockEntityTicker<? super E> ticker) {
        return clientType == serverType ? (BlockEntityTicker<A>) ticker : null;
    }
}
