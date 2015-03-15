package com.nisovin.shopkeepers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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

	public static List<String> colorize(List<String> messages) {
		if (messages == null) return messages;
		List<String> colored = new ArrayList<String>(messages.size());
		for (String message : messages) {
			colored.add(Utils.colorize(message));
		}
		return colored;
	}

	public static void sendMessage(CommandSender sender, String message, String... args) {
		// skip if sender is null or message is "empty":
		if (sender == null || message == null || message.isEmpty()) return;
		if (args != null && args.length >= 2) {
			// replace arguments (key-value replacement):
			String key;
			String value;
			for (int i = 1; i < args.length; i += 2) {
				key = args[i - 1];
				value = args[i];
				if (key == null || value == null) continue; // skip invalid arguments
				message = message.replace(key, value);
			}
		}

		message = Utils.colorize(message);
		String[] msgs = message.split("\n");
		for (String msg : msgs) {
			sender.sendMessage(msg);
		}
	}

	// entity utilities:

	public static List<Entity> getNearbyEntities(Location location, double radius, EntityType... types) {
		List<Entity> entities = new ArrayList<Entity>();
		if (location == null) return entities;
		if (radius <= 0.0D) return entities;

		double radius2 = radius * radius;
		int chunkRadius = ((int) (radius / 16)) + 1;
		Chunk center = location.getChunk();
		int startX = center.getX() - chunkRadius;
		int endX = center.getX() + chunkRadius;
		int startZ = center.getZ();
		int endZ = center.getZ() + chunkRadius;
		World world = location.getWorld();
		for (int chunkX = startX; chunkX <= endX; chunkX++) {
			for (int chunkZ = startZ; chunkZ <= endZ; chunkZ++) {
				if (!world.isChunkLoaded(chunkX, chunkZ)) continue;
				Chunk chunk = world.getChunkAt(chunkX, chunkZ);
				for (Entity entity : chunk.getEntities()) {
					Location entityLoc = entity.getLocation();
					// TODO: this is a workaround: for some yet unknown reason entities sometimes report to be in a different world..
					if (!entityLoc.getWorld().equals(world)) {
						Log.debug("Found an entity which reports to be in a different world than the chunk we got it from:");
						Log.debug("Location=" + location + ", Chunk=" + chunk + ", ChunkWorld=" + chunk.getWorld()
								+ ", entityType=" + entity.getType() + ", entityLocation=" + entityLoc);
						continue; // skip this entity
					}

					if (entityLoc.distanceSquared(location) <= radius2) {
						if (types == null) {
							entities.add(entity);
						} else {
							EntityType type = entity.getType();
							for (EntityType t : types) {
								if (type.equals(t)) {
									entities.add(entity);
									break;
								}
							}
						}
					}
				}
			}
		}
		return entities;
	}

	// itemstack utilities:

	public static ItemStack createItemStack(Material type, short data, String displayName, List<String> lore) {
		ItemStack item = new ItemStack(type, 1, data);
		return setItemStackNameAndLore(item, displayName, lore);
	}

	public static ItemStack setItemStackNameAndLore(ItemStack item, String displayName, List<String> lore) {
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Utils.colorize(displayName));
		meta.setLore(Utils.colorize(lore));
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
		return Utils.areSimilarReasoned(item1, item2) == null;
	}

	/**
	 * Checks if the given items are similar.
	 * Avoids using bukkit's built-in isSimilar() check, which not always returns the expected result (in case of comparison of attributes and skull data).
	 * 
	 * @param item1
	 * @param item2
	 * @return null if we consider the given items as being similar,
	 *         otherwise a string containing a reason why the items are not considered similar
	 *         which could for ex. be used in debugging
	 */
	public static String areSimilarReasoned(ItemStack item1, ItemStack item2) {
		// item type:
		boolean item1Empty = (item1 == null || item1.getType() == Material.AIR);
		boolean item2Empty = (item2 == null || item2.getType() == Material.AIR);
		if (item1Empty || item2Empty) {
			if (item1Empty == item2Empty) {
				return null; // both emtpy -> similar
			}
			return "differing types (one is empty)";
		}
		if (item1.getType() != item2.getType()) {
			return "differing types";
		}
		// data / durability:
		if (item1.getDurability() != item2.getDurability()) {
			return "differing durability / data value";
		}

		// version specific general item comparison:
		String reason = NMSManager.getProvider().areSimilarReasoned(item1, item2);
		if (reason != null) {
			return reason;
		}

		// item meta:
		if (item1.hasItemMeta() != item2.hasItemMeta()) {
			return "differing meta (one has no meta)";
		}
		if (item1.hasItemMeta() && item2.hasItemMeta()) {
			ItemMeta itemMeta1 = item1.getItemMeta();
			ItemMeta itemMeta2 = item2.getItemMeta();
			if (itemMeta1.getClass() != itemMeta2.getClass()) {
				return "differing meta types";
			}

			// display name:
			if (itemMeta1.hasDisplayName() != itemMeta2.hasDisplayName()) {
				return "differing displaynames (one has no displayname)";
			}
			if (itemMeta1.hasDisplayName()) {
				assert itemMeta2.hasDisplayName();
				if (!itemMeta1.getDisplayName().equals(itemMeta2.getDisplayName())) {
					return "differing displaynames";
				}
			}

			// enchants:
			if (itemMeta1.hasEnchants() != itemMeta2.hasEnchants()) {
				return "differing enchants (one has no enchants)";
			}
			if (itemMeta1.hasEnchants()) {
				assert itemMeta2.hasEnchants();
				if (!itemMeta1.getEnchants().equals(itemMeta2.getEnchants())) {
					return "differing enchants";
				}
			}

			// lore:
			if (itemMeta1.hasLore() != itemMeta2.hasLore()) {
				return "differing lores (one has no lore)";
			}
			if (itemMeta1.hasLore()) {
				assert itemMeta2.hasLore();
				if (!itemMeta1.getLore().equals(itemMeta2.getLore())) {
					return "differing lores";
				}
			}

			// attributes:
			if (!NMSManager.getProvider().areAttributesSimilar(item1, item2)) {
				return "differing attributes";
			}

			// special item meta types:

			if (itemMeta1 instanceof Repairable) {
				// repairable:
				assert itemMeta2 instanceof Repairable;
				Repairable repairable1 = (Repairable) itemMeta1;
				Repairable repairable2 = (Repairable) itemMeta2;

				// repair cost:
				if (repairable1.hasRepairCost() != repairable2.hasRepairCost()) {
					return "differing repair costs (one has no repair cost)";
				}
				if (repairable1.hasRepairCost()) {
					assert repairable2.hasRepairCost();
					if (repairable1.getRepairCost() != repairable2.getRepairCost()) {
						return "differing repair costs";
					}
				}
			}

			if (itemMeta1 instanceof BookMeta) {
				// book:
				assert itemMeta2 instanceof BookMeta;
				BookMeta book1 = (BookMeta) itemMeta1;
				BookMeta book2 = (BookMeta) itemMeta2;

				// author:
				if (book1.hasAuthor() != book2.hasAuthor()) {
					return "differing book authors (one has no author)";
				}
				if (book1.hasAuthor()) {
					assert book2.hasAuthor();
					if (!book1.getAuthor().equals(book2.getAuthor())) {
						return "differing book authors";
					}
				}

				// title:
				if (book1.hasTitle() != book2.hasTitle()) {
					return "differing book titles (one has no title)";
				}
				if (book1.hasTitle()) {
					assert book2.hasTitle();
					if (!book1.getTitle().equals(book2.getTitle())) {
						return "differing book title";
					}
				}

				// pages:
				if (book1.hasPages() != book2.hasPages()) {
					return "differing book pages (one has no pages)";
				}
				if (book1.hasPages()) {
					assert book2.hasPages();
					if (book1.getPageCount() != book2.getPageCount()) {
						return "differing book pages (differing page counts)";
					}
					if (!book1.getPages().equals(book2.getPages())) {
						return "differing book pages";
					}
				}
			} else if (itemMeta1 instanceof SkullMeta) {
				// skull:
				assert itemMeta2 instanceof SkullMeta;
				SkullMeta skull1 = (SkullMeta) itemMeta1;
				SkullMeta skull2 = (SkullMeta) itemMeta2;

				// owner:
				if (skull1.hasOwner() != skull2.hasOwner()) {
					return "differing skull owners (one has no owner)";
				}
				if (skull1.hasOwner()) {
					assert skull2.hasOwner();
					if (!skull1.getOwner().equals(skull2.getOwner())) {
						return "differing skull owners";
					}
				}
			} else if (itemMeta1 instanceof LeatherArmorMeta) {
				// leather armor:
				assert itemMeta2 instanceof LeatherArmorMeta;
				LeatherArmorMeta armor1 = (LeatherArmorMeta) itemMeta1;
				LeatherArmorMeta armor2 = (LeatherArmorMeta) itemMeta2;

				// color:
				if (!armor1.getColor().equals(armor2.getColor())) {
					return "differing leather armor color";
				}
			} else if (itemMeta1 instanceof EnchantmentStorageMeta) {
				// enchanted book:
				assert itemMeta2 instanceof EnchantmentStorageMeta;
				EnchantmentStorageMeta enchanted1 = (EnchantmentStorageMeta) itemMeta1;
				EnchantmentStorageMeta enchanted2 = (EnchantmentStorageMeta) itemMeta2;

				// stored enchants:
				if (enchanted1.hasStoredEnchants() != enchanted2.hasStoredEnchants()) {
					return "differing stored enchants (one has no stored enchants)";
				}
				if (enchanted1.hasStoredEnchants()) {
					assert enchanted2.hasStoredEnchants();
					if (!enchanted1.getStoredEnchants().equals(enchanted2.getStoredEnchants())) {
						return "differing stored enchants";
					}
				}
			} else if (itemMeta1 instanceof FireworkEffectMeta) {
				// firework effect:
				assert itemMeta2 instanceof FireworkEffectMeta;
				FireworkEffectMeta fireworkEffect1 = (FireworkEffectMeta) itemMeta1;
				FireworkEffectMeta fireworkEffect2 = (FireworkEffectMeta) itemMeta2;

				// effect:
				if (fireworkEffect1.hasEffect() != fireworkEffect2.hasEffect()) {
					return "differing stored firework effects (one has no effects)";
				}
				if (fireworkEffect1.hasEffect()) {
					assert fireworkEffect2.hasEffect();
					if (!fireworkEffect1.getEffect().equals(fireworkEffect2.getEffect())) {
						return "differing stored firework effects";
					}
				}
			} else if (itemMeta1 instanceof FireworkMeta) {
				// firework:
				assert itemMeta2 instanceof FireworkMeta;
				FireworkMeta firework1 = (FireworkMeta) itemMeta1;
				FireworkMeta firework2 = (FireworkMeta) itemMeta2;

				// effects:
				if (firework1.hasEffects() != firework2.hasEffects()) {
					return "differing firework effects (one has no effects)";
				}
				if (firework1.hasEffects()) {
					assert firework2.hasEffects();
					if (firework1.getEffectsSize() != firework2.getEffectsSize()) {
						return "differing firework effects (differing effect counts)";
					}
					if (!firework1.getEffects().equals(firework2.getEffects())) {
						return "differing firework effects";
					}
				}
			} else if (itemMeta1 instanceof PotionMeta) {
				// potion:
				assert itemMeta2 instanceof PotionMeta;
				PotionMeta potion1 = (PotionMeta) itemMeta1;
				PotionMeta potion2 = (PotionMeta) itemMeta2;

				// custom effects:
				if (potion1.hasCustomEffects() != potion2.hasCustomEffects()) {
					return "differing custom potion effects (one has no effects)";
				}
				if (potion1.hasCustomEffects()) {
					assert potion2.hasCustomEffects();
					if (!potion1.getCustomEffects().equals(potion2.getCustomEffects())) {
						return "differing custom potion effects";
					}
				}
			} else if (itemMeta1 instanceof MapMeta) {
				// map:
				assert itemMeta2 instanceof MapMeta;
				MapMeta map1 = (MapMeta) itemMeta1;
				MapMeta map2 = (MapMeta) itemMeta2;

				// is scaling:
				if (map1.isScaling() != map2.isScaling()) {
					return "differing map scaling";
				}
			}

			// version specific item meta comparison:
			reason = NMSManager.getProvider().areSimilarReasoned(itemMeta1, itemMeta2);
			if (reason != null) {
				return reason;
			}
		}

		return null; // considered similar
	}

	/**
	 * Checks if the given item matches the specified attributes.
	 * 
	 * @param item
	 * @param type
	 *            The item type.
	 * @param data
	 *            The data value/durability. If -1 is is ignored.
	 * @param displayName
	 *            The displayName. If null or empty it is ignored.
	 * @param lore
	 *            The item lore. If null or empty it is ignored.
	 * @return
	 */
	public static boolean isSimilar(ItemStack item, Material type, short data, String displayName, List<String> lore) {
		if (item == null) return false;
		if (item.getType() != type) return false;
		if (data != -1 && item.getDurability() != data) return false;
		ItemMeta itemMeta = item.getItemMeta();
		if (displayName != null && !displayName.isEmpty() && (!itemMeta.hasDisplayName() || !displayName.equals(itemMeta.getDisplayName()))) return false;
		if (lore != null && !lore.isEmpty() && (!itemMeta.hasLore() || !lore.equals(itemMeta.getLore()))) return false;

		return true;
	}

	// inventory utilities:

	/**
	 * Checks if the given inventory contains at least a certain amount of items which match the specified attributes.
	 * 
	 * @param inv
	 * @param type
	 *            The item type.
	 * @param data
	 *            The data value/durability. If -1 is is ignored.
	 * @param displayName
	 *            The displayName. If null it is ignored.
	 * @param lore
	 *            The item lore. If null or empty it is ignored.
	 * @param amount
	 * @return
	 */
	public static boolean hasInventoryItemsAtLeast(Inventory inv, Material type, short data, String displayName, List<String> lore, int amount) {
		for (ItemStack is : inv.getContents()) {
			if (!Utils.isSimilar(is, type, data, displayName, lore)) continue;
			int currentAmount = is.getAmount() - amount;
			if (currentAmount >= 0) {
				return true;
			} else {
				amount = -currentAmount;
			}
		}
		return false;
	}

	/**
	 * Removes the specified amount of items which match the specified attributes from the given inventory.
	 * 
	 * @param inv
	 * @param type
	 *            The item type.
	 * @param data
	 *            The data value/durability. If -1 is is ignored.
	 * @param displayName
	 *            The displayName. If null it is ignored.
	 * @param lore
	 *            The item lore. If null or empty it is ignored.
	 * @param amount
	 */
	public static void removeItemsFromInventory(Inventory inv, Material type, short data, String displayName, List<String> lore, int amount) {
		for (ItemStack is : inv.getContents()) {
			if (!Utils.isSimilar(is, type, data, displayName, lore)) continue;
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

	@SuppressWarnings("deprecation")
	public static void updateInventoryLater(final Player player) {
		Bukkit.getScheduler().runTaskLater(ShopkeepersPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				player.updateInventory();
			}
		}, 3L);
	}

	public static Integer parseInt(String intString) {
		try {
			return Integer.parseInt(intString);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}