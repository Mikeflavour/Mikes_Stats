package mikesstats.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

/** Small icon per table row: the block/item itself, the spawn egg for mobs, a fitting item for general stats. */
final class StatIcons {
    /** custom stat path -> item path */
    private static final Map<String, String> CUSTOM = Map.ofEntries(
            Map.entry("walk_one_cm", "leather_boots"), Map.entry("sprint_one_cm", "sugar"),
            Map.entry("crouch_one_cm", "leather_leggings"), Map.entry("swim_one_cm", "water_bucket"),
            Map.entry("fall_one_cm", "feather"), Map.entry("climb_one_cm", "ladder"),
            Map.entry("fly_one_cm", "phantom_membrane"), Map.entry("walk_on_water_one_cm", "lily_pad"),
            Map.entry("walk_under_water_one_cm", "turtle_helmet"), Map.entry("minecart_one_cm", "minecart"),
            Map.entry("boat_one_cm", "oak_boat"), Map.entry("pig_one_cm", "carrot_on_a_stick"),
            Map.entry("horse_one_cm", "saddle"), Map.entry("aviate_one_cm", "elytra"),
            Map.entry("strider_one_cm", "warped_fungus_on_a_stick"), Map.entry("happy_ghast_one_cm", "ghast_tear"),
            Map.entry("nautilus_one_cm", "nautilus_shell"), Map.entry("jump", "rabbit_foot"),
            Map.entry("sneak_time", "leather_leggings"), Map.entry("damage_dealt", "iron_sword"),
            Map.entry("damage_dealt_absorbed", "golden_sword"), Map.entry("damage_dealt_resisted", "stone_sword"),
            Map.entry("damage_taken", "iron_chestplate"), Map.entry("damage_blocked_by_shield", "shield"),
            Map.entry("damage_absorbed", "golden_apple"), Map.entry("damage_resisted", "diamond_chestplate"),
            Map.entry("mob_kills", "zombie_head"), Map.entry("animals_bred", "wheat"), Map.entry("fish_caught", "cod"),
            Map.entry("traded_with_villager", "emerald"), Map.entry("talked_to_villager", "book"),
            Map.entry("sleep_in_bed", "red_bed"), Map.entry("time_since_death", "skeleton_skull"),
            Map.entry("time_since_rest", "red_bed"), Map.entry("total_world_time", "clock"),
            Map.entry("leave_game", "oak_door"), Map.entry("drop", "dropper"),
            Map.entry("enchant_item", "enchanting_table"), Map.entry("open_chest", "chest"),
            Map.entry("open_enderchest", "ender_chest"), Map.entry("open_shulker_box", "shulker_box"),
            Map.entry("open_barrel", "barrel"), Map.entry("interact_with_crafting_table", "crafting_table"),
            Map.entry("interact_with_furnace", "furnace"), Map.entry("interact_with_blast_furnace", "blast_furnace"),
            Map.entry("interact_with_smoker", "smoker"), Map.entry("interact_with_anvil", "anvil"),
            Map.entry("interact_with_grindstone", "grindstone"), Map.entry("interact_with_stonecutter", "stonecutter"),
            Map.entry("interact_with_loom", "loom"), Map.entry("interact_with_cartography_table", "cartography_table"),
            Map.entry("interact_with_smithing_table", "smithing_table"), Map.entry("interact_with_lectern", "lectern"),
            Map.entry("interact_with_campfire", "campfire"), Map.entry("interact_with_brewingstand", "brewing_stand"),
            Map.entry("interact_with_beacon", "beacon"), Map.entry("eat_cake_slice", "cake"), Map.entry("bell_ring", "bell"),
            Map.entry("raid_trigger", "crossbow"), Map.entry("raid_win", "totem_of_undying"), Map.entry("target_hit", "target"),
            Map.entry("play_noteblock", "note_block"), Map.entry("tune_noteblock", "note_block"),
            Map.entry("play_record", "jukebox"), Map.entry("pot_flower", "flower_pot"), Map.entry("fill_cauldron", "cauldron"),
            Map.entry("use_cauldron", "cauldron"), Map.entry("clean_armor", "leather_chestplate"),
            Map.entry("clean_banner", "white_banner"), Map.entry("clean_shulker_box", "shulker_box"),
            Map.entry("inspect_dispenser", "dispenser"), Map.entry("inspect_dropper", "dropper"),
            Map.entry("inspect_hopper", "hopper"), Map.entry("trigger_trapped_chest", "trapped_chest"));

    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    private StatIcons() {
    }

    static ItemStack icon(String category, String statId) {
        return CACHE.computeIfAbsent(category + "|" + statId, k -> new ItemStack(find(category, statId)));
    }

    private static Item find(String category, String statId) {
        Identifier id = Identifier.tryParse(statId);
        if (id == null) return Items.PAPER;
        Item item = switch (category) {
            case "minecraft:custom" -> item(CUSTOM.get(id.getPath()));
            case "minecraft:mined" -> BuiltInRegistries.BLOCK.getOptional(id).map(Block::asItem).orElse(Items.AIR);
            case "minecraft:killed", "minecraft:killed_by" -> BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                    .flatMap(SpawnEggItem::byId).map(Holder::value)
                    .orElse(id.getPath().equals("player") ? Items.PLAYER_HEAD : Items.SKELETON_SKULL);
            default -> BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        };
        return item == Items.AIR ? Items.PAPER : item;
    }

    private static Item item(String path) {
        return path == null ? Items.PAPER : BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(path)).orElse(Items.PAPER);
    }
}
