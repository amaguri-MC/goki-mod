package net.amaguri.goki.client.spray;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** goki:spray - a soft puff of insecticide mist that swells, slows down and fades. */
public class SprayParticle extends SingleQuadParticle {
	private final SpriteSet sprites;
	private final float spin;

	protected SprayParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, xd, yd, zd, sprites.first());
		this.sprites = sprites;
		// the base constructor re-randomises the velocity; use the one that was asked for
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		this.lifetime = 18 + this.random.nextInt(14);
		this.quadSize = 0.28F + this.random.nextFloat() * 0.22F;
		this.friction = 0.88F;
		this.gravity = -0.006F;
		this.hasPhysics = false;
		this.roll = this.random.nextFloat() * (float) (Math.PI * 2.0);
		this.oRoll = this.roll;
		this.spin = (this.random.nextFloat() - 0.5F) * 0.2F;
		float tint = this.random.nextFloat() * 0.06F;
		this.setColor(1.0F - tint, 1.0F - tint * 0.4F, 1.0F);
		this.setSpriteFromAge(sprites);
	}

	@Override
	public float getQuadSize(float partialTick) {
		float t = Math.min(1.0F, (this.age + partialTick) / this.lifetime);
		return this.quadSize * (0.6F + 1.6F * (float) Math.sqrt(t));
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.removed) {
			this.setSpriteFromAge(this.sprites);
			this.oRoll = this.roll;
			this.roll += this.spin;
			float t = (float) this.age / this.lifetime;
			this.setAlpha(Math.max(0.0F, 0.95F * (1.0F - t * t)));
		}
	}

	@Override
	protected int getLightCoords(float partialTick) {
		return 0xF000F0;
	}

	@Override
	protected SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	public static final class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd, RandomSource random) {
			return new SprayParticle(level, x, y, z, xd, yd, zd, this.sprites);
		}
	}
}
