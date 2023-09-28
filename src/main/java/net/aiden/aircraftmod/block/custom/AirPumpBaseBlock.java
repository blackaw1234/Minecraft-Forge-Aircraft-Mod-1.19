package net.aiden.aircraftmod.block.custom;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import net.aiden.aircraftmod.block.entity.ModBlockEntities;
import net.aiden.aircraftmod.block.entity.AirPumpBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.aiden.aircraftmod.block.ModBlocks.AIR_PUMP_HEAD;
import static net.minecraft.world.level.block.piston.PistonBaseBlock.TRIGGER_EXTEND;
import static net.minecraft.world.level.block.piston.PistonBaseBlock.TRIGGER_CONTRACT;

/**
 * The base block of a pneumatic pump, which holds pressure when retracted.
 *
 * @author aiden
 */
public class AirPumpBaseBlock extends BaseEntityBlock {
    /**
     * used to check whether the pump is currently extended
     */
    public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
    /**
     * used to check which direction the pump is facing
     */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /**
     * collision box for the pump base when it is extended eastward
     */
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D);
    /**
     * collision box for the pump base when it is extended westward
     */
    protected static final VoxelShape WEST_AABB = Block.box(4.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    /**
     * collision box for the pump base when it is extended southward
     */
    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 12.0D);
    /**
     * collision box for the pump base when it is extended northward
     */
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 16.0D);
    /**
     * collision box for the pump base when it is extended upward
     */
    protected static final VoxelShape UP_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    /**
     * collision box for the pump base when it is extended downward
     */
    protected static final VoxelShape DOWN_AABB = Block.box(0.0D, 4.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    /**
     * Constructs a PneumaticPumpBase object.
     *
     * @param properties behavioral properties provided during registration
     */
    public AirPumpBaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(EXTENDED, false));
    }

    /**
     * Provides the collision box for the pump base based on whether it is extended and the direction it is facing.
     *
     * @param state            pump base's BlockState
     * @param getter           unused parameter from overridden method
     * @param pos              unused parameter from overridden method
     * @param collisionContext unused parameter from overridden method
     * @return a voxel that defines the collision box for the pump base
     */
    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter getter, @NotNull BlockPos pos, @NotNull CollisionContext collisionContext) {
        if (state.getValue(EXTENDED)) {
            return switch (state.getValue(FACING)) {
                case DOWN -> DOWN_AABB;
                default -> UP_AABB;
                case NORTH -> NORTH_AABB;
                case SOUTH -> SOUTH_AABB;
                case WEST -> WEST_AABB;
                case EAST -> EAST_AABB;
            };
        } else {
            return Shapes.block();
        }
    }

    /**
     * When the pump base is set as placed by some living entity, check if it should extend.
     *
     * @param level     spatial and network context
     * @param pos       pump base's location
     * @param state     pump base's BlockState
     * @param entity    entity that placed the pump base?
     * @param itemStack stack from which the pump base was placed?
     */
    @Override
    public void setPlacedBy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity entity, @NotNull ItemStack itemStack) {
        if (!level.isClientSide) this.checkIfExtend(level, pos, state);
    }

    /**
     * Returns the state of the pump base upon placement.
     *
     * @param placeContext object that stores information about the placement of this block
     * @return state of the pump base upon placement
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext placeContext) {
        return this.defaultBlockState().setValue(FACING, placeContext.getNearestLookingDirection().getOpposite()).setValue(EXTENDED, false);
    }

    /**
     * If a neighboring block is changed on the server, check if we need to change the extension value.
     *
     * @param state       pump base's BlockState
     * @param level       spatial and network context
     * @param basePos     location of the pump base
     * @param block       unused parameter from overridden method
     * @param neighborPos unused parameter from overridden method
     * @param b           unused parameter from overridden method
     */
    @Override
    public void neighborChanged(@NotNull BlockState state, Level level, @NotNull BlockPos basePos, @NotNull Block block, @NotNull BlockPos neighborPos, boolean b) {
        if (!level.isClientSide) {
            this.checkIfExtend(level, basePos, state);
        }
    }

    /**
     * Create an event for this block if its extension should be changed.
     *
     * @param level     spatial and network context
     * @param basePos   pump base's location
     * @param baseState pump base's BlockState
     */
    public void checkIfExtend(Level level, BlockPos basePos, BlockState baseState) {
        Direction pumpDirection = baseState.getValue(FACING); //set "direction" to the direction the block is facing
        boolean isOpposed = this.isOpposed(level, basePos);

        // If the pump needs to extend
        if (!isOpposed && !baseState.getValue(EXTENDED)) {
            if ((new AirPumpStructureResolver(level, basePos, pumpDirection)).isCanPush()) {// and if its structure resolves
                level.blockEvent(basePos, this, TRIGGER_EXTEND, pumpDirection.get3DDataValue());//make a block event for this block position, this block,
            }
        } else if (isOpposed && baseState.getValue(EXTENDED)) {
            level.blockEvent(basePos, this, TRIGGER_CONTRACT, pumpDirection.get3DDataValue());
        }
    }

    /**
     * Determines whether a piston is powered.
     *
     * @param level     spatial and network context
     * @param pistonPos location of piston tested for signal
     * @return true if piston is powered, false if it is not powered
     */
    private boolean isPistonPowered(Level level, BlockPos pistonPos) {
        Direction pistonDirection = level.getBlockState(pistonPos).getValue(FACING);

        // Piston is powered if any of the neighbors it isn't facing are powered.
        for (Direction testedSignalDirection : Direction.values()) {
            if (testedSignalDirection != pistonDirection && level.hasSignal(pistonPos.relative(testedSignalDirection), testedSignalDirection)) {
                return true;
            }
        }

        if (level.hasSignal(pistonPos, Direction.DOWN)) {
            return true;
        } else {
            BlockPos blockpos = pistonPos.above();

            for (Direction direction1 : Direction.values()) {
                if (direction1 != Direction.DOWN && level.hasSignal(blockpos.relative(direction1), direction1)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Determines whether the pump's extension is opposed by a powered piston.
     *
     * @param level   the "level" (dimension/world?) in which this check is occurring
     * @param basePos the position of the base of the pneumatic pump for which we are checking for opposition
     * @return true if the pump's extension is opposed by a powered piston head, false otherwise
     */
    public boolean isOpposed(Level level, BlockPos basePos) {
        BlockState baseState = level.getBlockState(basePos);
        Direction pumpDirection = baseState.getValue(FACING);
        BlockPos potentialPistonPos = basePos.relative(pumpDirection, 2);
        BlockState potentialPistonState = level.getBlockState(potentialPistonPos);
        return (potentialPistonState.is(Blocks.PISTON) || potentialPistonState.is(Blocks.STICKY_PISTON)) && isPistonPowered(level, potentialPistonPos) && potentialPistonState.getValue(FACING) == pumpDirection.getOpposite();
    }

    /**
     * This method tells the level when the pump is supposed to do something.
     *
     * @param baseState     object containing fields related to the pump, such as the direction it's facing
     * @param level         the "level" (dimension/world?) in which this check is occurring
     * @param basePos       location of the base of the pneumatic pump
     * @param extensionFlag determines whether the pump extends
     * @param direction     integer that encodes for one of six directions
     * @return true if the pump should trigger an event, false otherwise
     */
    @Override
    public boolean triggerEvent(BlockState baseState, Level level, @NotNull BlockPos basePos, int extensionFlag, int direction) {
        Direction pumpDirection = baseState.getValue(FACING);

        // pump is not opposed by a powered piston
        if (extensionFlag == TRIGGER_EXTEND) {
            // trigger no event if pump is already extending
            if (net.minecraftforge.event.ForgeEventFactory.onPistonMovePre(level, basePos, pumpDirection, true)) {
                return false;
            }
            // trigger no event if pump cannot push the blocks in front of it
            if (!this.moveBlock(level, basePos, pumpDirection)) {
                return false;
            }

            level.setBlock(basePos, baseState.setValue(EXTENDED, true), 67);//replace the base with an extended version of itself
            level.playSound(null, basePos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.25F + 0.6F);
            level.gameEvent(null, GameEvent.PISTON_EXTEND, basePos);
        } else {
            // pump is contracting
            if (net.minecraftforge.event.ForgeEventFactory.onPistonMovePre(level, basePos, pumpDirection, false))
                return false;//trigger no event if piston is already contracting
            BlockEntity headEntity = level.getBlockEntity(basePos.relative(pumpDirection));
            // if the block entity at the head's position is a PistonMovingBlockEntity,
            if (headEntity instanceof PistonMovingBlockEntity) ((PistonMovingBlockEntity) headEntity).finalTick();

            BlockState blockstate = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, pumpDirection).setValue(MovingPistonBlock.TYPE, PistonType.DEFAULT);
            level.setBlock(basePos, blockstate, 20);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(basePos, blockstate, this.defaultBlockState().setValue
                    (FACING, Direction.from3DDataValue(direction & 7)), pumpDirection, false, true));
            level.blockUpdated(basePos, blockstate.getBlock());// tell the level that a moving piston got updated at the position of the piston's base
            blockstate.updateNeighbourShapes(level, basePos, 2);
            level.removeBlock(basePos.relative(pumpDirection), false);// remove the piston head

            level.playSound(null, basePos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.15F + 0.6F);
            level.gameEvent(null, GameEvent.PISTON_CONTRACT, basePos);
        }

        net.minecraftforge.event.ForgeEventFactory.onPistonMovePost(level, basePos, pumpDirection, (extensionFlag == TRIGGER_EXTEND));
        return true;
    }

    /**
     * Checks whether the block in front of the pump base is pushable.
     *
     * @param pushCandidateState push candidate's BlockState
     * @param level              spatial and network context
     * @param pushCandidatePos   push candidate's location
     * @param pushDirection      direction in which the pump will attempt to push
     * @param isPushDestructible decides whether blocks that are destroyed by motion should be considered pushable
     * @return true if block in front of pump should not be pushed, false if it should be pushed
     */
    public static boolean isNotPushable(BlockState pushCandidateState, Level level, BlockPos pushCandidatePos, Direction pushDirection, boolean isPushDestructible) {
        if (pushCandidatePos.getY() >= level.getMinBuildHeight() && pushCandidatePos.getY() < level.getMaxBuildHeight() && level.getWorldBorder().isWithinBounds(pushCandidatePos)) {
            if (pushCandidateState.isAir()) {
                return false;
            } else if (!pushCandidateState.is(Blocks.OBSIDIAN) && !pushCandidateState.is(Blocks.CRYING_OBSIDIAN) && !pushCandidateState.is(Blocks.RESPAWN_ANCHOR) && !pushCandidateState.is(Blocks.REINFORCED_DEEPSLATE)) {
                if (pushDirection == Direction.DOWN && pushCandidatePos.getY() == level.getMinBuildHeight()) {
                    return true;
                } else if (pushDirection == Direction.UP && pushCandidatePos.getY() == level.getMaxBuildHeight() - 1) {
                    return true;
                } else {
                    if (!pushCandidateState.is(Blocks.PISTON) && !pushCandidateState.is(Blocks.STICKY_PISTON)) {
                        if (pushCandidateState.getDestroySpeed(level, pushCandidatePos) == -1.0F) {
                            return true;
                        }

                        switch (pushCandidateState.getPistonPushReaction()) {
                            case BLOCK -> {
                                return true;
                            }
                            case DESTROY -> {
                                return !isPushDestructible;
                            }
                            case PUSH_ONLY -> {
                                return false;
                            }
                        }
                    } else if (pushCandidateState.getValue(EXTENDED)) {
                        return true;
                    }

                    return pushCandidateState.hasBlockEntity();
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * Pushes the block in front of the pump head if it can.
     *
     * @param level         spatial and network context
     * @param basePos       pump base's location
     * @param pumpDirection direction in which the pump faces
     * @return true if the block was pushed, false otherwise
     */
    private boolean moveBlock(Level level, BlockPos basePos, Direction pumpDirection) {
        BlockPos headPos = basePos.relative(pumpDirection);

        AirPumpStructureResolver airPumpStructureResolver = new AirPumpStructureResolver(level, basePos, pumpDirection);
        if (!airPumpStructureResolver.isCanPush()) {
            return false;
        } else {
            Map<BlockPos, BlockState> map = Maps.newHashMap();

            List<BlockPos> locationsToDestroy = airPumpStructureResolver.getToDestroy();

            for (int i = locationsToDestroy.size() - 1; i >= 0; --i) {
                BlockPos locationToDestroy = locationsToDestroy.get(i);
                BlockState blockStateToDestroy = level.getBlockState(locationToDestroy);
                BlockEntity blockEntityToDestroy = blockStateToDestroy.hasBlockEntity() ? level.getBlockEntity(locationToDestroy) : null;

                dropResources(blockStateToDestroy, level, locationToDestroy, blockEntityToDestroy);
                level.setBlock(locationToDestroy, Blocks.AIR.defaultBlockState(), 18);
                level.gameEvent(GameEvent.BLOCK_DESTROY, locationToDestroy, GameEvent.Context.of(blockStateToDestroy));
                if (!blockStateToDestroy.is(BlockTags.FIRE)) {
                    level.addDestroyBlockEffect(locationToDestroy, blockStateToDestroy);
                }
            }

            PistonType pistonType = PistonType.DEFAULT;
            BlockState pumpHeadState = AIR_PUMP_HEAD.get().defaultBlockState().setValue(AirPumpHeadBlock.FACING, pumpDirection).setValue(AirPumpHeadBlock.TYPE, pistonType);
            BlockState blockstate6 = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, pumpDirection).setValue(MovingPistonBlock.TYPE, PistonType.DEFAULT);

            level.setBlock(headPos, blockstate6, 68);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(headPos, blockstate6, pumpHeadState, pumpDirection, true, true));

            level.updateNeighborsAt(headPos, AIR_PUMP_HEAD.get());

            return true;
        }
    }

    public @NotNull BlockState rotate(BlockState p_60215_, Rotation p_60216_) {
        return p_60215_.setValue(FACING, p_60216_.rotate(p_60215_.getValue(FACING)));
    }

    public BlockState rotate(BlockState state, net.minecraft.world.level.LevelAccessor world, BlockPos pos, Rotation direction) {
        return state.getValue(EXTENDED) ? state : super.rotate(state, world, pos, direction);
    }

    public @NotNull BlockState mirror(BlockState p_60212_, Mirror p_60213_) {
        return p_60212_.rotate(p_60213_.getRotation(p_60212_.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_60218_) {
        p_60218_.add(FACING, EXTENDED);
    }

    public boolean useShapeForLightOcclusion(BlockState p_60231_) {
        return p_60231_.getValue(EXTENDED);
    }

    @Override
    public boolean isPathfindable(@NotNull BlockState p_60187_, @NotNull BlockGetter p_60188_, @NotNull BlockPos p_60189_, @NotNull PathComputationType p_60190_) {
        return false;
    }

    public @NotNull PushReaction getPistonPushReaction(BlockState baseState) {
        if (baseState.getValue(EXTENDED)) return PushReaction.BLOCK;
        else return PushReaction.NORMAL;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new AirPumpBaseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.AIR_PUMP_BASE.get(), AirPumpBaseBlockEntity::tick);
    }
}
