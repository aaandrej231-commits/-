package ru.arena.votlistva.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.arena.votlistva.VotListva;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, VotListva.MOD_ID);

    public static final RegistryObject<Item> LIVING_LEAF = ITEMS.register(
            "living_leaf",
            () -> new BlockItem(ModBlocks.LIVING_LEAF.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> LIVING_LOG = ITEMS.register(
            "living_log",
            () -> new BlockItem(ModBlocks.LIVING_LOG.get(), new Item.Properties())
    );

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS)) {
            // RegistryObject is a supplier, not an ItemLike: resolve it here.
            event.accept(LIVING_LEAF.get());
            event.accept(LIVING_LOG.get());
        }
    }
}
