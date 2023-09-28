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

    public AirPumpStructureResolver(Level level, BlockPos basePos, Direction pushDirection) {
        this.level = level;
        this.basePos = basePos;
        this.pushDirection = pushDirection;
        this.headPos = basePos.relative(pushDirection);
    }

    /**
     * Checks whether the pump can push the block in front of its base.
     * Also handles destruction of a block that will be destroyed as a consequence.
     *
     * @return true if the pump can push the blocks in front of its base, false otherwise
     */
    public boolean isCanPush() {
        BlockState pushCandidateState = level.getBlockState(headPos);

        // Clear the destroy array
        toDestroy.clear();

        // Check if the block in front of the pump base will be destroyed by motion
        if (AirPumpBaseBlock.isNotPushable(pushCandidateState, level, headPos, pushDirection, false)) {
            if (pushCandidateState.getPistonPushReaction() == PushReaction.DESTROY) {
                toDestroy.add(headPos); // If so, add it to the list for destruction
                return true; // The structure resolves
            } else {
                return false;
            }
        } else return addBlockLine(headPos, pushDirection);
    }

    private boolean addBlockLine(BlockPos movingBlockPos, Direction pushDirection) {
        BlockState movingBlockState = level.getBlockState(movingBlockPos);

        if (level.isEmptyBlock(movingBlockPos)) {
            return true; //if the block is air
        } else if (AirPumpBaseBlock.isNotPushable(movingBlockState, level, movingBlockPos, pushDirection, false)) {
            return true; //if the block should break?
        } else if (movingBlockPos.equals(basePos)) {
            return true; //if the pump is extending
        } else {
            if (!PistonBaseBlock.isPushable(movingBlockState, level, movingBlockPos, pushDirection, true, pushDirection) || movingBlockPos.equals(basePos)) {
                return false;
            }

            if (movingBlockState.getPistonPushReaction() == PushReaction.DESTROY) {
                toDestroy.add(movingBlockPos);
                return true;
            }

            return false;
        }
    }

    public List<BlockPos> getToDestroy() {
        return toDestroy;
    }
}
