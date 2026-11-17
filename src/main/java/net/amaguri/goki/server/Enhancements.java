package net.amaguri.goki.server;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.amaguri.goki.GokiMod;

/**
 * Stage-based buffs for every player. Idempotent "reconcile" design: the wanted state is recomputed from the
 * stage and compared with what the player has, so logout, death, dimension change, milk etc. self-heal and
 * nothing is ever applied twice (modifiers are keyed by a fixed Identifier; effects are only topped up).
 * Attribute modifiers are permanent (saved with the player, so max health +4 is already there when the saved
 * health is loaded on login); they are still re-checked on join, respawn and every second.
 */
public final class Enhancements {
	private Enhancements() {
	}

	private static final Identifier ID_SPEED = GokiMod.id("speed");
	private static final Identifier ID_ARMOR = GokiMod.id("armor");
	private static final Identifier ID_ATTACK = GokiMod.id("attack");
	private static final Identifier ID_BREAK = GokiMod.id("break_speed");
	private static final Identifier ID_HEALTH = GokiMod.id("max_health");

	/** Effect length when (re)applied, and the remaining time below which it is topped up. Must stay above the 200-tick night vision flicker. */
	private static final int EFFECT_TICKS = 1200;
	private static final int REFRESH_BELOW = 600;

	public static void applyAll(MinecraftServer server) {
		int stage = GokiState.stage();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			apply(p, stage);
		}
	}

	/** With no target the mod is inactive: stage 0 strips every goki modifier and hidden effect. */
	public static void apply(ServerPlayer p, int stage) {
		if (!GokiState.hasTargets()) {
			stage = 0;
		}
		modifier(p, Attributes.MOVEMENT_SPEED, ID_SPEED, StageLogic.speedBonus(stage), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		modifier(p, Attributes.ARMOR, ID_ARMOR, StageLogic.armorBonus(stage), AttributeModifier.Operation.ADD_VALUE);
		modifier(p, Attributes.ATTACK_DAMAGE, ID_ATTACK, StageLogic.attackBonus(stage), AttributeModifier.Operation.ADD_VALUE);
		modifier(p, Attributes.BLOCK_BREAK_SPEED, ID_BREAK, StageLogic.breakSpeedBonus(stage), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		modifier(p, Attributes.MAX_HEALTH, ID_HEALTH, StageLogic.maxHealthBonus(stage), AttributeModifier.Operation.ADD_VALUE);
		effect(p, MobEffects.FIRE_RESISTANCE, StageLogic.fireResistance(stage));
		effect(p, MobEffects.NIGHT_VISION, StageLogic.nightVision(stage));
		if (p.getHealth() > p.getMaxHealth()) {
			p.setHealth(p.getMaxHealth());
		}
	}

	private static void modifier(ServerPlayer p, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
		AttributeInstance inst = p.getAttribute(attribute);
		if (inst == null) {
			return;
		}
		AttributeModifier current = inst.getModifier(id);
		if (amount == 0.0) {
			if (current != null) {
				inst.removeModifier(id);
			}
			return;
		}
		if (current != null && current.amount() == amount && current.operation() == op) {
			return;
		}
		inst.addOrReplacePermanentModifier(new AttributeModifier(id, amount, op));
	}

	/** Hidden (ambient, no particles, no icon) effect that never runs out while wanted. */
	private static void effect(ServerPlayer p, Holder<MobEffect> effect, boolean wanted) {
		MobEffectInstance current = p.getEffect(effect);
		if (wanted) {
			if (current == null || (!current.isInfiniteDuration() && current.getDuration() < REFRESH_BELOW)) {
				p.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, 0, true, false, false));
			}
		} else if (current != null && current.isAmbient() && !current.isVisible() && !current.showIcon()) {
			// only ours (stage lowered by command); a real potion is visible and stays
			p.removeEffect(effect);
		}
	}
}
