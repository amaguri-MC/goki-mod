package net.amaguri.goki.server;

import java.util.HashSet;
import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

import net.amaguri.goki.GokiStage;

/**
 * /goki (permission level 2). Results go to the executor only (sendSuccess(..., false) does not broadcast to ops).
 *
 * target &lt;player&gt; | target clear | stage &lt;0..8&gt; | trap | free | status
 */
public final class GokiCommands {
	private GokiCommands() {
	}

	private static final int FREE_RADIUS = 8;

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("goki")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("target")
						.then(Commands.literal("clear").executes(GokiCommands::targetClear))
						.then(Commands.argument("player", EntityArgument.player()).executes(GokiCommands::targetSet)))
				.then(Commands.literal("stage")
						.then(Commands.argument("n", IntegerArgumentType.integer(GokiStage.START, GokiStage.TRAPPED)).executes(GokiCommands::stage)))
				.then(Commands.literal("trap").executes(GokiCommands::trap))
				.then(Commands.literal("free").executes(GokiCommands::free))
				.then(Commands.literal("status").executes(GokiCommands::status)));
	}

	private static void say(CommandContext<CommandSourceStack> ctx, String text) {
		ctx.getSource().sendSuccess(() -> Component.literal(text), false);
	}

	private static int targetSet(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
		MinecraftServer server = ctx.getSource().getServer();
		GokiState.setTarget(server, p);
		say(ctx, "ターゲットを " + p.getPlainTextName() + " に設定しました（現在 stage " + GokiState.stage() + "）");
		return 1;
	}

	private static int targetClear(CommandContext<CommandSourceStack> ctx) {
		GokiState.setTarget(ctx.getSource().getServer(), null);
		say(ctx, "ターゲットを解除しました");
		return 1;
	}

	private static int stage(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		int n = IntegerArgumentType.getInteger(ctx, "n");
		int old = GokiState.setStage(server, n);
		Enhancements.applyAll(server);
		if (n > old) {
			LevelUp.announce(server, old, n); // ignores 8 (trap has its own scene)
		}
		say(ctx, "stage " + old + " -> " + GokiState.stage());
		return 1;
	}

	private static int trap(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		MinecraftServer server = src.getServer();
		ServerPlayer target = GokiState.onlineTarget(server);
		if (target != null) {
			TrapController.fire(server, target);
			Enhancements.applyAll(server);
			say(ctx, "ターゲット " + target.getPlainTextName() + " の足元にとりもちを置きました（stage 8）");
			return 1;
		}
		ServerPlayer self = src.getPlayer();
		if (self == null) {
			src.sendFailure(Component.literal("オンラインのターゲットがいません"));
			return 0;
		}
		TrapController.place(self);
		say(ctx, "ターゲット不在のため、あなたの足元にとりもちを置きました（stage は変えていません）");
		return 1;
	}

	private static int free(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		MinecraftServer server = src.getServer();
		Set<BlockPos> done = new HashSet<>();
		int removed = 0;
		removed += clear(src.getLevel(), BlockPos.containing(src.getPosition()), done);
		ServerPlayer target = GokiState.onlineTarget(server);
		if (target != null) {
			removed += clear(target.level(), target.blockPosition(), done);
		}
		say(ctx, "とりもちを " + removed + " 個撤去しました（半径 " + FREE_RADIUS + "）");
		return removed;
	}

	private static int clear(ServerLevel level, BlockPos center, Set<BlockPos> done) {
		int count = 0;
		BlockPos min = center.offset(-FREE_RADIUS, -FREE_RADIUS, -FREE_RADIUS);
		BlockPos max = center.offset(FREE_RADIUS, FREE_RADIUS, FREE_RADIUS);
		for (BlockPos p : BlockPos.betweenClosed(min, max)) {
			if (level.getBlockState(p).is(GokiBlocks.TORIMOCHI)) {
				BlockPos fixed = p.immutable();
				if (done.add(fixed)) {
					level.removeBlock(fixed, false);
					count++;
				}
			}
		}
		return count;
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		ServerPlayer target = GokiState.onlineTarget(server);
		String who;
		if (GokiState.target() == null) {
			who = "なし";
		} else if (target != null) {
			who = target.getPlainTextName() + "（オンライン）";
		} else {
			who = GokiState.targetName() + "（オフライン " + GokiState.target() + "）";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("target: ").append(who).append("\nstage: ").append(GokiState.stage()).append(" / ").append(GokiStage.TRAPPED);
		if (target != null) {
			boolean inEnd = target.level().dimension() == Level.END;
			sb.append("\nターゲットのディメンション: ").append(target.level().dimension().identifier());
			if (inEnd) {
				for (EnderDragon d : target.level().getDragons()) {
					if (d.isAlive() && d.getMaxHealth() > 0.0f) {
						sb.append(String.format("\nドラゴン体力: %.0f/%.0f (%.0f%%, 罠は %.0f%% 以下)", d.getHealth(), d.getMaxHealth(),
								100.0 * d.getHealth() / d.getMaxHealth(), 100.0 * GokiStage.TRAP_DRAGON_HEALTH_RATIO));
					}
				}
			}
		}
		say(ctx, sb.toString());
		return 1;
	}
}
