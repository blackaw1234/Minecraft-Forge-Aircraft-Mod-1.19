package net.aiden.aircraftmod.block.entity;

import net.aiden.aircraftmod.block.custom.AirPumpBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static net.aiden.aircraftmod.block.custom.AirPumpBaseBlock.*;
import static net.minecraft.world.level.block.piston.PistonBaseBlock.TRIGGER_CONTRACT;

/**
 * Block entity associated with air pump base. Allows the level
 * to check for an opponent piston every tick
 *
 * @author Aiden Black
 */
public class AirPumpBaseBlockEntity extends BlockEntity {
    /**
     * Constructs an AirPumpBaseBlockEntity object.
     * @param basePos pump base's location
     * @param baseState pump base's BlockState
     */
    public AirPumpBaseBlockEntity(BlockPos basePos, BlockState baseState) {
        super(ModBlockEntities.AIR_PUMP_BASE.get(), basePos, baseState);
    }

    /**
     * Checks if the pump should be extended every tick.
     *
     * @param level spatial and network context
     * @param basePos pump base's location
     * @param baseState pump base's BlockState
     * @param pEntity unused parameter
     */
    public static void tick(Level level, BlockPos basePos, BlockState baseState, AirPumpBaseBlockEntity pEntity) {
        if(level.isClientSide()) {
            return;
        }

        int direction;
        AirPumpBaseBlock base = (AirPumpBaseBlock) level.getBlockState(basePos).getBlock();

        direction = switch (baseState.getValue(FACING)) {
            case DOWN -> 0;
            case UP -> 1;
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
        };

        base.checkIfExtend(level, basePos, baseState);
    }
}
