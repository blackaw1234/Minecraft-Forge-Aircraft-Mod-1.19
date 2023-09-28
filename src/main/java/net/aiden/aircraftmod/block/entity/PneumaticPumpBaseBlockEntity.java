package net.aiden.aircraftmod.block.entity;

import net.aiden.aircraftmod.block.custom.PneumaticPumpBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static net.aiden.aircraftmod.block.custom.PneumaticPumpBase.*;

//TODO: Figure out what the final int parameter of PneumaticPumpBase.triggerEvent() does.

public class PneumaticPumpBaseBlockEntity extends BlockEntity {

    public PneumaticPumpBaseBlockEntity(BlockPos p_155229_, BlockState p_155230_) {
        super(ModBlockEntities.PNEUMATIC_PUMP_BASE.get(), p_155229_, p_155230_);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PneumaticPumpBaseBlockEntity pEntity) {
        if(level.isClientSide()) {
            return;
        }

        int direction = 7;
        PneumaticPumpBase base = (PneumaticPumpBase) level.getBlockState(pos).getBlock();

        switch(state.getValue(FACING)) {
            case DOWN:
                direction = 0;
                break;
            case UP:
                direction = 1;
                break;
            case NORTH:
                direction = 2;
                break;
            case SOUTH:
                direction = 3;
                break;
            case WEST:
                direction = 4;
                break;
            case EAST:
                direction = 5;
                break;
        }

        if(base.isOpposed(level, pos, state) && state.getValue(EXTENDED) == Boolean.valueOf(true)) {
            base.triggerEvent(state, level, pos, TRIGGER_CONTRACT, direction);
        }
    }
}
