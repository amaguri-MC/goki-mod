package net.amaguri.goki.server;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import net.amaguri.goki.GokiMod;

/** Registers goki:torimochi (block + item). Model/blockstate/texture JSON come from W3. */
public final class GokiBlocks {
	private GokiBlocks() {
	}

	public static Block TORIMOCHI;
	public static Item TORIMOCHI_ITEM;

	public static void register() {
		Identifier id = GokiMod.id("torimochi");

		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
				.setId(blockKey)
				.mapColor(MapColor.COLOR_BROWN)
				.noOcclusion()
				.strength(2.0f) // 60 ticks bare-handed (no tool required) = 3 s
				.sound(SoundType.SLIME_BLOCK)
				.speedFactor(0.1f)
				.jumpFactor(0.0f)
				.pushReaction(PushReaction.DESTROY)
				.noLootTable();
		TORIMOCHI = Registry.register(BuiltInRegistries.BLOCK, blockKey, new TorimochiBlock(props));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		Item.Properties itemProps = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
		TORIMOCHI_ITEM = Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(TORIMOCHI, itemProps));
	}
}
