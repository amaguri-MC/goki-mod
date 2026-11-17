package net.amaguri.goki.net;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.amaguri.goki.GokiMod;

/**
 * Server -> all clients: who the targets are (0..n players, all share one stage) and the current stage.
 * Sent on join and on every change. An empty list means the mod is inactive (vanilla). CONTRACT FILE.
 */
public record StageSyncPayload(List<UUID> targets, int stage) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<StageSyncPayload> TYPE = new CustomPacketPayload.Type<>(GokiMod.id("stage_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StageSyncPayload> CODEC = StreamCodec.ofMember(StageSyncPayload::write, StageSyncPayload::read);

	public StageSyncPayload {
		targets = List.copyOf(targets);
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(targets.size());
		for (UUID u : targets) {
			buf.writeLong(u.getMostSignificantBits());
			buf.writeLong(u.getLeastSignificantBits());
		}
		buf.writeVarInt(stage);
	}

	private static StageSyncPayload read(RegistryFriendlyByteBuf buf) {
		int n = buf.readVarInt();
		List<UUID> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			list.add(new UUID(buf.readLong(), buf.readLong()));
		}
		return new StageSyncPayload(list, buf.readVarInt());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
