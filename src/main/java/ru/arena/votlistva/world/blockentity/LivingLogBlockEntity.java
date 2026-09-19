package ru.arena.votlistva.world.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ru.arena.votlistva.registry.ModBlockEntities;
import ru.arena.votlistva.world.block.LivingLogBlock;
import ru.arena.votlistva.world.block.LivingLogType;

/**
 * Genetic block entity for a living log. It inherits the exact energy rules of
 * living leaves; only the offspring block is different: a matching sapling.
 */
public class LivingLogBlockEntity extends LivingLeafBlockEntity {
    public LivingLogBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIVING_LOG.get(), pos, state);
    }

    /** A living log creates a matching vanilla sapling instead of a leaf block. */
    @Override
    protected void reproduce(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos childPos = findEmptyNeighbour(level, pos);
        if (childPos == null) {
            burn(level, pos);
            return;
        }

        LivingLogType type = getBlockState().getValue(LivingLogBlock.LOG_TYPE);
        Block sapling = type.sapling();
        if (sapling == null) {
            // Nether stems have no vanilla sapling. They follow the same
            // "cannot reproduce -> burn" rule as a blocked log.
            burn(level, pos);
            return;
        }

        // A vanilla sapling has no place to store the child's genes. The
        // genetic chain resumes if that sapling grows into a new tree.
        if (!level.setBlock(childPos, sapling.defaultBlockState(), 3)) {
            burn(level, pos);
            return;
        }

        energy *= 0.5F;
        setChanged();
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.55F, 1.0F);
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                childPos.getX() + 0.5D,
                childPos.getY() + 0.5D,
                childPos.getZ() + 0.5D,
                4,
                0.25D,
                0.25D,
                0.25D,
                0.02D
        );
    }
}
