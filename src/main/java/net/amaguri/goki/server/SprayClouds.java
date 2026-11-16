package net.amaguri.goki.server;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

import net.amaguri.goki.GokiMod;
import net.amaguri.goki.GokiParticles;

/**
 * The ending gag: the dragon's fireball clouds and breath clouds become insecticide spray. Spray has no
 * potion effects at all (harmless to everybody), and the only thing that reacts to it is {@link #tick}, which
 * kills the target on contact. With no target set nothing here is active and the fight is pure vanilla.
 */
public final class SprayClouds {
	private SprayClouds() {
	}

	public static final ResourceKey<DamageType> INSECTICIDE = ResourceKey.create(Registries.DAMAGE_TYPE, GokiMod.id("insecticide"));

	/** Spray mode is on exactly while a target is set. */
	public static boolean active() {
		return GokiState.target() != null;
	}

	public static boolean isSpray(AreaEffectCloud cloud) {
		return cloud.getParticle() == GokiParticles.SPRAY;
	}

	/** Turns a (vanilla) dragon cloud into spray: new particle, no effects. Returns true if it changed anything. */
	public static boolean convert(AreaEffectCloud cloud) {
		if (isSpray(cloud)) {
			return false;
		}
		cloud.setCustomParticle(GokiParticles.SPRAY);
		cloud.setPotionContents(PotionContents.EMPTY);
		return true;
	}

	/** A fresh spray cloud (no effects, no damage by itself). */
	public static AreaEffectCloud spawn(ServerLevel level, double x, double y, double z, LivingEntity owner, int duration) {
		AreaEffectCloud cloud = new AreaEffectCloud(level, x, y, z);
		if (owner != null) {
			cloud.setOwner(owner);
		}
		cloud.setCustomParticle(GokiParticles.SPRAY);
		cloud.setRadius(3.0f);
		cloud.setDuration(duration);
		cloud.setRadiusPerTick((7.0f - 3.0f) / duration);
		level.addFreshEntity(cloud);
		return cloud;
	}

	/** The player the dragon should focus right now (late fight, target in the End), or null for vanilla behaviour. */
	public static ServerPlayer focusTarget(ServerLevel level, EnderDragon dragon) {
		ServerPlayer target = GokiState.onlineTarget(level.getServer());
		if (target == null || target.level() != level || target.isSpectator() || !target.isAlive()) {
			return null;
		}
		boolean ok = StageLogic.sprayFocus(level.dimension() == Level.END, dragon.isAlive() && !dragon.isDeadOrDying(),
				dragon.getHealth(), dragon.getMaxHealth());
		return ok ? target : null;
	}

	/** The dragon that currently runs this phase instance (phase classes do not expose their dragon). */
	public static EnderDragon dragonOf(ServerLevel level, DragonPhaseInstance phase) {
		for (EnderDragon d : level.getDragons()) {
			if (d.getPhaseManager().getCurrentPhase() == phase) {
				return d;
			}
		}
		return null;
	}

	/** Every tick: the target touching any spray cloud dies. Nobody else is ever touched. */
	public static void tick(MinecraftServer server) {
		ServerPlayer target = GokiState.onlineTarget(server);
		if (target == null || !target.isAlive() || target.isSpectator()) {
			return;
		}
		ServerLevel level = target.level();
		for (AreaEffectCloud cloud : level.getEntitiesOfClass(AreaEffectCloud.class, target.getBoundingBox().inflate(8.0),
				c -> isSpray(c) && !c.isWaiting())) {
			if (cloud.getBoundingBox().intersects(target.getBoundingBox())
					&& StageLogic.insideCloud(target.getX() - cloud.getX(), target.getZ() - cloud.getZ(), cloud.getRadius())) {
				kill(level, target);
				return;
			}
		}
	}

	/** Instant death by goki:insecticide (tags make it ignore armor, resistance, totems, creative invulnerability). */
	public static void kill(ServerLevel level, ServerPlayer target) {
		DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(INSECTICIDE));
		target.hurtServer(level, source, 1.0e6f);
		if (target.isAlive()) {
			target.kill(level); // safety net if something still absorbed it
		}
	}
}
