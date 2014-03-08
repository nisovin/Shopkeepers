package com.nisovin.shopkeepers;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.shopobjects.ShopObject;
import com.nisovin.shopkeepers.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.shoptypes.ShopkeeperType;

public abstract class Shopkeeper {

	protected ShopObject shopObject;
	protected String worldName;
	protected int x;
	protected int y;
	protected int z;
	protected String name;

	public Shopkeeper(ConfigurationSection config) {
		load(config);
	}

	/**
	 * Creates a new shopkeeper and spawns it in the world. This should be used when a player is
	 * creating a new shopkeeper.
	 * 
	 * @param location
	 *            the location to spawn at
	 * @param objectType
	 *            the ShopObjectType of this shopkeeper
	 */
	public Shopkeeper(Location location, ShopObjectType objectType) {
		worldName = location.getWorld().getName();
		x = location.getBlockX();
		y = location.getBlockY();
		z = location.getBlockZ();
		shopObject = objectType.createObject(this);
	}

	/**
	 * Loads a shopkeeper's saved data from a config section of a config file.
	 * 
	 * @param config
	 *            the config section
	 */
	public void load(ConfigurationSection config) {
		name = config.getString("name");
		worldName = config.getString("world");
		x = config.getInt("x");
		y = config.getInt("y");
		z = config.getInt("z");
		ShopObjectType objectType = ShopObjectType.getTypeFromName(config.getString("object"));
		shopObject = objectType.createObject(this);
		shopObject.load(config);
	}

	/**
	 * Saves the shopkeeper's data to the specified configuration section.
	 * 
	 * @param config
	 *            the config section
	 */
	public void save(ConfigurationSection config) {
		config.set("name", name);
		config.set("world", worldName);
		config.set("x", x);
		config.set("y", y);
		config.set("z", z);
		shopObject.save(config);
	}

	/**
	 * Gets the type of this shopkeeper (admin, normal player, book player, or buying player).
	 * 
	 * @return the shopkeeper type
	 */
	public abstract ShopkeeperType getType();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		shopObject.setName(name);
	}

	public ShopObject getShopObject() {
		return shopObject;
	}

	public boolean needsSpawned() {
		return shopObject.needsSpawned();
	}

	/**
	 * Spawns the shopkeeper into the world at its spawn location. Also sets the
	 * trade recipes and overwrites the villager AI.
	 */
	public boolean spawn() {
		return shopObject.spawn();
	}

	/**
	 * Checks if the shopkeeper is active (is alive in the world).
	 * 
	 * @return whether the shopkeeper is active
	 */
	public boolean isActive() {
		return shopObject.isActive();
	}

	/**
	 * Teleports this shopkeeper to its spawn location.
	 * 
	 * @return whether to update this shopkeeper in the collection
	 */
	public boolean teleport() {
		return shopObject.check();
	}

	/**
	 * Removes this shopkeeper from the world.
	 */
	public void remove() {
		shopObject.despawn();
	}

	protected void delete() {
		shopObject.delete();
	}

	/**
	 * Gets a string identifying the chunk this shopkeeper spawns in,
	 * in the format world,x,z.
	 * 
	 * @return the chunk as a string
	 */
	public String getChunk() {
		return worldName + "," + (x >> 4) + "," + (z >> 4);
	}

	public String getPositionString() {
		return worldName + "," + x + "," + y + "," + z;
	}

	public Location getActualLocation() {
		return shopObject.getActualLocation();
	}

	/**
	 * Gets the name of the world this shopkeeper lives in.
	 * 
	 * @return the world name
	 */
	public String getWorldName() {
		return worldName;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	/**
	 * Gets the shopkeeper's ID.
	 * 
	 * @return the id, or 0 if the shopkeeper is not in the world
	 */
	public String getId() {
		return shopObject.getId();
	}

	/**
	 * Gets the shopkeeper's trade recipes. This will be a list of ItemStack[3],
	 * where the first two elemets of the ItemStack[] array are the cost, and the third
	 * element is the trade result (the item sold by the shopkeeper).
	 * 
	 * @return the trade recipes of this shopkeeper
	 */
	public abstract List<ItemStack[]> getRecipes();

	/**
	 * Called when a player shift-right-clicks on the villager in an attempt to edit
	 * the shopkeeper information. This method should open the editing interface.
	 * 
	 * @param player
	 *            the player doing the edit
	 * @return whether the player is now editing (returns false if permission fails)
	 */
	public abstract boolean onEdit(Player player);

	/**
	 * Called when a player clicks on any slot in the editor window.
	 * 
	 * @param event
	 *            the click event
	 * @return how the main plugin should handle the click
	 */
	public EditorClickResult onEditorClick(InventoryClickEvent event) {
		// check for special buttons
		if (event.getRawSlot() == 8) {
			// it's the name button - ask for new name
			event.setCancelled(true);
			saveEditor(event.getInventory(), (Player) event.getWhoClicked());
			return EditorClickResult.SET_NAME;
		} else if (event.getRawSlot() == 17) {
			// it's the cycle button - cycle to next type
			if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
				shopObject.setItem(event.getCursor().clone());
			} else {
				shopObject.cycleType();
				ItemStack typeItem = shopObject.getTypeItem();
				if (typeItem != null) {
					event.getInventory().setItem(17, ItemUtils.setItemStackNameAndLore(typeItem, Settings.msgButtonType, Settings.msgButtonTypeLore));
				}
			}
			event.setCancelled(true);
			return EditorClickResult.SAVE_AND_CONTINUE;
		} else if (event.getRawSlot() == 26) {
			// it's the delete button - remove the shopkeeper
			delete();
			event.setCancelled(true);
			return EditorClickResult.DELETE_SHOPKEEPER;
		} else {
			return EditorClickResult.NOTHING;
		}
	}

	protected abstract void saveEditor(Inventory inv, Player player);

	/**
	 * Called when a player closes the editor window.
	 * 
	 * @param event
	 *            the close event
	 */
	public abstract void onEditorClose(InventoryCloseEvent event);

	/**
	 * Called when a player purchases an item from a shopkeeper.
	 * 
	 * @param event
	 *            the click event of the purchase
	 */
	public abstract void onPurchaseClick(InventoryClickEvent event);

	protected void closeInventory(HumanEntity player) {
		ShopkeepersPlugin.plugin.closeInventory(player);
	}

	protected int getNewAmountAfterEditorClick(int amount, InventoryClickEvent event) {
		if (event.isLeftClick()) {
			if (event.isShiftClick()) {
				amount += 10;
			} else {
				amount += 1;
			}
		} else if (event.isRightClick()) {
			if (event.isShiftClick()) {
				amount -= 10;
			} else {
				amount -= 1;
			}
		} else if (event.getClick() == ClickType.MIDDLE) {
			amount = 64;
		} else if (event.getHotbarButton() >= 0) {
			amount = event.getHotbarButton();
		}
		return amount;
	}

	protected void setActionButtons(Inventory inv) {
		inv.setItem(8, Settings.createNameButtonItem());
		ItemStack typeItem = shopObject.getTypeItem();
		if (typeItem != null) {
			inv.setItem(17, ItemUtils.setItemStackNameAndLore(typeItem, Settings.msgButtonType, Settings.msgButtonTypeLore));
		}
		inv.setItem(26, Settings.createDeleteButtonItem());
	}

	protected int getAmountAfterTaxes(int amount) {
		if (Settings.taxRate == 0) return amount;
		int taxes = 0;
		if (Settings.taxRoundUp) {
			taxes = (int) Math.ceil((double) amount * (Settings.taxRate / 100F));
		} else {
			taxes = (int) Math.floor((double) amount * (Settings.taxRate / 100F));
		}
		return amount - taxes;
	}
}