package net.amaguri.goki.server;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.amaguri.goki.GokiStage;

/** Level-up show: title + sound for everybody, extra "bonus" chat lines for the target only. */
public final class LevelUp {
	private LevelUp() {
	}

	/** Announces going from {@code from} to {@code to} (to in 1..7). Only one title/sound even if stages were skipped. */
	public static void announce(MinecraftServer server, int from, int to) {
		if (to <= from || to > GokiStage.MAX_ADVANCEMENT_STAGE) {
			return;
		}
		String sub = StageText.publicSubtitle(to);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (sub != null) {
				showTitle(p, Component.literal(StageText.TITLE).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
						Component.literal(sub).withStyle(ChatFormatting.YELLOW), 5, 50, 15);
			}
			p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.PLAYER_LEVELUP),
					SoundSource.PLAYERS, p.getX(), p.getY(), p.getZ(), 1.0f, 1.0f, p.getRandom().nextLong()));
		}
		ServerPlayer target = GokiState.onlineTarget(server);
		if (target != null) {
			// chat lines for every passed stage that has one, so a skipped stage does not lose its bonus
			for (int s = from + 1; s <= to; s++) {
				String line = StageText.targetBonus(s);
				if (line != null) {
					target.sendSystemMessage(Component.literal("▶ " + line).withStyle(ChatFormatting.GREEN));
				}
			}
		}
	}

	/** Shows a title and subtitle to one player. An empty title still lets the subtitle through. */
	public static void showTitle(ServerPlayer p, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
		p.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
		p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		p.connection.send(new ClientboundSetTitleTextPacket(title));
	}
}
