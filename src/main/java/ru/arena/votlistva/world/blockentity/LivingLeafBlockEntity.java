package ru.arena.votlistva.world.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import ru.arena.votlistva.registry.ModBlockEntities;

/**
 * Stores the two heritable values of one living leaf.
 *
 * <p>A block entity is used because speed and energy are individual to every
 * block, while putting them in a block state would make the number of block
 * states unnecessarily large.</p>
 */
public class LivingLeafBlockEntity extends BlockEntity {
    public static final float MIN_SPEED = -1.0F;
    public static final float MAX_SPEED = 1.0F;
    public static final float MIN_ENERGY = 0.0F;
    public static final float MAX_ENERGY = 2.0F;

    /** Energy removed from every leaf on each random tick. */
    public static final float METABOLIC_COST = 0.1F;
    /** Maximum gene mutation applied to a child in either direction. */
    public static final float MAX_MUTATION = 0.5F;
    /** A predator can take this much per random tick at speed -1. */
    public static final float MAX_STEAL_PER_TICK = 0.1F;

    private static final String SPEED_TAG = "Speed";
    private static final String ENERGY_TAG = "Energy";

    protected float speed;
    protected float energy;

    public LivingLeafBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.LIVING_LEAF.get(), pos, state);
    }

    protected LivingLeafBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Gives a newly placed leaf a random gene and a usable starting energy.
     */
    public void initializeNew(RandomSource random) {
        initialize(Mth.lerp(random.nextFloat(), MIN_SPEED, MAX_SPEED), 1.0F);
    }

    public void initialize(float newSpeed, float newEnergy) {
        this.speed = clampSpeed(newSpeed);
        this.energy = clampEnergy(newEnergy);
        setChanged();
    }

    public float getSpeed() {
        return speed;
    }

    public float getEnergy() {
        return energy;
    }

    /** Negative speed is a predator; non-negative speed is a solar leaf. */
    public boolean isPredator() {
        return speed < 0.0F;
    }

    public boolean isSolarLeaf() {
        return !isPredator();
    }

    /**
     * Called by the block's random tick. There is intentionally no ordinary
     * block-entity ticker: the gamerule randomTickSpeed controls the ecology.
     */
    public void onRandomTick(ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) != this) {
            return;
        }

        // Every organism pays its maintenance cost first.
        energy -= METABOLIC_COST;

        // The signed gene is the sunlight response. Solar leaves gain energy;
        // predators receive the same value as damage because their speed is -.
        energy += speed * sunlight(level, pos);

        if (energy < MIN_ENERGY) {
            die(level, pos);
            return;
        }

        if (isPredator()) {
            stealFromAdjacentSolarLeaf(level, pos);
        }

        if (energy > MAX_ENERGY) {
            reproduce(level, pos, random);
        } else {
            setChanged();
        }
    }

    /**
     * Returns the current sky/sun intensity in the range 0..1. A roof blocks
     * sunlight completely; night and weather lower the value through
     * Minecraft's sky-darkness value.
     *
     * <p>The sky light value at this exact position is the only occlusion test
     * used here. {@code canSeeSky} must NOT be used as a gate: it is a default
     * method of {@code BlockAndTintGetter} that requires the sky light to be at
     * the maximum (15), not merely non-zero. Leaves are light-filtering blocks
     * that reduce sky light by 1 per block of thickness, so every leaf with at
     * least one leaf above it in its column reads 14 and would report
     * {@code canSeeSky == false}. Gating on it returned sunlight 0 for the whole
     * inside of a canopy: those leaves gained nothing and only paid the
     * metabolic cost, so they died instead of gaining energy.</p>
     */
    protected static float sunlight(Level level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight()) {
            return 0.0F;
        }

        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        float intensity = (skyLight - level.getSkyDarken()) / 15.0F;
        return Mth.clamp(intensity, 0.0F, 1.0F);
    }

    /**
     * Predators do not receive energy from the sun. They take it directly from
     * adjacent solar leaves, preserving the total energy of both blocks.
     */
    private void stealFromAdjacentSolarLeaf(ServerLevel level, BlockPos pos) {
        float room = MAX_ENERGY - energy;
        float toSteal = Math.min(room, Math.abs(speed) * MAX_STEAL_PER_TICK);
        if (toSteal <= 0.0F) {
            return;
        }

        for (Direction direction : Direction.values()) {
            if (toSteal <= 0.0F) {
                break;
            }

            BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
            if (!(neighbour instanceof LivingLeafBlockEntity solar) || !solar.isSolarLeaf()) {
                continue;
            }

            float taken = Math.min(toSteal, solar.energy);
            if (taken <= 0.0F) {
                continue;
            }

            solar.energy -= taken;
            energy += taken;
            solar.setChanged();
            toSteal -= taken;
        }
    }

    /**
     * Splits the available energy between parent and child. This makes
     * reproduction energy-neutral instead of creating energy out of nowhere.
     */
    protected void reproduce(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos childPos = findEmptyNeighbour(level, pos);
        if (childPos == null) {
            burn(level, pos);
            return;
        }

        float childEnergy = energy * 0.5F;
        float childSpeed = mutatedSpeed(speed, random);

        // The child is placed into an empty block, so a waterlogged parent must
        // not hand its "wet" state over to a position that holds no water.
        BlockState childState = getBlockState();
        if (childState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            childState = childState.setValue(BlockStateProperties.WATERLOGGED, false);
        }

        if (!level.setBlock(childPos, childState, 3)) {
            burn(level, pos);
            return;
        }

        BlockEntity childEntity = level.getBlockEntity(childPos);
        if (!(childEntity instanceof LivingLeafBlockEntity child)) {
            level.removeBlock(childPos, false);
            burn(level, pos);
            return;
        }

        energy = childEnergy;
        child.initialize(childSpeed, childEnergy);
        setChanged();

        level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.45F, 1.15F);
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                childPos.getX() + 0.5D,
                childPos.getY() + 0.6D,
                childPos.getZ() + 0.5D,
                4,
                0.25D,
                0.25D,
                0.25D,
                0.02D
        );
    }

    protected static BlockPos findEmptyNeighbour(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos candidate = pos.relative(direction);
            if (level.isEmptyBlock(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    protected void die(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.35F, 0.8F);
        level.sendParticles(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                3,
                0.2D,
                0.2D,
                0.2D,
                0.01D
        );
    }

    protected void burn(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.0F);
        level.sendParticles(
                ParticleTypes.FLAME,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                8,
                0.3D,
                0.3D,
                0.3D,
                0.02D
        );
    }

    protected static float mutatedSpeed(float parentSpeed, RandomSource random) {
        return clampSpeed(parentSpeed + Mth.lerp(random.nextFloat(), -MAX_MUTATION, MAX_MUTATION));
    }

    protected static float clampSpeed(float value) {
        return Mth.clamp(value, MIN_SPEED, MAX_SPEED);
    }

    private static float clampEnergy(float value) {
        return Mth.clamp(value, MIN_ENERGY, MAX_ENERGY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat(SPEED_TAG, speed);
        tag.putFloat(ENERGY_TAG, energy);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        speed = clampSpeed(tag.getFloat(SPEED_TAG));
        energy = clampEnergy(tag.getFloat(ENERGY_TAG));
    }
}
