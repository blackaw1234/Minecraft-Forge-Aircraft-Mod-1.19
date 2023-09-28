package net.aiden.aircraftmod.block.entity;

import net.aiden.aircraftmod.AircraftMod;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.aiden.aircraftmod.block.ModBlocks;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AircraftMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<PneumaticPumpBaseBlockEntity>> PNEUMATIC_PUMP_BASE =
            BLOCK_ENTITIES.register("pneumatic_pump_base", () ->
                    BlockEntityType.Builder.of(PneumaticPumpBaseBlockEntity::new,
                            ModBlocks.PNEUMATIC_PUMP_BASE.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
