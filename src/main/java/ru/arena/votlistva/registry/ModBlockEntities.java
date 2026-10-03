package ru.arena.votlistva.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.arena.votlistva.VotListva;
import ru.arena.votlistva.world.blockentity.LivingLeafBlockEntity;
import ru.arena.votlistva.world.blockentity.LivingLogBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, VotListva.MOD_ID);

    public static final RegistryObject<BlockEntityType<LivingLeafBlockEntity>> LIVING_LEAF =
            BLOCK_ENTITY_TYPES.register(
                    "living_leaf",
                    () -> BlockEntityType.Builder.of(
                            LivingLeafBlockEntity::new,
                            ModBlocks.LIVING_LEAF.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<LivingLogBlockEntity>> LIVING_LOG =
            BLOCK_ENTITY_TYPES.register(
                    "living_log",
                    () -> BlockEntityType.Builder.of(
                            LivingLogBlockEntity::new,
                            ModBlocks.LIVING_LOG.get()
                    ).build(null)
            );

    private ModBlockEntities() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
