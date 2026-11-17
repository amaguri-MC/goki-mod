package net.amaguri.goki.server;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TooltipDisplay;

import net.amaguri.goki.GokiStage;

/**
 * Once the stage reaches GokiStage.elytra (stage 5+), every target gets an unbreakable elytra in the chest slot, one time each (the flag is per target; offline targets get it on joining).
 * A chestplate already worn goes to the inventory (or drops at the feet if it is full). The "given" flag is
 * saved in goki_state.json and reset when the stage drops below the elytra stage. The chat line comes from
 * StageText.targetBonus(5) via LevelUp.
 */
public final class ElytraGrant {
	private ElytraGrant() {
	}

	public static void check(MinecraftServer server) {
		if (!GokiStage.elytra(GokiState.stage())) {
			return;
		}
		for (ServerPlayer p : GokiState.onlineTargets(server)) {
			if (!GokiState.elytraGiven(p.getUUID())) {
				give(p);
				GokiState.markElytraGiven(p.getUUID());
			}
		}
	}

	private static void give(ServerPlayer p) {
		// Already wearing (or carrying) an elytra, e.g. target removed and re-added: don't hand out a second one.
		if (p.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
				|| p.getInventory().contains(s -> s.getItem() == Items.ELYTRA)) {
			return;
		}
		ItemStack elytra = new ItemStack(Items.ELYTRA);
		elytra.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		elytra.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.UNBREAKABLE, true));
		ItemStack worn = p.getItemBySlot(EquipmentSlot.CHEST);
		p.setItemSlot(EquipmentSlot.CHEST, elytra);
		if (!worn.isEmpty() && !p.getInventory().add(worn)) {
			p.spawnAtLocation(p.level(), worn);
		}
	}
}
