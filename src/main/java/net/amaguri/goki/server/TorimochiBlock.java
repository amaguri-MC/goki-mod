package net.amaguri.goki.server;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.amaguri.goki.GokiHooks;

/**
 * Glue trap ("torimochi"). A thin plate: whoever is inside the block cell can hardly move (stronger than a
 * cobweb) and cannot jump. Breaks in about 3 s bare-handed, but the target can never break it himself.
 * Works on both sides because {@code entityInside} runs in the client-side movement too.
 */
public class TorimochiBlock extends Block {
	public static final MapCodec<TorimochiBlock> CODEC = simpleCodec(TorimochiBlock::new);

	/** Outline: 2/16 high. Collision: 1/16 high. */
	private static final VoxelShape OUTLINE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
	private static final VoxelShape COLLISION = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);
	/** Cobweb is (0.25, 0.05, 0.25). This is about 12x stronger horizontally and also kills jumping. */
	private static final Vec3 STUCK = new Vec3(0.02, 0.02, 0.02);

	public TorimochiBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<TorimochiBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return OUTLINE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISION;
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean precise) {
		entity.makeStuckInBlock(state, STUCK);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (cannotBreak(player)) {
			return 0.0f;
		}
		return super.getDestroyProgress(state, player, level, pos);
	}

	/** Server: exact target check. Client: the contract hook (0 for non-targets), so the crack animation also stops. */
	private static boolean cannotBreak(Player player) {
		if (player.level().isClientSide()) {
			return GokiHooks.stageOf(player) > 0;
		}
		return GokiState.isTarget(player.getUUID());
	}

	@Override
	public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);
		// The block has no loot table JSON; give itself back so the prankster can reuse it.
		if (level instanceof ServerLevel && !player.isCreative()) {
			Block.popResource(level, pos, new ItemStack(this));
		}
	}
}
