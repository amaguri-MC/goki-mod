package net.amaguri.goki;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Insecticide spray mist particle (goki:spray). Registered here; the client provider, sprite texture and
 * particles/spray.json are W4's. CONTRACT FILE.
 */
public final class GokiParticles {
	private GokiParticles() {
	}

	public static final SimpleParticleType SPRAY = FabricParticleTypes.simple(true);

	public static void init() {
		Registry.register(BuiltInRegistries.PARTICLE_TYPE, GokiMod.id("spray"), SPRAY);
	}
}
