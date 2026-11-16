package net.amaguri.goki.client.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

/** Shared client rendering helpers. */
public final class GokiRender {
	private GokiRender() {
	}

	/** Stage of the entity behind a render state (0 for everybody except the target). Written by AvatarRendererMixin. */
	public static final RenderStateDataKey<Integer> STAGE = RenderStateDataKey.create(() -> "goki stage");

	public static int stage(EntityRenderState state) {
		Integer v = state.getData(STAGE);
		return v == null ? 0 : v;
	}

	// ---- texture presence check (W3's PNGs may not exist yet: never draw a missing-texture checkerboard) ----
	private record Entry(boolean present, long checkedAt) {
	}

	private static final Map<Identifier, Entry> CACHE = new ConcurrentHashMap<>();
	private static final long TTL_NS = 2_000_000_000L; // re-check every 2 s so F3+T reloads are picked up

	public static boolean textureExists(Identifier id) {
		if (id == null) {
			return false;
		}
		long now = System.nanoTime();
		Entry e = CACHE.get(id);
		if (e != null && now - e.checkedAt() < TTL_NS) {
			return e.present();
		}
		boolean present;
		try {
			present = Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
		} catch (RuntimeException ex) {
			present = false;
		}
		CACHE.put(id, new Entry(present, now));
		return present;
	}
}
