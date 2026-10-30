package mikesstats;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Translation keys for stat ids, the same ones the vanilla statistics screen uses. */
public final class StatNames {
    private StatNames() {
    }

    public static String key(String category, String statId) {
        Identifier id = Identifier.tryParse(statId);
        if (id == null) return statId;
        return switch (category) {
            case "minecraft:custom" -> "stat." + statId.replace(':', '.');
            case "minecraft:mined" -> BuiltInRegistries.BLOCK.getOptional(id).map(Block::getDescriptionId).orElse(statId);
            case "minecraft:killed", "minecraft:killed_by" ->
                    BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(EntityType::getDescriptionId).orElse(statId);
            default -> BuiltInRegistries.ITEM.getOptional(id).map(Item::getDescriptionId).orElse(statId);
        };
    }

    /** "used" of a block item means placed. */
    public static boolean isBlockItem(String statId) {
        Identifier id = Identifier.tryParse(statId);
        return id != null && BuiltInRegistries.ITEM.getOptional(id).orElse(null) instanceof BlockItem;
    }
}
