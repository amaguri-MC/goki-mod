package net.amaguri.goki.server;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.amaguri.goki.GokiStage;

/** Stage 7 -> 8: puts a torimochi under the target when the dragon is nearly dead. */
public final class TrapController {
	private TrapController() {
	}

	/** Called about twice a second. */
	public static void check(MinecraftServer server) {
		if (GokiState.stage() != GokiStage.MAX_ADVANCEMENT_STAGE) {
			return;
		}
		ServerPlayer target = GokiState.onlineTarget(server);
		if (target == null) {
			return;
		}
		ServerLevel level = target.level();
		boolean inEnd = level.dimension() == Level.END;
		if (!inEnd) {
			return;
		}
		boolean alive = false;
		float health = 0.0f;
		float max = 0.0f;
		double bestRatio = Double.MAX_VALUE;
		for (EnderDragon dragon : level.getDragons()) {
			if (!dragon.isAlive() || dragon.isDeadOrDying() || dragon.getMaxHealth() <= 0.0f) {
				continue;
			}
			double ratio = dragon.getHealth() / dragon.getMaxHealth();
			if (ratio < bestRatio) {
				bestRatio = ratio;
				alive = true;
				health = dragon.getHealth();
				max = dragon.getMaxHealth();
			}
		}
		if (StageLogic.shouldTrap(GokiState.stage(), inEnd, alive, health, max)) {
			fire(server, target);
		}
	}

	/** Places the trap under the target, sets stage 8 and tells only the target. */
	public static void fire(MinecraftServer server, ServerPlayer target) {
		place(target);
		GokiState.setStage(server, GokiStage.TRAPPED);
		LevelUp.showTitle(target, Component.empty(), Component.literal(StageText.TRAP_SUBTITLE), 5, 70, 20);
	}

	/** Torimochi at the feet (placed if the cell is air/replaceable), otherwise one block up. Returns where it went. */
	public static BlockPos place(ServerPlayer who) {
		ServerLevel level = who.level();
		BlockPos feet = who.blockPosition();
		BlockState at = level.getBlockState(feet);
		BlockPos pos = (at.isAir() || at.canBeReplaced()) ? feet : feet.above();
		level.setBlockAndUpdate(pos, GokiBlocks.TORIMOCHI.defaultBlockState());
		return pos;
	}
}
