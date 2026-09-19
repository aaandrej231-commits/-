package ru.arena.votlistva.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.arena.votlistva.VotListva;
import ru.arena.votlistva.world.block.LivingLeafBlock;
import ru.arena.votlistva.world.block.LivingLogBlock;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, VotListva.MOD_ID);

    public static final RegistryObject<LivingLeafBlock> LIVING_LEAF = BLOCKS.register(
            "living_leaf",
            () -> new LivingLeafBlock(Block.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.2F)
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .randomTicks())
    );

    public static final RegistryObject<LivingLogBlock> LIVING_LOG = BLOCKS.register(
            "living_log",
            () -> new LivingLogBlock(Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .randomTicks())
    );

    private ModBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
