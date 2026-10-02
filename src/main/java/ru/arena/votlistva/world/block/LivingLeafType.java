package ru.arena.votlistva.world.block;

import javax.annotation.Nullable;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Visual species retained when a vanilla leaf is converted into a living leaf.
 */
public enum LivingLeafType implements StringRepresentable {
    OAK("oak", Blocks.OAK_LEAVES),
    SPRUCE("spruce", Blocks.SPRUCE_LEAVES),
    BIRCH("birch", Blocks.BIRCH_LEAVES),
    JUNGLE("jungle", Blocks.JUNGLE_LEAVES),
    ACACIA("acacia", Blocks.ACACIA_LEAVES),
    DARK_OAK("dark_oak", Blocks.DARK_OAK_LEAVES),
    MANGROVE("mangrove", Blocks.MANGROVE_LEAVES),
    CHERRY("cherry", Blocks.CHERRY_LEAVES),
    AZALEA("azalea", Blocks.AZALEA_LEAVES),
    FLOWERING_AZALEA("flowering_azalea", Blocks.FLOWERING_AZALEA_LEAVES),
    OTHER("other", Blocks.OAK_LEAVES);

    private final String serializedName;
    private final Block vanillaBlock;

    LivingLeafType(String serializedName, Block vanillaBlock) {
        this.serializedName = serializedName;
        this.vanillaBlock = vanillaBlock;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    /** The vanilla leaf block this species is copied from. */
    public Block sourceBlock() {
        return vanillaBlock;
    }

    /**
     * @return the species of {@code block}, or {@code null} when {@code block}
     *         is not one of the vanilla leaf blocks this mod takes over.
     */
    @Nullable
    public static LivingLeafType fromVanillaBlock(Block block) {
        for (LivingLeafType type : values()) {
            if (type != OTHER && type.vanillaBlock == block) {
                return type;
            }
        }
        return null;
    }

    /**
     * {@code true} only for the vanilla leaf blocks the mod converts. Leaves
     * added by other mods keep their own block and are left alone.
     */
    public static boolean isVanillaLeaf(Block block) {
        return fromVanillaBlock(block) != null;
    }
}
