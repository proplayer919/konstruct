package dev.proplayer919.konstruct.loot;

import net.kyori.adventure.text.Component;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class LootGenerator {
    public static Inventory generateLoot(ChestIdentifier chestId) {
        Block block = chestId.block();
        LootTier tier = TIERS_BY_BLOCK.get(block.name());
        if (tier == null) {
            throw new IllegalArgumentException("Unsupported chest block type for loot generation: " + block);
        }
        return generateForTier(tier);
    }

    private static class LootEntry {
        final Material material;
        final int weight;
        final int minCount;
        final int maxCount;

        LootEntry(Material material, int weight, int minCount, int maxCount) {
            this.material = material;
            this.weight = Math.max(0, weight);
            this.minCount = Math.max(1, minCount);
            this.maxCount = Math.max(this.minCount, maxCount);
        }
    }

    private static class LootTier {
        final String title;
        final List<LootEntry> entries = new ArrayList<>();
        final int minItems;
        final int maxItems;
        private int totalWeight = 0;

        LootTier(String title, int minItems, int maxItems) {
            this.title = title;
            this.minItems = minItems;
            this.maxItems = maxItems;
        }

        LootTier add(Material mat, int weight) { return add(mat, weight, 1, 1); }
        LootTier add(Material mat, int weight, int minCount, int maxCount) {
            LootEntry e = new LootEntry(mat, weight, minCount, maxCount);
            entries.add(e);
            totalWeight += e.weight;
            return this;
        }

        LootEntry pick(Random rnd) {
            if (entries.isEmpty() || totalWeight <= 0) return null;
            int r = rnd.nextInt(totalWeight);
            int cumulative = 0;
            for (LootEntry e : entries) {
                cumulative += e.weight;
                if (r < cumulative) return e;
            }
            return entries.get(entries.size() - 1);
        }
    }

    // Build tier definitions here. Add new tiers by adding another factory method and mapping entry.
    private static final Map<String, LootTier> TIERS_BY_BLOCK = createTierRegistry();

    private static Map<String, LootTier> createTierRegistry() {
        Map<String, LootTier> m = new HashMap<>();
        m.put("minecraft:chest", createTier1());
        m.put("minecraft:waxed_copper_chest", createTier2());
        m.put("minecraft:ender_chest", createTier3());
        return m;
    }

    private static LootTier createTier1() {
        LootTier t = new LootTier("Tier 1 Chest", 3, 9);
        // Wooden tools (common) -> weight 6 each originally
        t.add(Material.WOODEN_SWORD, 6);
        t.add(Material.WOODEN_PICKAXE, 6);
        t.add(Material.WOODEN_AXE, 6);
        t.add(Material.WOODEN_SHOVEL, 6);

        // Stone tools (rarer)
        t.add(Material.STONE_SWORD, 2);
        t.add(Material.STONE_PICKAXE, 2);
        t.add(Material.STONE_AXE, 2);
        t.add(Material.STONE_SHOVEL, 2);

        // Leather armor (uncommon)
        t.add(Material.LEATHER_HELMET, 3);
        t.add(Material.LEATHER_CHESTPLATE, 3);
        t.add(Material.LEATHER_LEGGINGS, 3);
        t.add(Material.LEATHER_BOOTS, 3);

        // Chainmail armor (rare)
        t.add(Material.CHAINMAIL_HELMET, 1);
        t.add(Material.CHAINMAIL_CHESTPLATE, 1);
        t.add(Material.CHAINMAIL_LEGGINGS, 1);
        t.add(Material.CHAINMAIL_BOOTS, 1);

        // Stackables / consumables (weights approximate original repetition)
        t.add(Material.OAK_PLANKS, 10, 4, 16); // 4-16
        t.add(Material.COBBLESTONE, 8, 4, 16); // 4-16
        t.add(Material.BREAD, 6, 1, 4);
        t.add(Material.COOKED_BEEF, 5, 1, 4);
        t.add(Material.STICK, 12, 1, 8);
        t.add(Material.SHIELD, 1, 1, 1);

        return t;
    }

    private static LootTier createTier2() {
        LootTier t = new LootTier("Tier 2 Chest", 6, 16);
        // Stone tools
        t.add(Material.STONE_SWORD, 2);
        t.add(Material.STONE_PICKAXE, 2);
        t.add(Material.STONE_AXE, 2);
        t.add(Material.STONE_SHOVEL, 2);

        // Iron tools (common)
        t.add(Material.IRON_SWORD, 5);
        t.add(Material.IRON_PICKAXE, 5);
        t.add(Material.IRON_AXE, 5);
        t.add(Material.IRON_SHOVEL, 5);

        // Leather armor (uncommon)
        t.add(Material.LEATHER_HELMET, 3);
        t.add(Material.LEATHER_CHESTPLATE, 3);
        t.add(Material.LEATHER_LEGGINGS, 3);
        t.add(Material.LEATHER_BOOTS, 3);

        // Chainmail armor (common)
        t.add(Material.CHAINMAIL_HELMET, 4);
        t.add(Material.CHAINMAIL_CHESTPLATE, 4);
        t.add(Material.CHAINMAIL_LEGGINGS, 4);
        t.add(Material.CHAINMAIL_BOOTS, 4);

        // Iron armor (rare)
        t.add(Material.IRON_HELMET, 2);
        t.add(Material.IRON_CHESTPLATE, 2);
        t.add(Material.IRON_LEGGINGS, 2);
        t.add(Material.IRON_BOOTS, 2);

        // Buckets
        t.add(Material.WATER_BUCKET, 2);
        t.add(Material.LAVA_BUCKET, 1);

        // Stackables / consumables
        t.add(Material.COBBLESTONE, 8, 4, 16);
        t.add(Material.GOLDEN_CARROT, 6, 1, 4);
        t.add(Material.COOKED_BEEF, 7, 1, 4);
        t.add(Material.SHIELD, 3, 1, 1);

        return t;
    }

    private static LootTier createTier3() {
        LootTier t = new LootTier("Tier 3 Chest", 6, 16);
        // Iron tools
        t.add(Material.IRON_SWORD, 2);
        t.add(Material.IRON_PICKAXE, 1);
        t.add(Material.IRON_AXE, 2);
        t.add(Material.IRON_SHOVEL, 1);

        // Diamond tools
        t.add(Material.DIAMOND_SWORD, 4);
        t.add(Material.DIAMOND_PICKAXE, 3);
        t.add(Material.DIAMOND_AXE, 4);
        t.add(Material.DIAMOND_SHOVEL, 3);

        // Iron armor
        t.add(Material.IRON_HELMET, 2);
        t.add(Material.IRON_CHESTPLATE, 2);
        t.add(Material.IRON_LEGGINGS, 2);
        t.add(Material.IRON_BOOTS, 2);

        // Diamond armor
        t.add(Material.DIAMOND_HELMET, 4);
        t.add(Material.DIAMOND_CHESTPLATE, 4);
        t.add(Material.DIAMOND_LEGGINGS, 4);
        t.add(Material.DIAMOND_BOOTS, 4);

        // Consumables
        t.add(Material.GOLDEN_CARROT, 6, 5, 14);
        t.add(Material.GOLDEN_APPLE, 4, 1, 4);
        t.add(Material.ENCHANTED_GOLDEN_APPLE, 1, 1, 1);

        return t;
    }

    private static Inventory generateForTier(LootTier tier) {
        Inventory inventory = new Inventory(InventoryType.CHEST_3_ROW, Component.text(tier.title));
        Random rnd = ThreadLocalRandom.current();
        int itemsToPlace = tier.minItems + rnd.nextInt(Math.max(1, tier.maxItems - tier.minItems + 1));
        Set<Integer> usedSlots = new HashSet<>();
        int slotCount = InventoryType.CHEST_3_ROW.getSize(); // 27

        int placed = 0;
        while (placed < itemsToPlace && placed < slotCount) {
            int slot = rnd.nextInt(slotCount);
            if (usedSlots.contains(slot)) continue;

            LootEntry entry = tier.pick(rnd);
            if (entry == null) break;

            int countRange = entry.maxCount - entry.minCount + 1;
            int count = entry.minCount + (countRange > 1 ? rnd.nextInt(countRange) : 0);

            ItemStack itemStack = ItemStack.of(entry.material, count);
            inventory.setItemStack(slot, itemStack);
            usedSlots.add(slot);
            placed++;
        }

        return inventory;
    }
}
