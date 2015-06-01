package com.nisovin.shopkeepers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permissible;

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

	public static String translateColorCodesToAlternative(char altColorChar, String textToTranslate) {
		char[] b = textToTranslate.toCharArray();
		for (int i = 0; i < b.length - 1; i++) {
			if (b[i] == ChatColor.COLOR_CHAR && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
				b[i] = altColorChar;
				// needed?
				b[i + 1] = Character.toLowerCase(b[i + 1]);
			}
		}
		return new String(b);
	}

	public static String decolorize(String colored) {
		if (colored == null) return null;
		return Utils.translateColorCodesToAlternative('&', colored);
	}

	public static List<String> decolorize(List<String> colored) {
		if (colored == null) return null;
		List<String> decolored = new ArrayList<String>(colored.size());
		for (String string : decolored) {
			decolored.add(Utils.translateColorCodesToAlternative('&', string));
		}
		return decolored;
	}

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

		String[] msgs = message.split("\n");
		for (String msg : msgs) {
			sender.sendMessage(msg);
		}
	}

	/**
	 * Performs a permissions check and logs debug information about it.
	 * 
	 * @param permissible
	 * @param permission
	 * @return
	 */
	public static boolean hasPermission(Permissible permissible, String permission) {
		assert permissible != null;
		boolean hasPerm = permissible.hasPermission(permission);
		if (!hasPerm && (permissible instanceof Player)) {
			Log.debug("Player '" + ((Player) permissible).getName() + "' does not have permission '" + permission + "'.");
		}
		return hasPerm;
	}

	// entity utilities:

	public static boolean isNPC(Entity entity) {
		return entity.hasMetadata("NPC");
	}

	public static List<Entity> getNearbyEntities(Location location, double radius, EntityType... types) {
		List<Entity> entities = new ArrayList<Entity>();
		if (location == null) return entities;
		if (radius <= 0.0D) return entities;

		double radius2 = radius * radius;
		int chunkRadius = ((int) (radius / 16)) + 1;
		Chunk center = location.getChunk();
		int startX = center.getX() - chunkRadius;
		int endX = center.getX() + chunkRadius;
		int startZ = center.getZ() - chunkRadius;
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
		meta.setDisplayName(displayName);
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	public static String getSimpleItemInfo(ItemStack item) {
		if (item == null) return "none";
		StringBuilder sb = new StringBuilder();
		sb.append(item.getType()).append('~').append(item.getDurability());
		return sb.toString();
	}

	public static String getSimpleRecipeInfo(ItemStack[] recipe) {
		if (recipe == null) return "none";
		StringBuilder sb = new StringBuilder();
		sb.append("[0=").append(getSimpleItemInfo(recipe[0]))
			.append(",1=").append(getSimpleItemInfo(recipe[1]))
			.append(",2=").append(getSimpleItemInfo(recipe[2])).append("]");
		return sb.toString();
	}

	/**
	 * Same as {@link ItemStack#isSimilar(ItemStack)}, but taking into account that both given ItemStacks might be null.
	 * 
	 * @param item1
	 * @param item2
	 * @return if the given item stacks are both null or similar
	 */
	public static boolean isSimilar(ItemStack item1, ItemStack item2) {
		if (item1 == null) return (item2 == null);
		return item1.isSimilar(item2);
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

	// save and load itemstacks from config, including attributes:

	/**
	 * Saves the given {@link ItemStack} to the given configuration section.
	 * Also saves the item's attributes in the same section at '{node}_attributes'.
	 * 
	 * @param section
	 *            a configuration section
	 * @param node
	 *            where to save the item stack inside the section
	 * @param item
	 *            the item stack to save, can be null
	 */
	public static void saveItem(ConfigurationSection section, String node, ItemStack item) {
		assert section != null && node != null;
		section.set(node, item);
		String attributes = NMSManager.getProvider().saveItemAttributesToString(item);
		if (attributes != null && !attributes.isEmpty()) {
			String attributesNode = node + "_attributes";
			section.set(attributesNode, attributes);
		}
	}

	/**
	 * Loads an {@link ItemStack} from the given configuration section.
	 * Also attempts to load attributes saved at '{node}_attributes'.
	 * 
	 * @param section
	 *            a configuration section
	 * @param node
	 *            where to load the item stack from inside the section
	 * @return the loaded item stack, possibly null
	 */
	public static ItemStack loadItem(ConfigurationSection section, String node) {
		assert section != null && node != null;
		ItemStack item = section.getItemStack(node);
		String attributesNode = node + "_attributes";
		if (item != null && section.contains(attributesNode)) {
			String attributes = section.getString(attributesNode);
			if (attributes != null && !attributes.isEmpty()) {
				item = NMSManager.getProvider().loadItemAttributesFromString(item, attributes);
			}
		}
		return item;
	}

	// inventory utilities:

	public static List<ItemCount> getItemCountsFromInventory(Inventory inventory, Filter<ItemStack> filter) {
		List<ItemCount> itemCounts = new ArrayList<ItemCount>();
		if (inventory != null) {
			ItemStack[] contents = inventory.getContents();
			for (ItemStack item : contents) {
				if (item == null || item.getType() == Material.AIR) continue;
				if (filter != null && !filter.accept(item)) continue;

				// search entry in items list:
				ItemCount itemCount = ItemCount.findSimilar(itemCounts, item);
				if (itemCount != null) {
					// increase item count:
					itemCount.addAmount(item.getAmount());
				} else {
					// add new item entry:
					ItemStack itemCopy = item.clone();
					itemCopy.setAmount(1);
					itemCounts.add(new ItemCount(itemCopy, item.getAmount()));
				}
			}
		}
		return itemCounts;
	}

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
	 * @param ignoreNameAndLore
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
	 * Adds the given {@link ItemStack} to the given contents.
	 * This will first try to fill similar partial {@link ItemStack}s in the contents up to the item's max stack size.
	 * Afterwards it will insert the remaining amount into empty slots, splitting at the item's max stack size.
	 * 
	 * @param contents
	 *            The contents to add the given {@link ItemStack} to.
	 * @param item
	 *            The {@link ItemStack} to add to the given contents.
	 * @return The amount of items which couldn't be added (0 on full success).
	 */
	public static int addItems(ItemStack[] contents, ItemStack item) {
		Validate.notNull(contents);
		Validate.notNull(item);
		int amount = item.getAmount();
		Validate.isTrue(amount >= 0);
		if (amount == 0) return 0;

		// search for partially fitting item stacks:
		int maxStackSize = item.getMaxStackSize();
		int size = contents.length;
		for (int slot = 0; slot < size; slot++) {
			ItemStack slotItem = contents[slot];

			// slot empty? - skip, because we are currently filling existing item stacks up
			if (slotItem == null || slotItem.getType() == Material.AIR) continue;

			// slot already full?
			int slotAmount = slotItem.getAmount();
			if (slotAmount >= maxStackSize) continue;

			if (slotItem.isSimilar(item)) {
				int newAmount = slotAmount + amount;
				if (newAmount <= maxStackSize) {
					// remaining amount did fully fit into this stack:
					slotItem.setAmount(newAmount);
					return 0;
				} else {
					// did not fully fit:
					slotItem.setAmount(maxStackSize);
					amount -= (maxStackSize - slotAmount);
					assert amount != 0;
				}
			}
		}

		// we have items remaining:
		assert amount > 0;

		// search for free slots:
		for (int slot = 0; slot < size; slot++) {
			ItemStack slotItem = contents[slot];
			if (slotItem == null || slotItem.getType() == Material.AIR) {
				// found free slot:
				if (amount > maxStackSize) {
					// add full stack:
					ItemStack stack = item.clone();
					stack.setAmount(maxStackSize);
					contents[slot] = stack;
					amount -= maxStackSize;
				} else {
					// completely fits:
					ItemStack stack = item.clone(); // create a copy, just in case
					stack.setAmount(amount); // stack of remaining amount
					contents[slot] = stack;
					return 0;
				}
			}
		}

		// not all items did fit into the inventory:
		return amount;
	}

	/**
	 * Removes the given {@link ItemStack} from the given contents.
	 * If the amount of the given {@link ItemStack} is {@link Integer#MAX_VALUE}, then all similar items
	 * are being removed from the contents.
	 * 
	 * @param contents
	 *            The contents to remove the given {@link ItemStack} from.
	 * @param item
	 *            The {@link ItemStack} to remove from the given contents.
	 * @return The amount of items which couldn't be removed (0 on full success).
	 */
	public static int removeItems(ItemStack[] contents, ItemStack item) {
		Validate.notNull(contents);
		Validate.notNull(item);
		int amount = item.getAmount();
		Validate.isTrue(amount >= 0);
		if (amount == 0) return 0;

		boolean removeAll = (amount == Integer.MAX_VALUE);
		int size = contents.length;
		for (int slot = 0; slot < size; slot++) {
			ItemStack slotItem = contents[slot];
			if (slotItem == null) continue;
			if (item.isSimilar(slotItem)) {
				if (removeAll) {
					contents[slot] = null;
				} else {
					int newAmount = slotItem.getAmount() - amount;
					if (newAmount > 0) {
						slotItem.setAmount(newAmount);
						// all items were removed:
						return 0;
					} else {
						contents[slot] = null;
						amount = -newAmount;
						if (amount == 0) {
							// all items were removed:
							return 0;
						}
					}
				}
			}
		}

		if (removeAll) return 0;
		return amount;
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
	 * @param ignoreNameAndLore
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