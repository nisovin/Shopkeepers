package com.nisovin.shopkeepers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;

import com.nisovin.shopkeepers.compat.NMSManager;

public class Utils {

	public static final BlockFace[] chestProtectFaces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
	public static final BlockFace[] hopperProtectFaces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };

	public static boolean isChest(Material material) {
		return material == Material.CHEST || material == Material.TRAPPED_CHEST;
	}

	public static boolean isProtectedChestAroundChest(Player player, Block chest) {
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) return false;
		for (BlockFace face : Utils.chestProtectFaces) {
			Block b = chest.getRelative(face);
			if (isChest(b.getType()) && plugin.isChestProtected(player, b)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isProtectedChestAroundHopper(Player player, Block hopper) {
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) return false;
		for (BlockFace face : Utils.hopperProtectFaces) {
			Block b = hopper.getRelative(face);
			if (Utils.isChest(b.getType()) && plugin.isChestProtected(player, b)) {
				return true;
			}
		}
		return false;
	}

	// messages:

	public static String colorize(String message) {
		if (message == null || message.isEmpty()) return message;
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	public static void sendMessage(Player player, String message, String... args) {
		// skip if player is null or message is "empty":
		if (player == null || message == null || message.isEmpty()) return;
		if (args != null && args.length >= 2) {
			// replace arguments (key-value replacement):
			String key;
			String value;
			int pairCount = (int) (args.length / 2); // cut down to pairs of 2
			for (int i = 0; i < pairCount; i++) {
				key = args[2 * i];
				value = args[i + 1];
				if (key == null || value == null) continue; // skip invalid arguments
				message = message.replace(key, value);
			}
		}

		message = Utils.colorize(message);
		String[] msgs = message.split("\n");
		for (String msg : msgs) {
			player.sendMessage(msg);
		}
	}

	// itemstack utilities:

	public static ItemStack createItemStack(Material type, short data, String name, List<String> lore) {
		ItemStack item = new ItemStack(type, 1, data);
		return setItemStackNameAndLore(item, name, lore);
	}

	public static ItemStack setItemStackNameAndLore(ItemStack item, String name, List<String> lore) {
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Utils.colorize(name));
		List<String> loreColored = new ArrayList<String>(lore.size());
		for (String loreString : lore) {
			loreColored.add(Utils.colorize(loreString));
		}
		meta.setLore(loreColored);
		item.setItemMeta(meta);
		return item;
	}

	/**
	 * Checks if the given items are similar.
	 * Avoids using bukkit's built-in isSimilar() check, which not always returns the expected result (in case of comparison of attributes and skull data).
	 * 
	 * @param item1
	 * @param item2
	 * @return
	 */
	public static boolean areSimilar(ItemStack item1, ItemStack item2) {
		// item type:
		boolean item1Empty = (item1 == null || item1.getType() == Material.AIR);
		boolean item2Empty = (item2 == null || item2.getType() == Material.AIR);
		if (item1Empty || item2Empty) {
			return (item1Empty == item2Empty);
		}
		if (item1.getType() != item2.getType()) {
			return false;
		}
		// data / durability:
		if (item1.getDurability() != item2.getDurability()) {
			return false;
		}

		// item meta:
		if (item1.hasItemMeta() != item2.hasItemMeta()) {
			return false;
		}
		if (item1.hasItemMeta() && item2.hasItemMeta()) {
			ItemMeta itemMeta1 = item1.getItemMeta();
			ItemMeta itemMeta2 = item2.getItemMeta();
			if (itemMeta1.getClass() != itemMeta2.getClass()) {
				return false;
			}

			// display name:
			if (itemMeta1.hasDisplayName() != itemMeta2.hasDisplayName()) {
				return false;
			}
			if (itemMeta1.hasDisplayName()) {
				assert itemMeta2.hasDisplayName();
				if (!itemMeta1.getDisplayName().equals(itemMeta2.getDisplayName())) {
					return false;
				}
			}

			// enchants:
			if (itemMeta1.hasEnchants() != itemMeta2.hasEnchants()) {
				return false;
			}
			if (itemMeta1.hasEnchants()) {
				assert itemMeta2.hasEnchants();
				if (!itemMeta1.getEnchants().equals(itemMeta2.getEnchants())) {
					return false;
				}
			}

			// lore:
			if (itemMeta1.hasLore() != itemMeta2.hasLore()) {
				return false;
			}
			if (itemMeta1.hasLore()) {
				assert itemMeta2.hasLore();
				if (!itemMeta1.getLore().equals(itemMeta2.getLore())) {
					return false;
				}
			}

			// attributes:
			if (!NMSManager.getProvider().areAttributesSimilar(item1, item2)) {
				return false;
			}

			// special item meta types:

			if (itemMeta1 instanceof Repairable) {
				// repairable:
				assert itemMeta2 instanceof Repairable;
				Repairable repairable1 = (Repairable) itemMeta1;
				Repairable repairable2 = (Repairable) itemMeta1;

				// repair cost:
				if (repairable1.hasRepairCost() != repairable2.hasRepairCost()) {
					return false;
				}
				if (repairable1.hasRepairCost()) {
					assert repairable2.hasRepairCost();
					if (repairable1.getRepairCost() != repairable2.getRepairCost()) {
						return false;
					}
				}
			}

			if (itemMeta1 instanceof BookMeta) {
				// book:
				assert itemMeta2 instanceof BookMeta;
				BookMeta book1 = (BookMeta) itemMeta1;
				BookMeta book2 = (BookMeta) itemMeta1;

				// author:
				if (book1.hasAuthor() != book2.hasAuthor()) {
					return false;
				}
				if (book1.hasAuthor()) {
					assert book2.hasAuthor();
					if (!book1.getAuthor().equals(book2.getAuthor())) {
						return false;
					}
				}

				// title:
				if (book1.hasTitle() != book2.hasTitle()) {
					return false;
				}
				if (book1.hasTitle()) {
					assert book2.hasTitle();
					if (!book1.getTitle().equals(book2.getTitle())) {
						return false;
					}
				}

				// pages:
				if (book1.hasPages() != book2.hasPages()) {
					return false;
				}
				if (book1.hasPages()) {
					assert book2.hasPages();
					if (book1.getPageCount() != book2.getPageCount()) {
						return false;
					}
					if (!book1.getPages().equals(book2.getPages())) {
						return false;
					}
				}
			} else if (itemMeta1 instanceof SkullMeta) {
				// skull:
				assert itemMeta2 instanceof SkullMeta;
				SkullMeta skull1 = (SkullMeta) itemMeta1;
				SkullMeta skull2 = (SkullMeta) itemMeta1;

				// owner:
				if (skull1.hasOwner() != skull2.hasOwner()) {
					return false;
				}
				if (skull1.hasOwner()) {
					assert skull2.hasOwner();
					if (!skull1.getOwner().equals(skull2.getOwner())) {
						return false;
					}
				}
			} else if (itemMeta1 instanceof LeatherArmorMeta) {
				// leather armor:
				assert itemMeta2 instanceof LeatherArmorMeta;
				LeatherArmorMeta armor1 = (LeatherArmorMeta) itemMeta1;
				LeatherArmorMeta armor2 = (LeatherArmorMeta) itemMeta1;

				// color:
				if (!armor1.getColor().equals(armor2.getColor())) {
					return false;
				}
			} else if (itemMeta1 instanceof EnchantmentStorageMeta) {
				// enchanted book:
				assert itemMeta2 instanceof EnchantmentStorageMeta;
				EnchantmentStorageMeta enchanted1 = (EnchantmentStorageMeta) itemMeta1;
				EnchantmentStorageMeta enchanted2 = (EnchantmentStorageMeta) itemMeta1;

				// stored enchants:
				if (enchanted1.hasStoredEnchants() != enchanted2.hasStoredEnchants()) {
					return false;
				}
				if (enchanted1.hasStoredEnchants()) {
					assert enchanted2.hasStoredEnchants();
					if (!enchanted1.getStoredEnchants().equals(enchanted2.getStoredEnchants())) {
						return false;
					}
				}
			} else if (itemMeta1 instanceof FireworkEffectMeta) {
				// firework effect:
				assert itemMeta2 instanceof FireworkEffectMeta;
				FireworkEffectMeta fireworkEffect1 = (FireworkEffectMeta) itemMeta1;
				FireworkEffectMeta fireworkEffect2 = (FireworkEffectMeta) itemMeta1;

				// effect:
				if (fireworkEffect1.hasEffect() != fireworkEffect2.hasEffect()) {
					return false;
				}
				if (fireworkEffect1.hasEffect()) {
					assert fireworkEffect2.hasEffect();
					if (!fireworkEffect1.getEffect().equals(fireworkEffect2.getEffect())) {
						return false;
					}
				}
			} else if (itemMeta1 instanceof FireworkMeta) {
				// firework:
				assert itemMeta2 instanceof FireworkMeta;
				FireworkMeta firework1 = (FireworkMeta) itemMeta1;
				FireworkMeta firework2 = (FireworkMeta) itemMeta1;

				// effects:
				if (firework1.hasEffects() != firework2.hasEffects()) {
					return false;
				}
				if (firework1.hasEffects()) {
					assert firework2.hasEffects();
					if (firework1.getEffectsSize() != firework2.getEffectsSize()) {
						return false;
					}
					if (!firework1.getEffects().equals(firework2.getEffects())) {
						return false;
					}
				}
			} else if (itemMeta1 instanceof PotionMeta) {
				// potion:
				assert itemMeta2 instanceof PotionMeta;
				PotionMeta potion1 = (PotionMeta) itemMeta1;
				PotionMeta potion2 = (PotionMeta) itemMeta1;

				// custom effects:
				if (potion1.hasCustomEffects() != potion2.hasCustomEffects()) {
					return false;
				}
				if (potion1.hasCustomEffects()) {
					assert potion2.hasCustomEffects();
					if (!potion1.getCustomEffects().equals(potion2.getCustomEffects())) {
						return false;
					}
				}
			} else if (itemMeta1 instanceof MapMeta) {
				// map:
				assert itemMeta2 instanceof MapMeta;
				MapMeta map1 = (MapMeta) itemMeta1;
				MapMeta map2 = (MapMeta) itemMeta1;

				// is scaling:
				if (map1.isScaling() != map2.isScaling()) {
					return false;
				}
			}
		}

		return true;
	}

	// inventory utilities:

	public static boolean hasInventoryItemsAtLeast(Inventory inv, Material type, short data, int amount) {
		for (ItemStack is : inv.getContents()) {
			if (is != null && is.getType() == type && is.getDurability() == data) {
				int currentAmount = is.getAmount() - amount;
				if (currentAmount >= 0) {
					return true;
				} else {
					amount = -currentAmount;
				}
			}
		}
		return false;
	}

	public static void removeItemsFromInventory(Inventory inv, Material type, short data, int amount) {
		for (ItemStack is : inv.getContents()) {
			if (is != null && is.getType() == type && is.getDurability() == data) {
				int newamount = is.getAmount() - amount;
				if (newamount > 0) {
					is.setAmount(newamount);
					break;
				} else {
					inv.remove(is);
					amount = -newamount;
					if (amount == 0) break;
				}
			}
		}
	}

	public static void closeInventoryLater(final HumanEntity player) {
		Bukkit.getScheduler().runTaskLater(ShopkeepersPlugin.getInstance(), new Runnable() {
			public void run() {
				player.closeInventory();
			}
		}, 1);
	}

	@SuppressWarnings("deprecation")
	public static void updateInventoryLater(final Player player) {
		Bukkit.getScheduler().runTaskLater(ShopkeepersPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				player.updateInventory();
			}
		}, 3L);
	}
}