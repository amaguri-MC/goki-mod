package net.amaguri.goki.server;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import net.amaguri.goki.GokiStage;

/** Raises the stage when anybody completes the matching advancement. Never lowers it. */
public final class Progress {
	private Progress() {
	}

	public static void check(MinecraftServer server) {
		int current = GokiState.stage();
		if (current >= GokiStage.MAX) {
			return;
		}
		int next = StageLogic.nextStage(current, n -> anyoneDone(server, n));
		if (next > current) {
			GokiState.setStage(server, next);
			Enhancements.applyAll(server);
			LevelUp.announce(server, current, next);
		}
	}

	private static boolean anyoneDone(MinecraftServer server, int n) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (p.isCreative() && creativeReached(p, n)) {
				return true;
			}
		}
		String raw = GokiStage.ADVANCEMENTS[n];
		if (raw == null) {
			return false;
		}
		if (raw.startsWith(ITEM_PREFIX)) {
			return anyoneHas(server, raw.substring(ITEM_PREFIX.length()));
		}
		Identifier id = Identifier.tryParse(raw);
		if (id == null) {
			return false;
		}
		AdvancementHolder holder = server.getAdvancements().get(id);
		if (holder == null) {
			return false;
		}
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (p.getAdvancements().getOrStartProgress(holder).isDone()) {
				return true;
			}
		}
		return false;
	}

	/** "item:<id>" entries in GokiStage.ADVANCEMENTS: reached when somebody carries that item. */
	private static final String ITEM_PREFIX = "item:";

	private static boolean anyoneHas(MinecraftServer server, String itemId) {
		Identifier id = Identifier.tryParse(itemId);
		if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
			return false;
		}
		Item item = BuiltInRegistries.ITEM.getValue(id);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (has(p, item)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Debug aid: creative players don't get item drops, so advancements rarely fire. For them the stage item in the
	 * inventory (or being in the dimension) counts as reaching the stage.
	 */
	private static boolean creativeReached(ServerPlayer p, int n) {
		return switch (n) {
			case 1 -> has(p, Items.COBBLESTONE) || has(p, Items.COBBLED_DEEPSLATE) || has(p, Items.BLACKSTONE);
			case 2 -> has(p, Items.IRON_INGOT);
			case 3 -> has(p, Items.DIAMOND);
			case 4 -> p.level().dimension() == Level.NETHER;
			case 5 -> has(p, Items.BLAZE_ROD);
			case 6 -> has(p, Items.ENDER_PEARL);
			case 7 -> has(p, Items.ENDER_EYE); // follow_ender_eye is hard to reach in creative
			case 8 -> p.level().dimension() == Level.END;
			default -> false;
		};
	}

	private static boolean has(ServerPlayer p, Item item) {
		return p.getInventory().contains(stack -> stack.getItem() == item);
	}

	private static int count(ServerPlayer p, Item item) {
		int total = 0;
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			ItemStack stack = p.getInventory().getItem(i);
			if (stack.getItem() == item) {
				total += stack.getCount();
			}
		}
		return total;
	}
}
