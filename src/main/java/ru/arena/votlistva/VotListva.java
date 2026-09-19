package ru.arena.votlistva;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ru.arena.votlistva.registry.ModBlockEntities;
import ru.arena.votlistva.registry.ModBlocks;
import ru.arena.votlistva.registry.ModItems;

/**
 * Entry point for the Living Leaves mod.
 */
@Mod(VotListva.MOD_ID)
public final class VotListva {
    public static final String MOD_ID = "votlistva";

    public VotListva() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModItems.register(modBus);
    }
}
