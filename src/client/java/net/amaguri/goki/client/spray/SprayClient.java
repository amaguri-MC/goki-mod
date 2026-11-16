package net.amaguri.goki.client.spray;

import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

import net.amaguri.goki.GokiParticles;

/** Entry point of the spray-can dragon visuals (W4). Call SprayClient.init() once from GokiClient.onInitializeClient(). */
public final class SprayClient {
	private SprayClient() {
	}

	public static void init() {
		// goki:spray particle (sprites from assets/goki/particles/spray.json)
		ParticleProviderRegistry.getInstance().register(GokiParticles.SPRAY, (FabricSpriteSet sprites) -> new SprayParticle.Provider(sprites));
		SprayBreath.init();
		// The dragon body / fireball swaps are mixins (client/mixin/spray), nothing to register here.
	}
}
