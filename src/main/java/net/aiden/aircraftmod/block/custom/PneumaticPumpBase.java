package net.aiden.aircraftmod.block.custom;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import net.aiden.aircraftmod.block.entity.ModBlockEntities;
import net.aiden.aircraftmod.block.entity.PneumaticPumpBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import org.jetbrains.annotations.Nullable;

import static net.aiden.aircraftmod.block.ModBlocks.PNEUMATIC_PUMP_HEAD;

//TODO: Figure out how to make this create a connected PneumaticPumpBaseBlockEntity.

public class PneumaticPumpBase extends BaseEntityBlock {
    public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final int TRIGGER_EXTEND = 0;
    public static final int TRIGGER_CONTRACT = 1;
    public static final int TRIGGER_DROP = 2;
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(4.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 12.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape UP_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);
    protected static final VoxelShape DOWN_AABB = Block.box(0.0D, 4.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public PneumaticPumpBase(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(EXTENDED, Boolean.valueOf(false)));
    }

    /**
     * If the piston's state is extended, return a voxel that defines the box for the piston base I think.
     * @param state
     * @param getter
     * @param pos
     * @param collisionContext
     * @return
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext collisionContext) {
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

    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack itemStack) {
        if (!level.isClientSide) this.checkIfExtend(level, pos, state);
    }

    /**
     * If a neighboring block is changed on the server, checkIfExtend.
     * @param state
     * @param level
     * @param pos1
     * @param block
     * @param pos2
     * @param b
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos1, Block block, BlockPos pos2, boolean b) {
        if (!level.isClientSide) {
            this.checkIfExtend(level, pos1, state);
        }
    }

    @Override
    public void onPlace(BlockState state1, Level level, BlockPos pos, BlockState state2, boolean b) {
        if (!state2.is(state1.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) == null) {
                this.checkIfExtend(level, pos, state1);
            }

        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return this.defaultBlockState().setValue(FACING, blockPlaceContext.getNearestLookingDirection().getOpposite()).setValue(EXTENDED, Boolean.valueOf(false));
    }

    private void checkIfExtend(Level level, BlockPos basePos, BlockState baseState) {
        Direction pumpDirection = baseState.getValue(FACING); //set "direction" to the direction the block is facing
        boolean isOpposed = this.isOpposed(level, basePos, baseState);
        if (!isOpposed && !baseState.getValue(EXTENDED)) {//if the pump is not extended
            if ((new PneumaticPumpStructureResolver(level, basePos, pumpDirection)).resolve()) {//and if the pump's structure resolves
                level.blockEvent(basePos, this, 0, pumpDirection.get3DDataValue());//make a block event for this block position, this block,
            }
        } else if (isOpposed && baseState.getValue(EXTENDED)) {
            BlockPos blockpos = basePos.relative(pumpDirection, 2);//set blockpos to the block in front of the head
            BlockState blockstate = level.getBlockState(blockpos);//blockstate is the state of the block in front of the head
            int i = 1;
            if (blockstate.is(Blocks.MOVING_PISTON) && blockstate.getValue(FACING) == pumpDirection) {
                BlockEntity blockentity = level.getBlockEntity(blockpos);
                if (blockentity instanceof PistonMovingBlockEntity) {
                    PistonMovingBlockEntity pistonmovingblockentity = (PistonMovingBlockEntity) blockentity;
                    if (pistonmovingblockentity.isExtending() && (pistonmovingblockentity.getProgress(0.0F) < 0.5F || level.getGameTime() == pistonmovingblockentity.getLastTicked() || ((ServerLevel) level).isHandlingTick())) {
                        i = 2;
                    }
                }
            }

            level.blockEvent(basePos, this, i, pumpDirection.get3DDataValue());
        }
    }

    private boolean getNeighborSignal(Level p_60178_, BlockPos p_60179_, Direction p_60180_) {
        for (Direction direction : Direction.values()) {
            if (direction != p_60180_ && p_60178_.hasSignal(p_60179_.relative(direction), direction)) {
                return true;
            }
        }

        if (p_60178_.hasSignal(p_60179_, Direction.DOWN)) {
            return true;
        } else {
            BlockPos blockpos = p_60179_.above();

            for (Direction direction1 : Direction.values()) {
                if (direction1 != Direction.DOWN && p_60178_.hasSignal(blockpos.relative(direction1), direction1)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Used to determine whether the pump's extension is opposed by a powered piston.
     *
     * @param level     the "level" (dimension/world?) in which this check is occurring
     * @param basePos   the position of the base of the pneumatic pump for which we are checking for opposition
     * @param baseState the object containing fields related to the pump, such as the direction it's facing
     * @return true if the pump's extension is opposed by a powered piston head, false otherwise
     */
    public boolean isOpposed(Level level, BlockPos basePos, BlockState baseState) {
        Direction pumpDirection = baseState.getValue(FACING);
        BlockPos potentialPistonPos = basePos.relative(pumpDirection, 2);
        BlockState potentialPistonState = level.getBlockState(potentialPistonPos);
        return (potentialPistonState.is(Blocks.PISTON) || potentialPistonState.is(Blocks.STICKY_PISTON)) && this.getNeighborSignal(level, potentialPistonPos, pumpDirection) && potentialPistonState.getValue(FACING) == pumpDirection.getOpposite();
    }

    /**
     * This method tells the level when the pump is supposed to do something.
     * @param baseState object containing fields related to the pump, such as the direction it's facing
     * @param level the "level" (dimension/world?) in which this check is occurring
     * @param basePos location of the base of the pneumatic pump
     * @param extensionFlag determines what the pump base will do to the head and the blocks it faces
     * @param direction integer that encodes for six directions among three orthogonal axes
     * @return true if the pump should trigger an event, false otherwise
     */
    public boolean triggerEvent(BlockState baseState, Level level, BlockPos basePos, int extensionFlag, int direction) {
        Direction pumpDirection = baseState.getValue(FACING);
        if (!level.isClientSide) {
            boolean isOpposed = isOpposed(level, basePos, baseState);
            if (!isOpposed && (extensionFlag == TRIGGER_CONTRACT || extensionFlag == TRIGGER_DROP)) {
                //flag pump for extension if it hasn't been already
                level.setBlock(basePos, baseState.setValue(EXTENDED, Boolean.valueOf(true)), 2);
                return false;
            }

            if (isOpposed && extensionFlag == TRIGGER_EXTEND) {
                return false;
            }
        }

        if (extensionFlag == TRIGGER_EXTEND) {
            //pump is extending
            if (net.minecraftforge.event.ForgeEventFactory.onPistonMovePre(level, basePos, pumpDirection, true))
                return false;//trigger no event if pump is already extending
            if (!this.moveBlocks(level, basePos, pumpDirection))
                return false;//trigger no event if pump cannot push the blocks in front of it

            level.setBlock(basePos, baseState.setValue(EXTENDED, Boolean.valueOf(true)), 67);//replace the base with an extended version of itself
            level.playSound(null, basePos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.25F + 0.6F);
            level.gameEvent(null, GameEvent.PISTON_EXTEND, basePos);
        } else if (extensionFlag == TRIGGER_CONTRACT || extensionFlag == TRIGGER_DROP) {
            //pump is contracting
            if (net.minecraftforge.event.ForgeEventFactory.onPistonMovePre(level, basePos, pumpDirection, false))
                return false;//trigger no event if piston is already contracting
            BlockEntity headEntity = level.getBlockEntity(basePos.relative(pumpDirection));
            //if the block entity at the head's position is a PistonMovingBlockEntity,
            if (headEntity instanceof PistonMovingBlockEntity) ((PistonMovingBlockEntity) headEntity).finalTick();

            BlockState blockstate = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, pumpDirection).setValue(MovingPistonBlock.TYPE, PistonType.DEFAULT);
            level.setBlock(basePos, blockstate, 20);
            // Block entity? I wonder what this does. Maybe I need to replace some of this.
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(basePos, blockstate, this.defaultBlockState().setValue
                    (FACING, Direction.from3DDataValue(direction & 7)), pumpDirection, false, true));
            level.blockUpdated(basePos, blockstate.getBlock());//tell the level that a moving piston got updated at the position of the piston's base
            blockstate.updateNeighbourShapes(level, basePos, 2);
            level.removeBlock(basePos.relative(pumpDirection), false);//remove the piston head

            level.playSound(null, basePos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.15F + 0.6F);
            level.gameEvent(null, GameEvent.PISTON_CONTRACT, basePos);
        }

        net.minecraftforge.event.ForgeEventFactory.onPistonMovePost(level, basePos, pumpDirection, (extensionFlag == TRIGGER_EXTEND));
        return true;
    }

    public static boolean isPushable(BlockState pushCandidateState, Level level, BlockPos pos, Direction direction1, boolean p_60209_, Direction direction2) {
        if (pos.getY() >= level.getMinBuildHeight() && pos.getY() <= level.getMaxBuildHeight() - 1 && level.getWorldBorder().isWithinBounds(pos)) {
            if (pushCandidateState.isAir()) {
                return true;
            } else if (!pushCandidateState.is(Blocks.OBSIDIAN) && !pushCandidateState.is(Blocks.CRYING_OBSIDIAN) && !pushCandidateState.is(Blocks.RESPAWN_ANCHOR) && !pushCandidateState.is(Blocks.REINFORCED_DEEPSLATE)) {
                if (direction1 == Direction.DOWN && pos.getY() == level.getMinBuildHeight()) {
                    return false;
                } else if (direction1 == Direction.UP && pos.getY() == level.getMaxBuildHeight() - 1) {
                    return false;
                } else {
                    if (!pushCandidateState.is(Blocks.PISTON) && !pushCandidateState.is(Blocks.STICKY_PISTON)) {
                        if (pushCandidateState.getDestroySpeed(level, pos) == -1.0F) {
                            return false;
                        }

                        switch (pushCandidateState.getPistonPushReaction()) {
                            case BLOCK -> {
                                return false;
                            }
                            case DESTROY -> {
                                return p_60209_;
                            }
                            case PUSH_ONLY -> {
                                return direction1 == direction2;
                            }
                        }
                    } else if (pushCandidateState.getValue(EXTENDED)) {
                        return false;
                    }

                    return !pushCandidateState.hasBlockEntity();
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    //main method: annotate and modify
    private boolean moveBlocks(Level level, BlockPos basePos, Direction pumpDirection) {
        BlockPos headPos = basePos.relative(pumpDirection); //set 'blockpos' to the position of the head

        PneumaticPumpStructureResolver pneumaticPumpStructureResolver = new PneumaticPumpStructureResolver(level, basePos, pumpDirection);
        if (!pneumaticPumpStructureResolver.resolve()) {
            return false;
        } else {
            Map<BlockPos, BlockState> map = Maps.newHashMap();

            List<BlockPos> list2 = pneumaticPumpStructureResolver.getToDestroy();

            for (int k = list2.size() - 1; k >= 0; --k) {
                BlockPos blockpos2 = list2.get(k);
                BlockState blockstate1 = level.getBlockState(blockpos2);
                BlockEntity blockentity = blockstate1.hasBlockEntity() ? level.getBlockEntity(blockpos2) : null;
                dropResources(blockstate1, level, blockpos2, blockentity);
                level.setBlock(blockpos2, Blocks.AIR.defaultBlockState(), 18);
                level.gameEvent(GameEvent.BLOCK_DESTROY, blockpos2, GameEvent.Context.of(blockstate1));
                if (!blockstate1.is(BlockTags.FIRE)) {
                    level.addDestroyBlockEffect(blockpos2, blockstate1);
                }
            }

            PistonType pistontype = PistonType.DEFAULT;
            BlockState pumpHeadState = PNEUMATIC_PUMP_HEAD.get().defaultBlockState().setValue(PneumaticPumpHead.FACING, pumpDirection).setValue(PneumaticPumpHead.TYPE, pistontype);
            BlockState blockstate6 = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, pumpDirection).setValue(MovingPistonBlock.TYPE, PistonType.DEFAULT);
            map.remove(headPos);
            level.setBlock(headPos, blockstate6, 68);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(headPos, blockstate6, pumpHeadState, pumpDirection, true, true));

            BlockState blockstate3 = Blocks.AIR.defaultBlockState();

            for (BlockPos blockpos4 : map.keySet()) {
                level.setBlock(blockpos4, blockstate3, 82);
            }

            for (Map.Entry<BlockPos, BlockState> entry : map.entrySet()) {
                BlockPos blockpos5 = entry.getKey();
                BlockState blockstate2 = entry.getValue();
                blockstate2.updateIndirectNeighbourShapes(level, blockpos5, 2);
                blockstate3.updateNeighbourShapes(level, blockpos5, 2);
                blockstate3.updateIndirectNeighbourShapes(level, blockpos5, 2);
            }

            level.updateNeighborsAt(headPos, PNEUMATIC_PUMP_HEAD.get());

            return true;
        }
    }

    public BlockState rotate(BlockState p_60215_, Rotation p_60216_) {
        return p_60215_.setValue(FACING, p_60216_.rotate(p_60215_.getValue(FACING)));
    }

    public BlockState rotate(BlockState state, net.minecraft.world.level.LevelAccessor world, BlockPos pos, Rotation direction) {
        return state.getValue(EXTENDED) ? state : super.rotate(state, world, pos, direction);
    }

    public BlockState mirror(BlockState p_60212_, Mirror p_60213_) {
        return p_60212_.rotate(p_60213_.getRotation(p_60212_.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_60218_) {
        p_60218_.add(FACING, EXTENDED);
    }

    public boolean useShapeForLightOcclusion(BlockState p_60231_) {
        return p_60231_.getValue(EXTENDED);
    }

    public boolean isPathfindable(BlockState p_60187_, BlockGetter p_60188_, BlockPos p_60189_, PathComputationType p_60190_) {
        return false;
    }

    public PushReaction getPistonPushReaction(BlockState baseState) {
        if (baseState.getValue(EXTENDED)) return PushReaction.BLOCK;
        else return PushReaction.NORMAL;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumaticPumpBaseBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PNEUMATIC_PUMP_BASE.get(), PneumaticPumpBaseBlockEntity::tick);
    }
}
