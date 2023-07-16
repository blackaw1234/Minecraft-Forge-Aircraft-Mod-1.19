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
        //clear the destroy array
        this.toDestroy.clear();

        BlockState blockstate = this.level.getBlockState(this.startPos); //blockstate represents the block to be pushed
        if (!PneumaticPumpBase.isPushable(blockstate, this.level, this.startPos, this.pushDirection, false, this.pistonDirection)) {
            if (this.extending && blockstate.getPistonPushReaction() == PushReaction.DESTROY) {
                this.toDestroy.add(this.startPos);
                return true;
            } else {
                return false;
            }
        } else if (!this.addBlockLine(this.startPos, this.pushDirection)) return false;
        else return true;
    }

    private boolean addBlockLine(BlockPos movingBlockPos, Direction pushDirection) {
        BlockState blockstate = this.level.getBlockState(movingBlockPos);
        if (level.isEmptyBlock(movingBlockPos)) {
            return true; //if the block is air
        } else if (!PneumaticPumpBase.isPushable(blockstate, this.level, movingBlockPos, this.pushDirection, false, pushDirection)) {
            return true; //if the block should break?
        } else if (movingBlockPos.equals(this.pumpPos)) {
            return true; //if the pump is already extended?
        } else {
            if (!PistonBaseBlock.isPushable(blockstate, this.level, movingBlockPos, this.pushDirection, true, this.pushDirection) || movingBlockPos.equals(this.pumpPos)) {
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
