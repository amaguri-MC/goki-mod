package net.amaguri.goki.net;

import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.amaguri.goki.GokiMod;

/**
 * Server -> all clients: who the target is and the current stage. Sent on join and on every change.
 * target == NONE means no target set. CONTRACT FILE.
 */
public record StageSyncPayload(UUID target, int stage) implements CustomPacketPayload {
	public static final UUID NONE = new UUID(0L, 0L);
	public static final CustomPacketPayload.Type<StageSyncPayload> TYPE = new CustomPacketPayload.Type<>(GokiMod.id("stage_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StageSyncPayload> CODEC = StreamCodec.ofMember(StageSyncPayload::write, StageSyncPayload::read);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeLong(target.getMostSignificantBits());
		buf.writeLong(target.getLeastSignificantBits());
		buf.writeVarInt(stage);
	}

	private static StageSyncPayload read(RegistryFriendlyByteBuf buf) {
		return new StageSyncPayload(new UUID(buf.readLong(), buf.readLong()), buf.readVarInt());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
