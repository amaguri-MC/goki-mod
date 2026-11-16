package net.amaguri.goki.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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
 * target &lt;player&gt; | target clear | stage &lt;0..MAX&gt; | spray | status
 */
public final class GokiCommands {
	private GokiCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("goki")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("target")
						.then(Commands.literal("clear").executes(GokiCommands::targetClear))
						.then(Commands.argument("player", EntityArgument.player()).executes(GokiCommands::targetSet)))
				.then(Commands.literal("stage")
						.then(Commands.argument("n", IntegerArgumentType.integer(GokiStage.START, GokiStage.MAX)).executes(GokiCommands::stage)))
				.then(Commands.literal("spray").executes(GokiCommands::spray))
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
		ElytraGrant.check(server);
		if (n > old) {
			LevelUp.announce(server, old, n);
		}
		say(ctx, "stage " + old + " -> " + GokiState.stage());
		return 1;
	}

	/** Spray cloud right on the target (for re-shooting the ending). It kills the target like the real thing. */
	private static int spray(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		ServerPlayer target = GokiState.onlineTarget(src.getServer());
		if (target == null) {
			src.sendFailure(Component.literal("オンラインのターゲットがいません"));
			return 0;
		}
		SprayClouds.spawn(target.level(), target.getX(), target.getY(), target.getZ(), null, 100);
		say(ctx, "ターゲット " + target.getPlainTextName() + " の位置にスプレーの雲を出しました");
		return 1;
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
		sb.append("target: ").append(who).append("\nstage: ").append(GokiState.stage()).append(" / ").append(GokiStage.MAX);
		sb.append("\nスプレー化: ").append(SprayClouds.active() ? "ON（ターゲット設定中）" : "OFF（完全バニラ）");
		if (target != null) {
			ServerLevel level = target.level();
			boolean inEnd = level.dimension() == Level.END;
			sb.append("\nターゲットのディメンション: ").append(level.dimension().identifier());
			if (inEnd) {
				for (EnderDragon d : level.getDragons()) {
					if (d.isAlive() && d.getMaxHealth() > 0.0f) {
						boolean focus = SprayClouds.focusTarget(level, d) != null;
						sb.append(String.format("\nドラゴン体力: %.0f/%.0f (%.0f%%) 狙い撃ち(%.0f%%以下): %s", d.getHealth(), d.getMaxHealth(),
								100.0 * d.getHealth() / d.getMaxHealth(), 100.0 * GokiStage.SPRAY_FOCUS_HEALTH_RATIO, focus ? "ON" : "OFF"));
					}
				}
			}
		}
		say(ctx, sb.toString());
		return 1;
	}
}
