package net.aiden.aircraftmod.block.custom;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class AirPumpStructureResolver {
    /** spatial and network context of the air pump */
    private final Level level;
    private final BlockPos headPos;
    private final BlockPos basePos;
    private final Direction pushDirection;
    private final List<BlockPos> toDestroy = Lists.newArrayList();
    private final Direction pistonDirection;

    public AirPumpStructureResolver(Level level, BlockPos basePos, Direction pushDirection) {
        this.level = level;
        this.pistonDirection = pushDirection;
        this.basePos = basePos;
        this.pushDirection = pushDirection;
        this.headPos = basePos.relative(pushDirection);
    }

    public boolean resolve() {
        //clear the destroy array
        this.toDestroy.clear();

        BlockState headState = this.level.getBlockState(this.headPos); //blockstate represents the block to be pushed
        if (AirPumpBaseBlock.isNotPushable(headState, this.level, this.headPos, this.pushDirection, false, this.pistonDirection)) {
            if (headState.getPistonPushReaction() == PushReaction.DESTROY) {
                this.toDestroy.add(this.headPos);
                return true;
            } else {
                return false;
            }
        } else if (!this.addBlockLine(this.headPos, this.pushDirection)) return false;
        else return true;
    }

    private boolean addBlockLine(BlockPos movingBlockPos, Direction pushDirection) {
        BlockState blockstate = this.level.getBlockState(movingBlockPos);
        if (level.isEmptyBlock(movingBlockPos)) {
            return true; //if the block is air
        } else if (AirPumpBaseBlock.isNotPushable(blockstate, this.level, movingBlockPos, this.pushDirection, false, pushDirection)) {
            return true; //if the block should break?
        } else if (movingBlockPos.equals(this.basePos)) {
            return true; //if the pump is already extended?
        } else {
            if (!PistonBaseBlock.isPushable(blockstate, this.level, movingBlockPos, this.pushDirection, true, this.pushDirection) || movingBlockPos.equals(this.basePos)) {
                return false;
            }

            if (blockstate.getPistonPushReaction() == PushReaction.DESTROY) {
                this.toDestroy.add(movingBlockPos);
                return true;
            }

            return false;
        }
    }

    public List<BlockPos> getToDestroy() {
        return this.toDestroy;
    }
}
