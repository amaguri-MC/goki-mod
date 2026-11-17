package net.amaguri.goki.gametest;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.amaguri.goki.client.ClientGokiState;

public class GokiClientGameTest implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("GokiGameTest");

	private static void log(String s) {
		LOGGER.info("GOKI_TEST: {}", s);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			log("world created");
			closeScreens(context);
			sp.getServer().runCommand("time set day");
			sp.getServer().runCommand("weather clear");
			sp.getServer().runCommand("execute as @p run effect give @s minecraft:resistance infinite 4 true");
			sp.getServer().runCommand("execute as @p run clear @s");
			sp.getServer().runCommand("execute as @p run goki target @p");
			context.waitTicks(10);
			closeScreens(context);

			// 1. stages 0..8
			for (int n = 0; n <= 8; n++) {
				sp.getServer().runCommand("goki stage " + n);
				final int stage = n;
				if (n > 0) {
					context.waitFor(c -> ClientGokiState.stage == stage, 200);
				}
				log("stage set " + n + " clientStage=" + ClientGokiState.stage + " selfIsTarget=" + context.computeOnClient(c -> ClientGokiState.selfIsTarget()));
				sp.getConnection().waitForClientboundPackets();
				if (n > 0) {
					context.waitTicks(12);
					context.takeScreenshot("stage" + n + "_0_title");
				}
				context.waitTicks(70); // let the level-up title fade
				closeScreens(context);
				setCamera(context, CameraType.FIRST_PERSON);
				context.waitTicks(3);
				context.takeScreenshot("stage" + n + "_1_first");
				setCamera(context, CameraType.THIRD_PERSON_BACK);
				context.waitTicks(3);
				context.takeScreenshot("stage" + n + "_2_back");
				setCamera(context, CameraType.THIRD_PERSON_FRONT);
				context.waitTicks(3);
				context.takeScreenshot("stage" + n + "_3_front");
				setCamera(context, CameraType.FIRST_PERSON);
				closeScreens(context);
				if (n == 3 || n == 6) {
					context.getInput().pressKey(o -> o.keyToggleGui);
					context.waitTicks(3);
					context.takeScreenshot("stage" + n + "_4_f1");
					context.getInput().pressKey(o -> o.keyToggleGui);
					context.waitTicks(2);
				}
			}

			// multi-target: list / remove / add
			for (String line : capture(sp, "goki target list")) {
				log("target list: " + line.replace("\n", " | "));
			}
			sp.getServer().runCommand("execute as @p run goki target remove @p");
			context.waitFor(c -> !context.computeOnClient(m -> ClientGokiState.selfIsTarget()), 200);
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			closeScreens(context);
			log("after remove: selfIsTarget=" + context.computeOnClient(c -> ClientGokiState.selfIsTarget()));
			for (String line : capture(sp, "goki target list")) {
				log("target list(removed): " + line.replace("\n", " | "));
			}
			for (String line : capture(sp, "goki status")) {
				log("status(removed): " + line.replace("\n", " | "));
			}
			setCamera(context, CameraType.FIRST_PERSON);
			context.waitTicks(3);
			context.takeScreenshot("multi_1_removed_first");
			setCamera(context, CameraType.THIRD_PERSON_FRONT);
			context.waitTicks(3);
			context.takeScreenshot("multi_2_removed_front");
			sp.getServer().runCommand("execute as @p run goki target add @p");
			context.waitFor(c -> context.computeOnClient(m -> ClientGokiState.selfIsTarget()), 200);
			sp.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			closeScreens(context);
			log("after add: selfIsTarget=" + context.computeOnClient(c -> ClientGokiState.selfIsTarget()));
			context.takeScreenshot("multi_3_readded_front");
			setCamera(context, CameraType.FIRST_PERSON);
			context.waitTicks(3);
			context.takeScreenshot("multi_4_readded_first");

			// 2. status
			for (String line : capture(sp, "goki status")) {
				log("status: " + line.replace("\n", " | "));
			}

			// 3. the End: dragon as a spray can
			goToEnd(context, sp);
			faceDragon(context, sp, 22);
			setCamera(context, CameraType.FIRST_PERSON);
			context.takeScreenshot("end_1_dragon_target_a");
			context.waitTicks(40);
			faceDragon(context, sp, 14);
			context.takeScreenshot("end_2_dragon_target_b");
			context.waitTicks(60);
			faceDragon(context, sp, 30);
			context.takeScreenshot("end_3_dragon_target_c");
			for (String line : capture(sp, "goki status")) {
				log("status(end): " + line.replace("\n", " | "));
			}

			// spray: instant death
			sp.getServer().runCommand("execute as @p in minecraft:the_end run tp @s 0 75 0");
			context.waitTicks(150); // slow falling: land on the island first
			closeScreens(context);
			sp.getServer().runCommand("execute as @p run effect clear @s minecraft:resistance");
			log("player alive before spray: " + sp.getServer().computeOnServer(s -> s.getPlayerList().getPlayers().get(0).isAlive() + " y=" + s.getPlayerList().getPlayers().get(0).getY()));
			for (String line : capture(sp, "goki spray")) {
				log("spray: " + line);
			}
			try {
				context.waitForScreen(DeathScreen.class);
			} catch (Throwable t) {
				log("DeathScreen did not appear: " + t);
			}
			context.waitTicks(30);
			String deathMsg = sp.getServer().computeOnServer(s -> {
				try {
					return s.getPlayerList().getPlayers().get(0).getCombatTracker().getDeathMessage().getString();
				} catch (Throwable t) {
					return "unavailable: " + t;
				}
			});
			log("death message: " + deathMsg);
			context.takeScreenshot("end_4_death_screen");

			// 4. target clear -> vanilla dragon
			context.tryClickScreenButton("deathScreen.respawn");
			context.waitTicks(40);
			closeScreens(context);
			sp.getServer().runCommand("goki target clear");
			for (String line : capture(sp, "goki status")) {
				log("status(cleared): " + line.replace("\n", " | "));
			}
			sp.getServer().runCommand("execute as @p run effect give @s minecraft:resistance infinite 4 true");
			sp.getServer().runCommand("execute as @p run effect give @s minecraft:slow_falling infinite 0 true");
			goToEnd(context, sp);
			faceDragon(context, sp, 22);
			context.takeScreenshot("end_5_dragon_cleared_a");
			context.waitTicks(40);
			faceDragon(context, sp, 14);
			context.takeScreenshot("end_6_dragon_cleared_b");
			log("test finished");
		}
	}

	private static void setCamera(ClientGameTestContext context, CameraType type) {
		context.runOnClient(c -> c.options.setCameraType(type));
	}

	/** An open pause screen freezes the integrated server, so close it whenever we wait. */
	private static void closeScreens(ClientGameTestContext context) {
		context.runOnClient(c -> {
			if (c.gui.screen() instanceof PauseScreen) {
				c.gui.setScreen(null);
			}
		});
	}

	private static List<String> capture(TestSingleplayerContext sp, String command) {
		return sp.getServer().computeOnServer(server -> {
			List<String> out = new ArrayList<>();
			CommandSource src = new CommandSource() {
				@Override
				public void sendSystemMessage(Component c) {
					out.add(c.getString());
				}

				@Override
				public boolean acceptsSuccess() {
					return true;
				}

				@Override
				public boolean acceptsFailure() {
					return true;
				}

				@Override
				public boolean shouldInformAdmins() {
					return false;
				}
			};
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(src), command);
			return out;
		});
	}

	private static void goToEnd(ClientGameTestContext context, TestSingleplayerContext sp) {
		sp.getServer().runCommand("execute as @p run effect give @s minecraft:slow_falling infinite 0 true");
		sp.getServer().runCommand("execute as @p run effect give @s minecraft:resistance infinite 4 true");
		sp.getServer().runCommand("execute as @p in minecraft:the_end run tp @s 0 75 30");
		log("teleported to the End");
		sp.getServer().waitFor(s -> {
			ServerLevel l = s.getLevel(Level.END);
			return l != null && !l.getDragons().isEmpty();
		}, 1200);
		log("dragon exists on server");
		context.waitTicks(40);
		closeScreens(context);
		sp.getConnection().waitForChunksRender();
		closeScreens(context);
	}

	/** Put the player {@code dist} blocks from the dragon (same height) looking at it. */
	private static void faceDragon(ClientGameTestContext context, TestSingleplayerContext sp, double dist) {
		closeScreens(context);
		double[] pos = sp.getServer().computeOnServer(s -> {
			ServerLevel l = s.getLevel(Level.END);
			var d = l.getDragons().get(0);
			return new double[] { d.getX(), d.getY(), d.getZ() };
		});
		double ang = Math.atan2(pos[2], pos[0]);
		double px = pos[0] + Math.cos(ang) * dist;
		double pz = pos[2] + Math.sin(ang) * dist;
		if (Math.hypot(pos[0], pos[2]) < 1.0) {
			px = pos[0];
			pz = pos[2] + dist;
		}
		sp.getServer().runCommand(String.format(java.util.Locale.ROOT,
				"execute as @p in minecraft:the_end run tp @s %.2f %.2f %.2f facing entity @e[type=minecraft:ender_dragon,limit=1] eyes",
				px, pos[1] + 0.5, pz));
		log(String.format(java.util.Locale.ROOT, "dragon at %.1f %.1f %.1f, viewer at %.1f %.1f %.1f", pos[0], pos[1], pos[2], px, pos[1] + 0.5, pz));
		sp.getConnection().waitForClientboundPackets();
		context.waitTicks(4);
	}
}
