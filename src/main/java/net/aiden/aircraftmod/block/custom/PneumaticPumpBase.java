package net.aiden.aircraftmod.block.custom;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PneumaticPumpBase extends DirectionalBlock {
    public static final BooleanProperty EXTENDED = BlockStateProperties.EXTENDED;
    public static final int TRIGGER_EXTEND = 0;
    public static final int TRIGGER_CONTRACT = 1;
    public static final int TRIGGER_DROP = 2;
    public static final float PLATFORM_THICKNESS = 4.0F;
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

    //A function we can use to find info about this block in game
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        //use is called twice on the Server (mainhand & offhand) & twice on the Client (mainhand & offhand)
        player.sendSystemMessage(Component.literal(this.getClass().getSimpleName()));

        return super.use(state, level, pos, player, hand, result);
    }

    //If the piston's state is extended, return a voxel that defines the box for the piston head I think
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext collisionContext) {
        if (state.getValue(EXTENDED)) {
            switch ((Direction)state.getValue(FACING)) {
                case DOWN:
                    return DOWN_AABB;
                case UP:
                default:
                    return UP_AABB;
                case NORTH:
                    return NORTH_AABB;
                case SOUTH:
                    return SOUTH_AABB;
                case WEST:
                    return WEST_AABB;
                case EAST:
                    return EAST_AABB;
            }
        } else {
            return Shapes.block();
        }
    }

    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack itemStack) {
        if (!level.isClientSide) {
            this.checkIfExtend(level, pos, state);
        }

    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos1, Block block, BlockPos pos2, boolean b) {
        if (!level.isClientSide) {
            this.checkIfExtend(level, pos1, state);
        }

    }

    public void onPlace(BlockState state1, Level level, BlockPos pos, BlockState state2, boolean b) {
        if (!state2.is(state1.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) == null) {
                this.checkIfExtend(level, pos, state1);
            }

        }
    }

    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return this.defaultBlockState().setValue(FACING, blockPlaceContext.getNearestLookingDirection().getOpposite()).setValue(EXTENDED, Boolean.valueOf(true));
    }

    //THIS FUNCTION RIGHT HERE DOES SOMETHING SPECIAL; figure it out
    private void checkIfExtend(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING); //set "direction" to the direction the block is facing
        boolean flag = this.getNeighborSignal(level, pos, direction); //set "flag" to true if the piston is powered
        if (flag && !state.getValue(EXTENDED)) {//if the piston is powered, but not extended
            if ((new PneumaticPumpStructureResolver(level, pos, direction, true)).resolve()) {//and if the piston's structure resolves
                level.blockEvent(pos, this, 0, direction.get3DDataValue());//make a block event for this block position, this block,
            }
        } else if (!flag && state.getValue(EXTENDED)) {
            BlockPos blockpos = pos.relative(direction, 2);
            BlockState blockstate = level.getBlockState(blockpos);
            int i = 1;
            if (blockstate.is(Blocks.MOVING_PISTON) && blockstate.getValue(FACING) == direction) {
                BlockEntity blockentity = level.getBlockEntity(blockpos);
                if (blockentity instanceof PistonMovingBlockEntity) {
                    PistonMovingBlockEntity pistonmovingblockentity = (PistonMovingBlockEntity)blockentity;
                    if (pistonmovingblockentity.isExtending() && (pistonmovingblockentity.getProgress(0.0F) < 0.5F || level.getGameTime() == pistonmovingblockentity.getLastTicked() || ((ServerLevel)level).isHandlingTick())) {
                        i = 2;
                    }
                }
            }

            level.blockEvent(pos, this, i, direction.get3DDataValue());
        }

    }

    //This function prob has something to do with redstone signals; I should be able to get rid of it
    private boolean getNeighborSignal(Level level, BlockPos pumpPos, Direction pumpDirection) {
        for(Direction direction : Direction.values()) {
            if (direction != pumpDirection && level.hasSignal(pumpPos.relative(direction), direction)) {
                return true;
            }
        }

        if (level.hasSignal(pumpPos, Direction.DOWN)) {
            return true;
        } else {
            BlockPos blockpos = pumpPos.above();

            for(Direction direction1 : Direction.values()) {
                if (direction1 != Direction.DOWN && level.hasSignal(blockpos.relative(direction1), direction1)) {
                    return true;
                }
            }

            return false;
        }
    }

    //This method tells the level when the pump is supposed to do something
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int p_60195_, int p_60196_) {
        Direction direction = state.getValue(FACING);
        if (!level.isClientSide) {
            boolean flag = this.getNeighborSignal(level, pos, direction);
            if (flag && (p_60195_ == 1 || p_60195_ == 2)) {
                level.setBlock(pos, state.setValue(EXTENDED, Boolean.valueOf(true)), 2);
                return false;
            }

            if (!flag && p_60195_ == 0) {
                return false;
            }
        }

        if (p_60195_ == 0) {
            if (net.minecraftforge.event.ForgeEventFactory.onPistonMovePre(level, pos, direction, true)) return false;
            if (!this.moveBlocks(level, pos, direction, true)) {
                return false;
            }

            level.setBlock(pos, state.setValue(EXTENDED, Boolean.valueOf(true)), 67);
            level.playSound((Player)null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.25F + 0.6F);
            level.gameEvent((Entity)null, GameEvent.PISTON_EXTEND, pos);
        } else if (p_60195_ == 1 || p_60195_ == 2) {
            if (net.minecraftforge.event.ForgeEventFactory.onPistonMovePre(level, pos, direction, false)) return false;
            BlockEntity blockentity1 = level.getBlockEntity(pos.relative(direction));
            if (blockentity1 instanceof PistonMovingBlockEntity) {
                ((PistonMovingBlockEntity)blockentity1).finalTick();
            }

            BlockState blockstate = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, direction).setValue(MovingPistonBlock.TYPE, PistonType.DEFAULT);
            level.setBlock(pos, blockstate, 20);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(pos, blockstate, this.defaultBlockState().setValue(FACING, Direction.from3DDataValue(p_60196_ & 7)), direction, false, true));
            level.blockUpdated(pos, blockstate.getBlock());
            blockstate.updateNeighbourShapes(level, pos, 2);
            level.removeBlock(pos.relative(direction), false);

            level.playSound((Player)null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.15F + 0.6F);
            level.gameEvent((Entity)null, GameEvent.PISTON_CONTRACT, pos);
        }

        net.minecraftforge.event.ForgeEventFactory.onPistonMovePost(level, pos, direction, (p_60195_ == 0));
        return true;
    }

    public static boolean isPushable(BlockState state, Level level, BlockPos pos, Direction direction1, boolean p_60209_, Direction direction2) {
        if (pos.getY() >= level.getMinBuildHeight() && pos.getY() <= level.getMaxBuildHeight() - 1 && level.getWorldBorder().isWithinBounds(pos)) {
            if (state.isAir()) {
                return true;
            } else if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.CRYING_OBSIDIAN) && !state.is(Blocks.RESPAWN_ANCHOR) && !state.is(Blocks.REINFORCED_DEEPSLATE)) {
                if (direction1 == Direction.DOWN && pos.getY() == level.getMinBuildHeight()) {
                    return false;
                } else if (direction1 == Direction.UP && pos.getY() == level.getMaxBuildHeight() - 1) {
                    return false;
                } else {
                    if (!state.is(Blocks.PISTON) && !state.is(Blocks.STICKY_PISTON)) {
                        if (state.getDestroySpeed(level, pos) == -1.0F) {
                            return false;
                        }

                        switch (state.getPistonPushReaction()) {
                            case BLOCK:
                                return false;
                            case DESTROY:
                                return p_60209_;
                            case PUSH_ONLY:
                                return direction1 == direction2;
                        }
                    } else if (state.getValue(EXTENDED)) {
                        return false;
                    }

                    return !state.hasBlockEntity();
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    //main method: annotate and modify
    private boolean moveBlocks(Level level, BlockPos pos, Direction pumpDirection, boolean shouldBeExtended) {
        BlockPos blockpos = pos.relative(pumpDirection); //set 'blockpos' to the position of the block to be pushed
        if (!shouldBeExtended && level.getBlockState(blockpos).is(Blocks.PISTON_HEAD)) {
            level.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 20);
        }

        PneumaticPumpStructureResolver pneumaticPumpStructureResolver = new PneumaticPumpStructureResolver(level, pos, pumpDirection, shouldBeExtended);
        if (!pneumaticPumpStructureResolver.resolve()) {
            return false;
        } else {
            Map<BlockPos, BlockState> map = Maps.newHashMap();
            List<BlockPos> list = pneumaticPumpStructureResolver.getToPush();
            List<BlockState> list1 = Lists.newArrayList();

            for(int i = 0; i < list.size(); ++i) {
                BlockPos blockpos1 = list.get(i);
                BlockState blockstate = level.getBlockState(blockpos1);
                list1.add(blockstate);
                map.put(blockpos1, blockstate);
            }

            List<BlockPos> list2 = pneumaticPumpStructureResolver.getToDestroy();
            BlockState[] ablockstate = new BlockState[list.size() + list2.size()];
            Direction direction = shouldBeExtended ? pumpDirection : pumpDirection.getOpposite();
            int j = 0;

            for(int k = list2.size() - 1; k >= 0; --k) {
                BlockPos blockpos2 = list2.get(k);
                BlockState blockstate1 = level.getBlockState(blockpos2);
                BlockEntity blockentity = blockstate1.hasBlockEntity() ? level.getBlockEntity(blockpos2) : null;
                dropResources(blockstate1, level, blockpos2, blockentity);
                level.setBlock(blockpos2, Blocks.AIR.defaultBlockState(), 18);
                level.gameEvent(GameEvent.BLOCK_DESTROY, blockpos2, GameEvent.Context.of(blockstate1));
                if (!blockstate1.is(BlockTags.FIRE)) {
                    level.addDestroyBlockEffect(blockpos2, blockstate1);
                }

                ablockstate[j++] = blockstate1;
            }

            for(int l = list.size() - 1; l >= 0; --l) {
                BlockPos blockpos3 = list.get(l);
                BlockState blockstate5 = level.getBlockState(blockpos3);
                blockpos3 = blockpos3.relative(direction);
                map.remove(blockpos3);
                BlockState blockstate8 = Blocks.MOVING_PISTON.defaultBlockState().setValue(FACING, pumpDirection);
                level.setBlock(blockpos3, blockstate8, 68);
                level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(blockpos3, blockstate8, list1.get(l), pumpDirection, shouldBeExtended, false));
                ablockstate[j++] = blockstate5;
            }

            if (shouldBeExtended) {
                PistonType pistontype = PistonType.DEFAULT;
                BlockState blockstate4 = Blocks.PISTON_HEAD.defaultBlockState().setValue(PneumaticPumpHead.FACING, pumpDirection).setValue(PneumaticPumpHead.TYPE, pistontype);
                BlockState blockstate6 = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, pumpDirection).setValue(MovingPistonBlock.TYPE, PistonType.DEFAULT);
                map.remove(blockpos);
                level.setBlock(blockpos, blockstate6, 68);
                level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(blockpos, blockstate6, blockstate4, pumpDirection, true, true));
            }

            BlockState blockstate3 = Blocks.AIR.defaultBlockState();

            for(BlockPos blockpos4 : map.keySet()) {
                level.setBlock(blockpos4, blockstate3, 82);
            }

            for(Map.Entry<BlockPos, BlockState> entry : map.entrySet()) {
                BlockPos blockpos5 = entry.getKey();
                BlockState blockstate2 = entry.getValue();
                blockstate2.updateIndirectNeighbourShapes(level, blockpos5, 2);
                blockstate3.updateNeighbourShapes(level, blockpos5, 2);
                blockstate3.updateIndirectNeighbourShapes(level, blockpos5, 2);
            }

            j = 0;

            for(int i1 = list2.size() - 1; i1 >= 0; --i1) {
                BlockState blockstate7 = ablockstate[j++];
                BlockPos blockpos6 = list2.get(i1);
                blockstate7.updateIndirectNeighbourShapes(level, blockpos6, 2);
                level.updateNeighborsAt(blockpos6, blockstate7.getBlock());
            }

            for(int j1 = list.size() - 1; j1 >= 0; --j1) {
                level.updateNeighborsAt(list.get(j1), ablockstate[j++].getBlock());
            }

            if (shouldBeExtended) {
                level.updateNeighborsAt(blockpos, Blocks.PISTON_HEAD);
            }

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
}
