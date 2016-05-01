package com.nisovin.shopkeepers;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SkullType;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.Permissible;
import org.bukkit.util.Vector;

import com.nisovin.shopkeepers.compat.NMSManager;

public class Utils {

	public static final BlockFace[] chestProtectFaces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
	public static final BlockFace[] hopperProtectFaces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };

	public static boolean isChest(Material material) {
		return material == Material.CHEST || material == Material.TRAPPED_CHEST;
	}

	public static boolean isSign(Material material) {
		return material == Material.WALL_SIGN || material == Material.SIGN_POST || material == Material.SIGN;
	}

	// TODO temporary, due to a bukkit bug custom head item can currently not be saved
	public static boolean isCustomHeadItem(ItemStack item) {
		if (item == null) return false;
		if (item.getType() != Material.SKULL_ITEM) {
			return false;
		}
		if (item.getDurability() != SkullType.PLAYER.ordinal()) {
			return false;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta instanceof SkullMeta) {
			SkullMeta skullMeta = (SkullMeta) meta;
			if (skullMeta.hasOwner() && skullMeta.getOwner() == null) {
				// custom head items usually don't have a valid owner
				return true;
			}
		}
		return false;
	}

	public static boolean isProtectedChestAroundChest(Player player, Block chest) {
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) return false;
		for (BlockFace face : Utils.chestProtectFaces) {
			Block adjacentBlock = chest.getRelative(face);
			if (Utils.isChest(adjacentBlock.getType()) && plugin.isChestProtected(player, adjacentBlock)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isProtectedChestAroundHopper(Player player, Block hopper) {
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) return false;
		for (BlockFace face : Utils.hopperProtectFaces) {
			Block adjacentBlock = hopper.getRelative(face);
			if (Utils.isChest(adjacentBlock.getType()) && plugin.isChestProtected(player, adjacentBlock)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the given {@link BlockFace} is valid to be used for a wall sign.
	 * 
	 * @param blockFace
	 * @return
	 */
	public static boolean isWallSignFace(BlockFace blockFace) {
		return blockFace == BlockFace.NORTH || blockFace == BlockFace.SOUTH || blockFace == BlockFace.EAST || blockFace == BlockFace.WEST;
	}

	/**
	 * Determines the axis-aligned {@link BlockFace} for the given direction.
	 * If modY is zero only {@link BlockFace}s facing horizontal will be returned.
	 * This method takes into account that the values for EAST/WEST and NORTH/SOUTH
	 * were switched in some past version of bukkit. So it should also properly work
	 * with older bukkit versions.
	 * 
	 * @param modX
	 * @param modY
	 * @param modZ
	 * @return
	 */
	public static BlockFace getAxisBlockFace(double modX, double modY, double modZ) {
		double xAbs = Math.abs(modX);
		double yAbs = Math.abs(modY);
		double zAbs = Math.abs(modZ);

		if (xAbs >= zAbs) {
			if (xAbs >= yAbs) {
				if (modX >= 0.0D) {
					// EAST/WEST and NORTH/SOUTH values were switched in some past bukkit version:
					// with this additional checks it should work across different versions
					if (BlockFace.EAST.getModX() == 1) {
						return BlockFace.EAST;
					} else {
						return BlockFace.WEST;
					}
				} else {
					if (BlockFace.EAST.getModX() == 1) {
						return BlockFace.WEST;
					} else {
						return BlockFace.EAST;
					}
				}
			} else {
				if (modY >= 0.0D) {
					return BlockFace.UP;
				} else {
					return BlockFace.DOWN;
				}
			}
		} else {
			if (zAbs >= yAbs) {
				if (modZ >= 0.0D) {
					if (BlockFace.SOUTH.getModZ() == 1) {
						return BlockFace.SOUTH;
					} else {
						return BlockFace.NORTH;
					}
				} else {
					if (BlockFace.SOUTH.getModZ() == 1) {
						return BlockFace.NORTH;
					} else {
						return BlockFace.SOUTH;
					}
				}
			} else {
				if (modY >= 0.0D) {
					return BlockFace.UP;
				} else {
					return BlockFace.DOWN;
				}
			}
		}
	}

	/**
	 * Tries to find the nearest wall sign {@link BlockFace} facing towards the given direction.
	 * 
	 * @param direction
	 * @return a valid wall sign face
	 */
	public static BlockFace toWallSignFace(Vector direction) {
		assert direction != null;
		return getAxisBlockFace(direction.getX(), 0.0D, direction.getZ());
	}

	/**
	 * Gets the block face a player is looking at.
	 * 
	 * @param player
	 *            the player
	 * @param targetBlock
	 *            the block the player is looking at
	 * @return the block face, or null if none was found
	 */
	public static BlockFace getTargetBlockFace(Player player, Block targetBlock) {
		Location intersection = getBlockIntersection(player, targetBlock);
		if (intersection == null) return null;
		Location blockCenter = targetBlock.getLocation().add(0.5D, 0.5D, 0.5D);
		Vector centerToIntersection = intersection.subtract(blockCenter).toVector();
		double x = centerToIntersection.getX();
		double y = centerToIntersection.getY();
		double z = centerToIntersection.getZ();
		return getAxisBlockFace(x, y, z);
	}

	/**
	 * Determines the exact intersection point of a players view and a targeted block.
	 * 
	 * @param player
	 *            the player
	 * @param targetBlock
	 *            the block the player is looking at
	 * @return the intersection point of the players view and the target block,
	 *         or null if no intersection was found
	 */
	public static Location getBlockIntersection(Player player, Block targetBlock) {
		if (player == null || targetBlock == null) return null;

		// block bounds:
		double minX = targetBlock.getX();
		double minY = targetBlock.getY();
		double minZ = targetBlock.getZ();

		double maxX = minX + 1.0D;
		double maxY = minY + 1.0D;
		double maxZ = minZ + 1.0D;

		// ray origin:
		Location origin = player.getEyeLocation();
		double originX = origin.getX();
		double originY = origin.getY();
		double originZ = origin.getZ();

		// ray direction
		Vector dir = origin.getDirection();
		double dirX = dir.getX();
		double dirY = dir.getY();
		double dirZ = dir.getZ();

		// tiny improvement to save a few divisions below:
		double divX = 1.0D / dirX;
		double divY = 1.0D / dirY;
		double divZ = 1.0D / dirZ;

		// intersection interval:
		double t0 = 0.0D;
		double t1 = Double.MAX_VALUE;

		double tmin;
		double tmax;

		double tymin;
		double tymax;

		double tzmin;
		double tzmax;

		if (dirX >= 0.0D) {
			tmin = (minX - originX) * divX;
			tmax = (maxX - originX) * divX;
		} else {
			tmin = (maxX - originX) * divX;
			tmax = (minX - originX) * divX;
		}

		if (dirY >= 0.0D) {
			tymin = (minY - originY) * divY;
			tymax = (maxY - originY) * divY;
		} else {
			tymin = (maxY - originY) * divY;
			tymax = (minY - originY) * divY;
		}

		if ((tmin > tymax) || (tymin > tmax)) {
			return null;
		}

		if (tymin > tmin) tmin = tymin;
		if (tymax < tmax) tmax = tymax;

		if (dirZ >= 0.0D) {
			tzmin = (minZ - originZ) * divZ;
			tzmax = (maxZ - originZ) * divZ;
		} else {
			tzmin = (maxZ - originZ) * divZ;
			tzmax = (minZ - originZ) * divZ;
		}

		if ((tmin > tzmax) || (tzmin > tmax)) {
			return null;
		}

		if (tzmin > tmin) tmin = tzmin;
		if (tzmax < tmax) tmax = tzmax;

		if ((tmin >= t1) || (tmax <= t0)) {
			return null;
		}

		// intersection:
		Location intersection = origin.add(dir.multiply(tmin));
		return intersection;
	}

	// messages:

	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.US));
	static {
		DECIMAL_FORMAT.setGroupingUsed(false);
	}

	public static String getLocationString(Location location) {
		return getLocationString(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
	}

	public static String getLocationString(Block block) {
		return getLocationString(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	public static String getLocationString(String worldName, double x, double y, double z) {
		return worldName + "," + DECIMAL_FORMAT.format(x) + "," + DECIMAL_FORMAT.format(x) + "," + DECIMAL_FORMAT.format(x);
	}

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
					// TODO: this is a workaround: for some yet unknown reason entities sometimes report to be in a
					// different world..
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

	public static ItemStack createItemStack(Material type, int amount, short data, String displayName, List<String> lore) {
		// TODO return null in case of type AIR?
		ItemStack item = new ItemStack(type, amount, data);
		return setItemStackNameAndLore(item, displayName, lore);
	}

	public static ItemStack setItemStackNameAndLore(ItemStack item, String displayName, List<String> lore) {
		if (item == null) return null;
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(displayName);
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
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
	 * Same as {@link ItemStack#isSimilar(ItemStack)}, but taking into account that both given ItemStacks might be
	 * <code>null</code>.
	 * 
	 * @param item1
	 *            an itemstack
	 * @param item2
	 *            another itemstack
	 * @return <code>true</code> if the given item stacks are both <code>null</code> or similar
	 */
	public static boolean isSimilar(ItemStack item1, ItemStack item2) {
		if (item1 == null) return (item2 == null);
		return item1.isSimilar(item2);
	}

	/**
	 * Checks if the given item matches the specified attributes.
	 * 
	 * @param item
	 *            the item
	 * @param type
	 *            The item type.
	 * @param data
	 *            The data value/durability. If -1 is is ignored.
	 * @param displayName
	 *            The displayName. If null or empty it is ignored.
	 * @param lore
	 *            The item lore. If null or empty it is ignored.
	 * @return <code>true</code> if the item has similar attributes
	 */
	public static boolean isSimilar(ItemStack item, Material type, short data, String displayName, List<String> lore) {
		if (item == null) return false;
		if (item.getType() != type) return false;
		if (data != -1 && item.getDurability() != data) return false;

		boolean hasDisplayName = (displayName != null && !displayName.isEmpty());
		boolean hasLore = (lore != null && !lore.isEmpty());
		if (hasDisplayName || hasLore) {
			if (!item.hasItemMeta()) return false;
			ItemMeta itemMeta = item.getItemMeta();
			if (itemMeta == null) return false;
			if (hasDisplayName) {
				if (!itemMeta.hasDisplayName() || !displayName.equals(itemMeta.getDisplayName())) {
					return false;
				}
			}
			if (hasLore) {
				if (!itemMeta.hasLore() || !lore.equals(itemMeta.getLore())) {
					return false;
				}
			}
		} else {
			if (item.hasItemMeta()) {
				ItemMeta itemMeta = item.getItemMeta();
				assert itemMeta != null;
				if (itemMeta.hasDisplayName() || itemMeta.hasLore()) {
					return false;
				}
			}
		}

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
		// saving attributes manually, as they weren't saved by bukkit in the past:
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
		// loading separately stored attributes:
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
