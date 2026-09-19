package ru.arena.votlistva.world.block;

import javax.annotation.Nullable;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Visual species and sapling produced by a living log. */
public enum LivingLogType implements StringRepresentable {
    OAK("oak", Blocks.OAK_LOG, Blocks.OAK_SAPLING),
    SPRUCE("spruce", Blocks.SPRUCE_LOG, Blocks.SPRUCE_SAPLING),
    BIRCH("birch", Blocks.BIRCH_LOG, Blocks.BIRCH_SAPLING),
    JUNGLE("jungle", Blocks.JUNGLE_LOG, Blocks.JUNGLE_SAPLING),
    ACACIA("acacia", Blocks.ACACIA_LOG, Blocks.ACACIA_SAPLING),
    DARK_OAK("dark_oak", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_SAPLING),
    MANGROVE("mangrove", Blocks.MANGROVE_LOG, Blocks.MANGROVE_PROPAGULE),
    CHERRY("cherry", Blocks.CHERRY_LOG, Blocks.CHERRY_SAPLING),
    CRIMSON("crimson", Blocks.CRIMSON_STEM, null),
    WARPED("warped", Blocks.WARPED_STEM, null),
    OTHER("other", Blocks.OAK_LOG, Blocks.OAK_SAPLING);

    private final String serializedName;
    private final Block vanillaBlock;
    @Nullable
    private final Block sapling;

    LivingLogType(String serializedName, Block vanillaBlock, @Nullable Block sapling) {
        this.serializedName = serializedName;
        this.vanillaBlock = vanillaBlock;
        this.sapling = sapling;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    @Nullable
    public Block sapling() {
        return sapling;
    }

    public static LivingLogType fromVanillaBlock(Block block) {
        if (block == Blocks.OAK_LOG || block == Blocks.STRIPPED_OAK_LOG) {
            return OAK;
        }
        if (block == Blocks.SPRUCE_LOG || block == Blocks.STRIPPED_SPRUCE_LOG) {
            return SPRUCE;
        }
        if (block == Blocks.BIRCH_LOG || block == Blocks.STRIPPED_BIRCH_LOG) {
            return BIRCH;
        }
        if (block == Blocks.JUNGLE_LOG || block == Blocks.STRIPPED_JUNGLE_LOG) {
            return JUNGLE;
        }
        if (block == Blocks.ACACIA_LOG || block == Blocks.STRIPPED_ACACIA_LOG) {
            return ACACIA;
        }
        if (block == Blocks.DARK_OAK_LOG || block == Blocks.STRIPPED_DARK_OAK_LOG) {
            return DARK_OAK;
        }
        if (block == Blocks.MANGROVE_LOG || block == Blocks.STRIPPED_MANGROVE_LOG) {
            return MANGROVE;
        }
        if (block == Blocks.CHERRY_LOG || block == Blocks.STRIPPED_CHERRY_LOG) {
            return CHERRY;
        }
        if (block == Blocks.CRIMSON_STEM || block == Blocks.STRIPPED_CRIMSON_STEM) {
            return CRIMSON;
        }
        if (block == Blocks.WARPED_STEM || block == Blocks.STRIPPED_WARPED_STEM) {
            return WARPED;
        }
        return OTHER;
    }
}
