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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import ru.arena.votlistva.world.blockentity.LivingLogBlockEntity;

/** A living log with the same genetic energy system as living leaves. */
public class LivingLogBlock extends RotatedPillarBlock implements EntityBlock {
    public static final EnumProperty<LivingLogType> LOG_TYPE =
            EnumProperty.create("log_type", LivingLogType.class);

    public LivingLogBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.Y)
                .setValue(LOG_TYPE, LivingLogType.OAK));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LOG_TYPE);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LivingLogBlockEntity(pos, state);
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

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LivingLogBlockEntity log) {
            log.initializeNew(level.random);
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof LivingLogBlockEntity log) {
            log.onRandomTick(level, pos, random);
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
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LivingLogBlockEntity log) {
            player.sendSystemMessage(
                    Component.translatable("votlistva.inspect", log.getSpeed(), log.getEnergy())
                            .withStyle(ChatFormatting.GRAY)
            );
            player.sendSystemMessage(
                    Component.translatable("votlistva.role.log").withStyle(ChatFormatting.GOLD)
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
