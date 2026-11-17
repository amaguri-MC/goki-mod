package net.amaguri.goki.server;

import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

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
 * target add &lt;players&gt; | target remove &lt;players&gt; | target clear | target list | target &lt;player&gt; (= add) | stage &lt;0..MAX&gt; | spray | status
 */
public final class GokiCommands {
	private GokiCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("goki")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("target")
						.then(Commands.literal("clear").executes(GokiCommands::targetClear))
						.then(Commands.literal("list").executes(GokiCommands::targetList))
						.then(Commands.literal("add").then(Commands.argument("players", EntityArgument.players()).executes(GokiCommands::targetAdd)))
						.then(Commands.literal("remove").then(Commands.argument("players", EntityArgument.players()).executes(GokiCommands::targetRemove)))
						.then(Commands.argument("player", EntityArgument.players()).executes(GokiCommands::targetAddLegacy)))
				.then(Commands.literal("stage")
						.then(Commands.argument("n", IntegerArgumentType.integer(GokiStage.START, GokiStage.MAX)).executes(GokiCommands::stage)))
				.then(Commands.literal("spray").executes(GokiCommands::spray))
				.then(Commands.literal("status").executes(GokiCommands::status)));
	}

	private static void say(CommandContext<CommandSourceStack> ctx, String text) {
		ctx.getSource().sendSuccess(() -> Component.literal(text), false);
	}

	private static int targetAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return addAll(ctx, EntityArgument.getPlayers(ctx, "players"));
	}

	/** Old form /goki target <player>: same as add. */
	private static int targetAddLegacy(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return addAll(ctx, EntityArgument.getPlayers(ctx, "player"));
	}

	private static int addAll(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
		MinecraftServer server = ctx.getSource().getServer();
		int added = 0;
		for (ServerPlayer p : players) {
			if (GokiState.addTarget(server, p)) {
				added++;
			}
		}
		Enhancements.applyAll(server);
		ElytraGrant.check(server);
		say(ctx, "ターゲットを " + added + " 人追加しました（合計 " + GokiState.entries().size() + " 人、現在 stage " + GokiState.stage() + "）");
		return added;
	}

	private static int targetRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		MinecraftServer server = ctx.getSource().getServer();
		int removed = 0;
		for (ServerPlayer p : EntityArgument.getPlayers(ctx, "players")) {
			if (GokiState.removeTarget(server, p.getUUID())) {
				removed++;
			}
		}
		Enhancements.applyAll(server);
		say(ctx, "ターゲットを " + removed + " 人外しました（残り " + GokiState.entries().size() + " 人）");
		return removed;
	}

	private static int targetClear(CommandContext<CommandSourceStack> ctx) {
		GokiState.clearTargets(ctx.getSource().getServer());
		Enhancements.applyAll(ctx.getSource().getServer());
		say(ctx, "ターゲットを全員解除しました");
		return 1;
	}

	private static int targetList(CommandContext<CommandSourceStack> ctx) {
		say(ctx, "ターゲット: " + describeTargets(ctx.getSource().getServer()));
		return GokiState.entries().size();
	}

	private static String describeTargets(MinecraftServer server) {
		if (!GokiState.hasTargets()) {
			return "なし";
		}
		StringBuilder sb = new StringBuilder();
		for (GokiState.Entry e : GokiState.entries()) {
			ServerPlayer p = server.getPlayerList().getPlayer(e.id());
			sb.append("\n  ").append(p != null ? p.getPlainTextName() + "（オンライン）" : e.name + "（オフライン " + e.uuid + "）");
		}
		return sb.toString();
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

	/** Spray clouds right on every online, living target (for re-shooting the ending). They kill like the real thing. */
	private static int spray(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		int n = 0;
		StringBuilder names = new StringBuilder();
		for (ServerPlayer target : GokiState.onlineTargets(src.getServer())) {
			if (!target.isAlive()) {
				continue;
			}
			SprayClouds.spawn(target.level(), target.getX(), target.getY(), target.getZ(), null, 100);
			names.append(n++ == 0 ? "" : ", ").append(target.getPlainTextName());
		}
		if (n == 0) {
			src.sendFailure(Component.literal("オンラインで生存中のターゲットがいません"));
			return 0;
		}
		say(ctx, "ターゲット " + n + " 人（" + names + "）の位置にスプレーの雲を出しました");
		return n;
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		StringBuilder sb = new StringBuilder();
		sb.append("target: ").append(describeTargets(server)).append("\nstage: ").append(GokiState.stage()).append(" / ").append(GokiStage.MAX);
		sb.append("\nスプレー化: ").append(SprayClouds.active() ? "ON（ターゲット設定中）" : "OFF（完全バニラ）");
		for (ServerPlayer target : GokiState.onlineTargets(server)) {
			sb.append("\n").append(target.getPlainTextName()).append(": ").append(target.level().dimension().identifier());
		}
		for (ServerLevel level : server.getAllLevels()) {
			if (level.dimension() != Level.END) {
				continue;
			}
			for (EnderDragon d : level.getDragons()) {
				if (d.isAlive() && d.getMaxHealth() > 0.0f) {
					ServerPlayer focus = SprayClouds.focusTarget(level, d);
					sb.append(String.format("\nドラゴン体力: %.0f/%.0f (%.0f%%) 狙い撃ち(%.0f%%以下): %s", d.getHealth(), d.getMaxHealth(),
							100.0 * d.getHealth() / d.getMaxHealth(), 100.0 * GokiStage.SPRAY_FOCUS_HEALTH_RATIO,
							focus == null ? "OFF" : "ON → " + focus.getPlainTextName()));
				}
			}
		}
		say(ctx, sb.toString());
		return 1;
	}
}
