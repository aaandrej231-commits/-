package ru.arena.votlistva.world.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import ru.arena.votlistva.world.blockentity.LivingLeafBlockEntity;

/** A leaf block whose block entity contains its genes and energy. */
public class LivingLeafBlock extends LeavesBlock implements EntityBlock {
    public static final EnumProperty<LivingLeafType> LEAF_TYPE =
            EnumProperty.create("leaf_type", LivingLeafType.class);

    public LivingLeafBlock(BlockBehaviour.Properties properties) {
        super(properties);
        // Vanilla LeavesBlock starts at distance 7; keep the same default so a
        // freshly placed block is not treated as if it were glued to a log.
        registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.DISTANCE, 7)
                .setValue(LEAF_TYPE, LivingLeafType.OAK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEAF_TYPE);
    }

    /** The ecology replaces vanilla leaf decay, but still uses random ticks. */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LivingLeafBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LivingLeafBlockEntity leaf) {
            leaf.initializeNew(level.random);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof LivingLeafBlockEntity leaf) {
            leaf.onRandomTick(level, pos, random);
        }
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LivingLeafBlockEntity leaf) {
            String roleKey = leaf.isPredator()
                    ? "votlistva.role.predator"
                    : "votlistva.role.solar";
            ChatFormatting roleColor = leaf.isPredator()
                    ? ChatFormatting.RED
                    : ChatFormatting.GREEN;

            player.sendSystemMessage(
                    Component.translatable("votlistva.inspect", leaf.getSpeed(), leaf.getEnergy())
                            .withStyle(ChatFormatting.GRAY)
            );
            player.sendSystemMessage(
                    Component.translatable(roleKey).withStyle(roleColor)
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
