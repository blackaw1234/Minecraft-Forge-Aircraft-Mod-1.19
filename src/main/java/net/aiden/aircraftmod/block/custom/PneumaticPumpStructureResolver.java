package net.aiden.aircraftmod.block.custom;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class PneumaticPumpStructureResolver {
    private final Level level;
    private final boolean extending;
    private final BlockPos startPos;
    private final BlockPos pumpPos;
    private final Direction pushDirection;
    private final List<BlockPos> toPush = Lists.newArrayList();
    private final List<BlockPos> toDestroy = Lists.newArrayList();
    private final Direction pistonDirection;

    public PneumaticPumpStructureResolver(Level level, BlockPos inputPumpPos, Direction pushDirection, boolean isExtending) {
        this.level = level;
        this.pistonDirection = pushDirection;
        this.pumpPos = inputPumpPos;
        this.extending = isExtending;
        if (isExtending) {
            this.pushDirection = pushDirection;
            this.startPos = inputPumpPos.relative(pushDirection);
        } else {
            this.pushDirection = pushDirection.getOpposite();
            this.startPos = inputPumpPos.relative(pushDirection, 2);
        }

    }

    public boolean resolve() {
        //clear the push and destroy arrays
        this.toPush.clear();
        this.toDestroy.clear();

        BlockState blockstate = this.level.getBlockState(this.startPos); //blockstate represents the block to be pushed
        if (!PneumaticPumpBase.isPushable(blockstate, this.level, this.startPos, this.pushDirection, false, this.pistonDirection)) {
            if (this.extending && blockstate.getPistonPushReaction() == PushReaction.DESTROY) {
                this.toDestroy.add(this.startPos);
                return true;
            } else {
                return false;
            }
        } else if (!this.addBlockLine(this.startPos, this.pushDirection)) {
            return false;
        } else {
            for(int i = 0; i < this.toPush.size(); ++i) {
                BlockPos blockpos = this.toPush.get(i);
                if (this.level.getBlockState(blockpos).isStickyBlock() && !this.addBranchingBlocks(blockpos)) {
                    return false;
                }
            }

            return true;
        }
    }

    private boolean addBlockLine(BlockPos movingBlockPos, Direction pushDirection) {
        BlockState blockstate = this.level.getBlockState(movingBlockPos);
        if (level.isEmptyBlock(movingBlockPos)) {
            return true;
        } else if (!PneumaticPumpBase.isPushable(blockstate, this.level, movingBlockPos, this.pushDirection, false, pushDirection)) {
            return true; //If the block should break or can't be pushed
        } else if (movingBlockPos.equals(this.pumpPos)) {
            return true;
        } else if (this.toPush.contains(movingBlockPos)) {
            return true;
        } else {
            if (this.toPush.size() > 0) {
                return false;
            } else {
                this.toPush.add(movingBlockPos.relative(this.pushDirection.getOpposite(), 0));

                int l = 1;
                int j1 = 1;

                while(true) {
                    BlockPos blockpos1 = movingBlockPos.relative(this.pushDirection, j1);
                    int j = this.toPush.indexOf(blockpos1);
                    if (j > -1) {
                        this.reorderListAtCollision(l, j);

                        for(int k = 0; k <= j + l; ++k) {
                            BlockPos blockpos2 = this.toPush.get(k);
                            if (this.level.getBlockState(blockpos2).isStickyBlock() && !this.addBranchingBlocks(blockpos2)) {
                                return false;
                            }
                        }

                        return true;
                    }

                    blockstate = this.level.getBlockState(blockpos1);
                    if (blockstate.isAir()) {
                        return true;
                    }

                    if (!PistonBaseBlock.isPushable(blockstate, this.level, blockpos1, this.pushDirection, true, this.pushDirection) || blockpos1.equals(this.pumpPos)) {
                        return false;
                    }

                    if (blockstate.getPistonPushReaction() == PushReaction.DESTROY) {
                        this.toDestroy.add(blockpos1);
                        return true;
                    }

                    if (this.toPush.size() > 0) {
                        return false;
                    }

                    this.toPush.add(blockpos1);
                    ++l;
                    ++j1;
                }
            }
        }
    }

    private void reorderListAtCollision(int p_60424_, int p_60425_) {
        List<BlockPos> list = Lists.newArrayList();
        List<BlockPos> list1 = Lists.newArrayList();
        List<BlockPos> list2 = Lists.newArrayList();
        list.addAll(this.toPush.subList(0, p_60425_));
        list1.addAll(this.toPush.subList(this.toPush.size() - p_60424_, this.toPush.size()));
        list2.addAll(this.toPush.subList(p_60425_, this.toPush.size() - p_60424_));
        this.toPush.clear();
        this.toPush.addAll(list);
        this.toPush.addAll(list1);
        this.toPush.addAll(list2);
    }

    private boolean addBranchingBlocks(BlockPos p_60432_) {
        BlockState blockstate = this.level.getBlockState(p_60432_);

        for(Direction direction : Direction.values()) {
            if (direction.getAxis() != this.pushDirection.getAxis()) {
                BlockPos blockpos = p_60432_.relative(direction);
                BlockState blockstate1 = this.level.getBlockState(blockpos);
                if (blockstate1.canStickTo(blockstate) && blockstate.canStickTo(blockstate1) && !this.addBlockLine(blockpos,direction)) {
                    return false;
                }
            }
        }

        return true;
    }

    public Direction getPushDirection() {
        return this.pushDirection;
    }

    public List<BlockPos> getToPush() {
        return this.toPush;
    }

    public List<BlockPos> getToDestroy() {
        return this.toDestroy;
    }
}
