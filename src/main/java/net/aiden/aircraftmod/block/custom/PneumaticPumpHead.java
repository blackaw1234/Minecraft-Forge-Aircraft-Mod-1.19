package net.aiden.aircraftmod.block.custom;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.aiden.aircraftmod.block.ModBlocks.PNEUMATIC_PUMP_BASE;

public class PneumaticPumpHead extends DirectionalBlock {
    public static final EnumProperty<PistonType> TYPE = BlockStateProperties.PISTON_TYPE;
    public static final BooleanProperty SHORT = BlockStateProperties.SHORT;
    protected static final VoxelShape EAST_AABB = Block.box(12.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(0.0D, 0.0D, 0.0D, 4.0D, 16.0D, 16.0D);
    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 12.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 4.0D);
    protected static final VoxelShape UP_AABB = Block.box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape DOWN_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D);
    protected static final VoxelShape UP_ARM_AABB = Block.box(6.0D, -4.0D, 6.0D, 10.0D, 12.0D, 10.0D);
    protected static final VoxelShape DOWN_ARM_AABB = Block.box(6.0D, 4.0D, 6.0D, 10.0D, 20.0D, 10.0D);
    protected static final VoxelShape SOUTH_ARM_AABB = Block.box(6.0D, 6.0D, -4.0D, 10.0D, 10.0D, 12.0D);
    protected static final VoxelShape NORTH_ARM_AABB = Block.box(6.0D, 6.0D, 4.0D, 10.0D, 10.0D, 20.0D);
    protected static final VoxelShape EAST_ARM_AABB = Block.box(-4.0D, 6.0D, 6.0D, 12.0D, 10.0D, 10.0D);
    protected static final VoxelShape WEST_ARM_AABB = Block.box(4.0D, 6.0D, 6.0D, 20.0D, 10.0D, 10.0D);
    protected static final VoxelShape SHORT_UP_ARM_AABB = Block.box(6.0D, 0.0D, 6.0D, 10.0D, 12.0D, 10.0D);
    protected static final VoxelShape SHORT_DOWN_ARM_AABB = Block.box(6.0D, 4.0D, 6.0D, 10.0D, 16.0D, 10.0D);
    protected static final VoxelShape SHORT_SOUTH_ARM_AABB = Block.box(6.0D, 6.0D, 0.0D, 10.0D, 10.0D, 12.0D);
    protected static final VoxelShape SHORT_NORTH_ARM_AABB = Block.box(6.0D, 6.0D, 4.0D, 10.0D, 10.0D, 16.0D);
    protected static final VoxelShape SHORT_EAST_ARM_AABB = Block.box(0.0D, 6.0D, 6.0D, 12.0D, 10.0D, 10.0D);
    protected static final VoxelShape SHORT_WEST_ARM_AABB = Block.box(4.0D, 6.0D, 6.0D, 16.0D, 10.0D, 10.0D);
    private static final VoxelShape[] SHAPES_SHORT = makeShapes(true);
    private static final VoxelShape[] SHAPES_LONG = makeShapes(false);

    private static VoxelShape[] makeShapes(boolean p_60313_) {
        return Arrays.stream(Direction.values()).map((p_60316_) -> calculateShape(p_60316_, p_60313_)).toArray(VoxelShape[]::new);
    }

    private static VoxelShape calculateShape(Direction p_60310_, boolean p_60311_) {
        return switch (p_60310_) {
            default -> Shapes.or(DOWN_AABB, p_60311_ ? SHORT_DOWN_ARM_AABB : DOWN_ARM_AABB);
            case UP -> Shapes.or(UP_AABB, p_60311_ ? SHORT_UP_ARM_AABB : UP_ARM_AABB);
            case NORTH -> Shapes.or(NORTH_AABB, p_60311_ ? SHORT_NORTH_ARM_AABB : NORTH_ARM_AABB);
            case SOUTH -> Shapes.or(SOUTH_AABB, p_60311_ ? SHORT_SOUTH_ARM_AABB : SOUTH_ARM_AABB);
            case WEST -> Shapes.or(WEST_AABB, p_60311_ ? SHORT_WEST_ARM_AABB : WEST_ARM_AABB);
            case EAST -> Shapes.or(EAST_AABB, p_60311_ ? SHORT_EAST_ARM_AABB : EAST_ARM_AABB);
        };
    }

    public PneumaticPumpHead(BlockBehaviour.Properties p_60259_) {
        super(p_60259_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, PistonType.DEFAULT).setValue(SHORT, Boolean.FALSE));
    }

    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    public VoxelShape getShape(BlockState p_60320_, BlockGetter p_60321_, BlockPos p_60322_, CollisionContext p_60323_) {
        return (p_60320_.getValue(SHORT) ? SHAPES_SHORT : SHAPES_LONG)[p_60320_.getValue(FACING).ordinal()];
    }

    private boolean isFittingBase(BlockState headState, BlockState baseState) {
        boolean isTypeMatch = baseState.is(PNEUMATIC_PUMP_BASE.get());
        boolean isExtended = baseState.getValue(PneumaticPumpBase.EXTENDED);
        boolean isAligned = baseState.getValue(FACING) == headState.getValue(FACING);
        return isTypeMatch && isExtended && isAligned;
    }

    public void playerWillDestroy(Level p_60265_, BlockPos p_60266_, BlockState p_60267_, Player p_60268_) {
        if (!p_60265_.isClientSide && p_60268_.getAbilities().instabuild) {
            BlockPos blockpos = p_60266_.relative(p_60267_.getValue(FACING).getOpposite());
            if (this.isFittingBase(p_60267_, p_60265_.getBlockState(blockpos))) {
                p_60265_.destroyBlock(blockpos, false);
            }
        }

        super.playerWillDestroy(p_60265_, p_60266_, p_60267_, p_60268_);
    }

    public void onRemove(BlockState headState, Level p_60283_, BlockPos p_60284_, BlockState p_60285_, boolean p_60286_) {
        if (!headState.is(p_60285_.getBlock())) {
            super.onRemove(headState, p_60283_, p_60284_, p_60285_, p_60286_);
            BlockPos blockpos = p_60284_.relative(headState.getValue(FACING).getOpposite());
            if (this.isFittingBase(headState, p_60283_.getBlockState(blockpos))) {
                p_60283_.destroyBlock(blockpos, true);
            }

        }
    }

    public BlockState updateShape(BlockState headState, Direction p_60302_, BlockState p_60303_, LevelAccessor accessor, BlockPos p_60305_, BlockPos p_60306_) {
        boolean isAlignedWithPotentialBase = p_60302_.getOpposite() == headState.getValue(FACING);
        return  isAlignedWithPotentialBase && !headState.canSurvive(accessor, p_60305_) ? Blocks.AIR.defaultBlockState() : super.updateShape(headState, p_60302_, p_60303_, accessor, p_60305_, p_60306_);
    }

    //The head-breaking bug is right here
    public boolean canSurvive(BlockState headState, LevelReader levelReader, BlockPos pos) {
        BlockState baseState = levelReader.getBlockState(pos.relative(headState.getValue(FACING).getOpposite()));
        boolean isFittingBase = this.isFittingBase(headState, baseState);
        boolean isMovingPiston = baseState.is(Blocks.MOVING_PISTON);
        boolean isAlignedWithBase = baseState.getValue(FACING) == headState.getValue(FACING);
        return  isFittingBase || isMovingPiston && isAlignedWithBase;
    }

    public void neighborChanged(BlockState p_60275_, Level p_60276_, BlockPos p_60277_, Block p_60278_, BlockPos p_60279_, boolean p_60280_) {
        if (p_60275_.canSurvive(p_60276_, p_60277_)) {
            p_60276_.neighborChanged(p_60277_.relative(p_60275_.getValue(FACING).getOpposite()), p_60278_, p_60279_);
        }

    }

    public ItemStack getCloneItemStack(BlockGetter p_60261_, BlockPos p_60262_, BlockState p_60263_) {
        return new ItemStack(p_60263_.getValue(TYPE) == PistonType.STICKY ? Blocks.STICKY_PISTON : Blocks.PISTON);
    }

    public BlockState rotate(BlockState p_60295_, Rotation p_60296_) {
        return p_60295_.setValue(FACING, p_60296_.rotate(p_60295_.getValue(FACING)));
    }

    public BlockState mirror(BlockState p_60292_, Mirror p_60293_) {
        return p_60292_.rotate(p_60293_.getRotation(p_60292_.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_60308_) {
        p_60308_.add(FACING, TYPE, SHORT);
    }

    public boolean isPathfindable(BlockState p_60270_, BlockGetter p_60271_, BlockPos p_60272_, PathComputationType p_60273_) {
        return false;
    }
}