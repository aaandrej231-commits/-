package ru.arena.votlistva.events;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.arena.votlistva.VotListva;
import ru.arena.votlistva.registry.ModBlocks;
import ru.arena.votlistva.world.block.LivingLeafBlock;
import ru.arena.votlistva.world.block.LivingLeafType;
import ru.arena.votlistva.world.block.LivingLogBlock;
import ru.arena.votlistva.world.block.LivingLogType;
import ru.arena.votlistva.world.blockentity.LivingLeafBlockEntity;
import ru.arena.votlistva.world.blockentity.LivingLogBlockEntity;

/**
 * Brings naturally generated vanilla trees into the ecology. Vanilla leaves
 * and logs are static blocks, so they are converted as soon as their chunk is
 * loaded and rescanned on a round-robin pass to catch newly grown trees.
 *
 * <p>Only the vanilla blocks listed by {@link LivingLeafType} and
 * {@link LivingLogType} are converted. Leaves and logs added by other mods keep
 * their own block, loot table and behaviour.</p>
 */
@Mod.EventBusSubscriber(modid = VotListva.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldEcologyEvents {
    private static final long RESCAN_INTERVAL_TICKS = 20L;

    /**
     * How many chunks are rescanned per interval. Rescanning every loaded chunk
     * of a large world every second is far too expensive, so the pass is spread
     * over several intervals instead. A full pass therefore takes
     * {@code loadedChunks / MAX_CHUNKS_PER_RESCAN} seconds.
     */
    private static final int MAX_CHUNKS_PER_RESCAN = 32;

    /** The vanilla leaf and log/stem blocks the mod is allowed to take over. */
    private static final Set<Block> VANILLA_ECOLOGY_BLOCKS = collectVanillaEcologyBlocks();

    /**
     * Loaded chunks of every live level. The set is insertion ordered, which
     * turns the periodic rescan into a round-robin, and the weak level key
     * prevents leaking unloaded levels.
     */
    private static final Map<ServerLevel, LinkedHashSet<LevelChunk>> LOADED_CHUNKS =
            new WeakHashMap<>();

    private WorldEcologyEvents() {
    }

    private static Set<Block> collectVanillaEcologyBlocks() {
        Set<Block> blocks = new HashSet<>();
        for (LivingLeafType type : LivingLeafType.values()) {
            if (type != LivingLeafType.OTHER) {
                blocks.add(type.sourceBlock());
            }
        }
        for (LivingLogType type : LivingLogType.values()) {
            if (type != LivingLogType.OTHER) {
                blocks.add(type.sourceBlock());
            }
        }
        return Set.copyOf(blocks);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        LOADED_CHUNKS.computeIfAbsent(level, ignored -> new LinkedHashSet<>()).add(chunk);
        scanChunk(level, chunk);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        LinkedHashSet<LevelChunk> chunks = LOADED_CHUNKS.get(level);
        if (chunks != null) {
            chunks.remove(chunk);
            if (chunks.isEmpty()) {
                LOADED_CHUNKS.remove(level);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)
                || level.getGameTime() % RESCAN_INTERVAL_TICKS != 0L) {
            return;
        }

        LinkedHashSet<LevelChunk> chunks = LOADED_CHUNKS.get(level);
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        // Detach the next batch before scanning: a scan can load neighbour
        // chunks, which would otherwise invalidate the iterator below.
        List<LevelChunk> batch = new ArrayList<>(MAX_CHUNKS_PER_RESCAN);
        Iterator<LevelChunk> iterator = chunks.iterator();
        for (int i = 0; i < MAX_CHUNKS_PER_RESCAN && iterator.hasNext(); i++) {
            batch.add(iterator.next());
            iterator.remove();
        }

        for (LevelChunk chunk : batch) {
            chunks.add(chunk); // back of the rotation: round-robin over the level
            scanChunk(level, chunk);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LOADED_CHUNKS.clear();
    }

    private static void scanChunk(ServerLevel level, LevelChunk chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        int minBlockX = chunk.getPos().getMinBlockX();
        int minBlockZ = chunk.getPos().getMinBlockZ();
        int minBlockY = level.getMinBuildHeight();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir() || !section.maybeHas(WorldEcologyEvents::isVanillaEcologyBlock)) {
                continue;
            }

            int sectionMinY = minBlockY + sectionIndex * 16;
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        BlockState state = section.getBlockState(localX, localY, localZ);
                        if (!isVanillaEcologyBlock(state)) {
                            continue;
                        }

                        BlockPos pos = new BlockPos(
                                minBlockX + localX,
                                sectionMinY + localY,
                                minBlockZ + localZ
                        );
                        convert(level, pos, state);
                    }
                }
            }
        }
    }

    private static boolean isVanillaEcologyBlock(BlockState state) {
        return VANILLA_ECOLOGY_BLOCKS.contains(state.getBlock());
    }

    private static void convert(ServerLevel level, BlockPos pos, BlockState source) {
        Block sourceBlock = source.getBlock();

        LivingLeafType leafType = LivingLeafType.fromVanillaBlock(sourceBlock);
        if (leafType != null) {
            BlockState target = ModBlocks.LIVING_LEAF.get().defaultBlockState()
                    .setValue(LivingLeafBlock.LEAF_TYPE, leafType);
            target = copyLeafProperties(source, target);

            if (level.setBlock(pos, target, 3)
                    && level.getBlockEntity(pos) instanceof LivingLeafBlockEntity leaf) {
                leaf.initializeNew(level.random);
            }
            return;
        }

        LivingLogType logType = LivingLogType.fromVanillaBlock(sourceBlock);
        if (logType != null) {
            BlockState target = ModBlocks.LIVING_LOG.get().defaultBlockState()
                    .setValue(LivingLogBlock.LOG_TYPE, logType);
            if (source.hasProperty(BlockStateProperties.AXIS)
                    && target.hasProperty(BlockStateProperties.AXIS)) {
                target = target.setValue(BlockStateProperties.AXIS,
                        source.getValue(BlockStateProperties.AXIS));
            }

            if (level.setBlock(pos, target, 3)
                    && level.getBlockEntity(pos) instanceof LivingLogBlockEntity log) {
                log.initializeNew(level.random);
            }
        }
    }

    private static BlockState copyLeafProperties(BlockState source, BlockState target) {
        if (source.hasProperty(BlockStateProperties.DISTANCE)
                && target.hasProperty(BlockStateProperties.DISTANCE)) {
            target = target.setValue(BlockStateProperties.DISTANCE,
                    source.getValue(BlockStateProperties.DISTANCE));
        }
        if (source.hasProperty(BlockStateProperties.PERSISTENT)
                && target.hasProperty(BlockStateProperties.PERSISTENT)) {
            target = target.setValue(BlockStateProperties.PERSISTENT,
                    source.getValue(BlockStateProperties.PERSISTENT));
        }
        if (source.hasProperty(BlockStateProperties.WATERLOGGED)
                && target.hasProperty(BlockStateProperties.WATERLOGGED)) {
            target = target.setValue(BlockStateProperties.WATERLOGGED,
                    source.getValue(BlockStateProperties.WATERLOGGED));
        }
        return target;
    }
}
