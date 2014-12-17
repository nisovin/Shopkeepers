package com.nisovin.shopkeepers.shoptypes;

import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.shopobjects.CitizensShop;
import com.nisovin.shopkeepers.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.HiringHandler;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;

/**
 * A shopkeeper that is managed by a player. This shopkeeper draws its supplies from a chest that it
 * stands on, and will deposit earnings back into that chest.
 * 
 */
public abstract class PlayerShopkeeper extends Shopkeeper {

	protected static abstract class PlayerShopEditorHandler extends EditorHandler {

		protected PlayerShopEditorHandler(UIType uiType, PlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected boolean canOpen(Player player) {
			return super.canOpen(player) && (((PlayerShopkeeper) shopkeeper).isOwner(player) || player.hasPermission("shopkeeper.bypass"));
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			int slot = event.getRawSlot();

			// prevent shift clicks on player inventory items:
			if (slot >= 27 && event.isShiftClick()) {
				event.setCancelled(true);
			}
			if (slot >= 18 && slot <= 25) {
				// change low cost:
				event.setCancelled(true);
				ItemStack item = event.getCurrentItem();
				if (item != null) {
					if (item.getType() == Settings.currencyItem) {
						int amount = item.getAmount();
						amount = this.getNewAmountAfterEditorClick(event, amount);
						if (amount > 64) amount = 64;
						if (amount <= 0) {
							setZeroCurrencyItem(item);
							item.setAmount(1);
						} else {
							item.setAmount(amount);
						}
					} else if (item.getType() == Settings.zeroCurrencyItem) {
						setCurrencyItem(item);
						item.setAmount(1);
					}
				}
			} else if (slot >= 9 && slot <= 16) {
				// change high cost:
				event.setCancelled(true);
				ItemStack item = event.getCurrentItem();
				if (item != null && Settings.highCurrencyItem != Material.AIR) {
					if (item.getType() == Settings.highCurrencyItem) {
						int amount = item.getAmount();
						amount = this.getNewAmountAfterEditorClick(event, amount);
						if (amount > 64) amount = 64;
						if (amount <= 0) {
							setHighZeroCurrencyItem(item);
							item.setAmount(1);
						} else {
							item.setAmount(amount);
						}
					} else if (item.getType() == Settings.highZeroCurrencyItem) {
						setHighCurrencyItem(item);
						item.setAmount(1);
					}
				}
			} else {
				super.onInventoryClick(event, player);
			}
		}

		protected void setEditColumnCost(Inventory inventory, int column, int cost) {
			if (cost > 0) {
				if (Settings.highCurrencyItem != Material.AIR && cost > Settings.highCurrencyMinCost) {
					int highCost = cost / Settings.highCurrencyValue;
					int lowCost = cost % Settings.highCurrencyValue;
					if (highCost > 0) {
						ItemStack item = createHighCurrencyItem(highCost);
						if (highCost > item.getMaxStackSize()) {
							lowCost += (highCost - item.getMaxStackSize()) * Settings.highCurrencyValue;
							item.setAmount(item.getMaxStackSize());
						}
						inventory.setItem(column + 9, item);
					} else {
						inventory.setItem(column + 9, createHighZeroCurrencyItem());
					}
					if (lowCost > 0) {
						ItemStack item = createCurrencyItem(lowCost);
						inventory.setItem(column + 18, item);
					} else {
						inventory.setItem(column + 18, createZeroCurrencyItem());
					}
				} else {
					ItemStack item = createCurrencyItem(cost);
					inventory.setItem(column + 18, item);
					if (Settings.highCurrencyItem != Material.AIR) {
						inventory.setItem(column + 9, createHighZeroCurrencyItem());
					}
				}
			} else {
				inventory.setItem(column + 18, createZeroCurrencyItem());
				if (Settings.highCurrencyItem != Material.AIR) {
					inventory.setItem(column + 9, createHighZeroCurrencyItem());
				}
			}
		}

		protected int getCostFromColumn(Inventory inventory, int column) {
			ItemStack lowCostItem = inventory.getItem(column + 18);
			ItemStack highCostItem = inventory.getItem(column + 9);
			int cost = 0;
			if (lowCostItem != null && lowCostItem.getType() == Settings.currencyItem && lowCostItem.getAmount() > 0) {
				cost += lowCostItem.getAmount();
			}
			if (Settings.highCurrencyItem != Material.AIR && highCostItem != null && highCostItem.getType() == Settings.highCurrencyItem && highCostItem.getAmount() > 0) {
				cost += highCostItem.getAmount() * Settings.highCurrencyValue;
			}
			return cost;
		}
	}

	protected static abstract class PlayerShopTradingHandler extends TradingHandler {

		protected PlayerShopTradingHandler(UIType uiType, PlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected boolean canOpen(Player player) {
			if (!super.canOpen(player)) return false;

			// stop opening if trading shall be prevented while the owner is offline:
			if (Settings.preventTradingWhileOwnerIsOnline && !player.hasPermission("shopkeeper.bypass")) {
				Player ownerPlayer = ((PlayerShopkeeper) shopkeeper).getOwner();
				if (ownerPlayer != null) {
					Utils.sendMessage(player, Settings.msgCantTradeWhileOwnerOnline, "{owner}", ownerPlayer.getName());
					Log.debug("Blocked trade window opening from " + player.getName() + " because the owner is online");
					return false;
				}
			}
			return true;
		}

		@Override
		protected void onPurchaseClick(InventoryClickEvent event, Player player) {
			assert event.isLeftClick() && event.isShiftClick() ? this.isShiftTradeAllowed(event) : true;

			if (Settings.preventTradingWithOwnShop && ((PlayerShopkeeper) shopkeeper).isOwner(player) && !player.isOp()) {
				event.setCancelled(true);
				Log.debug("Cancelled trade from " + player.getName() + " because he can't trade with his own shop");
				return;
			}

			if (Settings.preventTradingWhileOwnerIsOnline && !player.hasPermission("shopkeeper.bypass")) {
				Player ownerPlayer = ((PlayerShopkeeper) shopkeeper).getOwner();
				if (ownerPlayer != null && !((PlayerShopkeeper) shopkeeper).isOwner(player)) {
					Utils.sendMessage(player, Settings.msgCantTradeWhileOwnerOnline, "{owner}", ownerPlayer.getName());
					event.setCancelled(true);
					Log.debug("Cancelled trade from " + event.getWhoClicked().getName() + " because the owner is online");
					return;
				}
			}
		}

		protected boolean removeFromInventory(ItemStack item, ItemStack[] contents) {
			item = item.clone();
			for (int i = 0; i < contents.length; i++) {
				if (contents[i] != null && item.isSimilar(contents[i])) {
					if (contents[i].getAmount() > item.getAmount()) {
						contents[i].setAmount(contents[i].getAmount() - item.getAmount());
						return true;
					} else if (contents[i].getAmount() == item.getAmount()) {
						contents[i] = null;
						return true;
					} else {
						item.setAmount(item.getAmount() - contents[i].getAmount());
						contents[i] = null;
					}
				}
			}
			return false;
		}

		protected boolean addToInventory(ItemStack item, ItemStack[] contents) {
			item = item.clone();
			for (int i = 0; i < contents.length; i++) {
				if (contents[i] == null) {
					contents[i] = item;
					return true;
				} else if (item.isSimilar(contents[i]) && contents[i].getAmount() != contents[i].getMaxStackSize()) {
					int amt = contents[i].getAmount() + item.getAmount();
					if (amt <= contents[i].getMaxStackSize()) {
						contents[i].setAmount(amt);
						return true;
					} else {
						item.setAmount(amt - contents[i].getMaxStackSize());
						contents[i].setAmount(contents[i].getMaxStackSize());
					}
				}
			}
			return false;
		}
	}

	protected static class PlayerShopHiringHandler extends HiringHandler {

		protected PlayerShopHiringHandler(UIType uiType, PlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected boolean canOpen(Player player) {
			return ((PlayerShopkeeper) shopkeeper).isForHire() && super.canOpen(player);
		}

		@Override
		protected boolean openWindow(Player player) {
			Inventory inventory = Bukkit.createInventory(player, 9, Utils.colorize(Settings.forHireTitle));

			ItemStack hireItem = Settings.createHireButtonItem();
			inventory.setItem(2, hireItem);
			inventory.setItem(6, hireItem);

			ItemStack hireCost = ((PlayerShopkeeper) shopkeeper).getHireCost();
			if (hireCost == null) return false;
			inventory.setItem(4, hireCost);

			player.openInventory(inventory);
			return true;
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			super.onInventoryClick(event, player);
			int slot = event.getRawSlot();
			if (slot == 2 || slot == 6) {
				ItemStack[] inventory = player.getInventory().getContents();
				ItemStack hireCost = ((PlayerShopkeeper) shopkeeper).getHireCost().clone();
				for (int i = 0; i < inventory.length; i++) {
					ItemStack item = inventory[i];
					if (item != null && item.isSimilar(hireCost)) {
						if (item.getAmount() > hireCost.getAmount()) {
							item.setAmount(item.getAmount() - hireCost.getAmount());
							hireCost.setAmount(0);
							break;
						} else if (item.getAmount() == hireCost.getAmount()) {
							inventory[i] = null;
							hireCost.setAmount(0);
							break;
						} else {
							hireCost.setAmount(hireCost.getAmount() - item.getAmount());
							inventory[i] = null;
						}
					}
				}
				if (hireCost.getAmount() == 0) {
					// hire it
					player.getInventory().setContents(inventory);
					((PlayerShopkeeper) shopkeeper).setForHire(false, null);
					((PlayerShopkeeper) shopkeeper).setOwner(player);
					ShopkeepersPlugin.getInstance().save();
					Utils.sendMessage(player, Settings.msgHired);
				} else {
					// not enough money
					Utils.sendMessage(player, Settings.msgCantHire);
				}
				this.informOnClose(player);
				Utils.closeInventoryLater(player);
			}
		}
	}

	protected UUID ownerUUID;
	protected String ownerName;
	protected int chestx;
	protected int chesty;
	protected int chestz;
	protected boolean forHire;
	protected ItemStack hireCost;

	protected PlayerShopkeeper(ConfigurationSection config) {
		super(config);
		this.onConstruction();
	}

	protected PlayerShopkeeper(ShopCreationData creationData) {
		super(creationData);
		Player owner = creationData.creator;
		Block chest = creationData.chest;
		Validate.notNull(owner);
		Validate.notNull(chest);

		this.ownerUUID = owner.getUniqueId();
		this.ownerName = owner.getName().toLowerCase();
		this.chestx = chest.getX();
		this.chesty = chest.getY();
		this.chestz = chest.getZ();
		this.forHire = false;

		this.onConstruction();
	}

	private final void onConstruction() {
		this.registerUIHandler(new PlayerShopHiringHandler(DefaultUIs.HIRING_WINDOW, this));
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		try {
			ownerUUID = UUID.fromString(config.getString("owner uuid"));
		} catch (Exception e) {
			// uuid invalid or non-existent, yet
			ownerUUID = null;
		}

		ownerName = config.getString("owner");
		chestx = config.getInt("chestx");
		chesty = config.getInt("chesty");
		chestz = config.getInt("chestz");
		forHire = config.getBoolean("forhire");
		hireCost = config.getItemStack("hirecost");
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		config.set("owner uuid", ownerUUID == null ? "unknown" : ownerUUID.toString());
		config.set("owner", ownerName);
		config.set("chestx", chestx);
		config.set("chesty", chesty);
		config.set("chestz", chestz);
		config.set("forhire", forHire);
		config.set("hirecost", hireCost);
	}

	/**
	 * Sets the owner of this shop (sets name and uuid).
	 * 
	 * @param player
	 *            the owner of this shop
	 */
	public void setOwner(Player player) {
		this.setOwner(player.getUniqueId(), player.getName());
	}

	public void setOwner(UUID ownerUUID, String ownerName) {
		this.ownerUUID = ownerUUID;
		this.ownerName = ownerName;
		// TODO do this in a more abstract way
		if (!Settings.allowRenamingOfPlayerNpcShops && this.getShopObject().getObjectType() == DefaultShopObjectTypes.CITIZEN) {
			// update the npc's name:
			((CitizensShop) this.getShopObject()).setName(ownerName);
		}
	}

	/**
	 * Gets the uuid of the player who owns this shop.
	 * 
	 * @return the owners player uuid, or null if unknown
	 */
	public UUID getOwnerUUID() {
		return ownerUUID;
	}

	/**
	 * Gets the name of the player who owns this shop.
	 * 
	 * @return the owners player name
	 */
	public String getOwnerName() {
		return ownerName;
	}

	public String getOwnerAsString() {
		return ownerName + "(" + (ownerUUID != null ? ownerUUID.toString() : "unknown uuid") + ")";
	}

	/**
	 * Checks if the given owner is owning this shop.
	 * 
	 * @param player
	 *            the player to check
	 * @return true, if the given player owns this shop
	 */
	public boolean isOwner(Player player) {
		// the player is online, so this shopkeeper should already have an uuid assigned if that player is the owner:
		return ownerUUID != null && player.getUniqueId().equals(ownerUUID);
	}

	/**
	 * Gets the owner of this shop IF he is online.
	 * 
	 * @return the owner of this shop, null if the owner is offline
	 */
	public Player getOwner() {
		// owner name should always be given, so try with that first
		// afterwards compare uuids to be sure:
		Player ownerPlayer = Bukkit.getPlayer(ownerName);
		if (ownerPlayer != null && ownerPlayer.getUniqueId().equals(ownerUUID)) {
			return ownerPlayer;
		}
		return null;
	}

	public boolean isForHire() {
		return forHire;
	}

	public void setForHire(boolean forHire, ItemStack hireCost) {
		this.forHire = forHire;
		this.hireCost = hireCost;
		if (forHire) {
			this.setName(Settings.forHireTitle);
		} else {
			this.setName(null);
		}
	}

	public ItemStack getHireCost() {
		return hireCost;
	}

	/**
	 * Checks whether this shop uses the indicated chest.
	 * 
	 * @param chest
	 *            the chest to check
	 * @return
	 */
	public boolean usesChest(Block chest) {
		if (!chest.getWorld().getName().equals(worldName)) return false;
		int x = chest.getX();
		int y = chest.getY();
		int z = chest.getZ();
		if (x == chestx && y == chesty && z == chestz) return true;
		if (x == chestx + 1 && y == chesty && z == chestz) return true;
		if (x == chestx - 1 && y == chesty && z == chestz) return true;
		if (x == chestx && y == chesty && z == chestz + 1) return true;
		if (x == chestx && y == chesty && z == chestz - 1) return true;
		return false;
	}

	public Block getChest() {
		return Bukkit.getWorld(worldName).getBlockAt(chestx, chesty, chestz);
	}

	protected void setRecipeCost(ItemStack[] recipe, int cost) {
		if (Settings.highCurrencyItem != Material.AIR && cost > Settings.highCurrencyMinCost) {
			int highCost = cost / Settings.highCurrencyValue;
			int lowCost = cost % Settings.highCurrencyValue;
			if (highCost > 0) {
				ItemStack item = new ItemStack(Settings.highCurrencyItem, highCost, Settings.highCurrencyItemData);
				Utils.setItemStackNameAndLore(item, Settings.highCurrencyItemName, Settings.highCurrencyItemLore);
				recipe[0] = item;
				if (highCost > recipe[0].getMaxStackSize()) {
					lowCost += (highCost - recipe[0].getMaxStackSize()) * Settings.highCurrencyValue;
					recipe[0].setAmount(recipe[0].getMaxStackSize());
				}
			}
			if (lowCost > 0) {
				ItemStack item = new ItemStack(Settings.currencyItem, lowCost, Settings.currencyItemData);
				Utils.setItemStackNameAndLore(item, Settings.currencyItemName, Settings.currencyItemLore);
				recipe[1] = item;
				if (lowCost > recipe[1].getMaxStackSize()) {
					Log.warning("Shopkeeper at " + worldName + "," + x + "," + y + "," + z + " owned by " + ownerName + " has an invalid cost!");
				}
			}
		} else {
			ItemStack item = new ItemStack(Settings.currencyItem, cost, Settings.currencyItemData);
			Utils.setItemStackNameAndLore(item, Settings.currencyItemName, Settings.currencyItemLore);
			recipe[0] = item;
		}
	}

	@Override
	protected void onPlayerInteraction(Player player) {
		// TODO what if something is replacing the default PlayerShopHiringHandler with some other kind of handler?
		PlayerShopHiringHandler hiringHandler = (PlayerShopHiringHandler) this.getUIHandler(DefaultUIs.HIRING_WINDOW.getIdentifier());
		if (!player.isSneaking() && hiringHandler.canOpen(player)) {
			// show hiring interface:
			this.openHireWindow(player);
		} else {
			super.onPlayerInteraction(player);
		}
	}

	// item utilities:

	// currency item:
	public static ItemStack createCurrencyItem(int amount) {
		ItemStack item = new ItemStack(Settings.currencyItem, amount, Settings.currencyItemData);
		return Utils.setItemStackNameAndLore(item, Settings.currencyItemName, Settings.currencyItemLore);
	}

	public static void setCurrencyItem(ItemStack item) {
		if (item == null) return;
		item.setType(Settings.currencyItem);
		item.setDurability(Settings.currencyItemData);
		Utils.setItemStackNameAndLore(item, Settings.currencyItemName, Settings.currencyItemLore);
	}

	public static boolean isCurrencyItem(ItemStack item) {
		return Utils.isSimilar(item, Settings.currencyItem, Settings.currencyItemData, Settings.currencyItemName, Settings.currencyItemLore);
	}

	// high currency item:
	public static ItemStack createHighCurrencyItem(int amount) {
		ItemStack item = new ItemStack(Settings.highCurrencyItem, amount, Settings.highCurrencyItemData);
		return Utils.setItemStackNameAndLore(item, Settings.highCurrencyItemName, Settings.highCurrencyItemLore);
	}

	public static void setHighCurrencyItem(ItemStack item) {
		if (item == null) return;
		item.setType(Settings.highCurrencyItem);
		item.setDurability(Settings.highCurrencyItemData);
		Utils.setItemStackNameAndLore(item, Settings.highCurrencyItemName, Settings.highCurrencyItemLore);
	}

	public static boolean isHighCurrencyItem(ItemStack item) {
		return Utils.isSimilar(item, Settings.highCurrencyItem, Settings.highCurrencyItemData, Settings.highCurrencyItemName, Settings.highCurrencyItemLore);
	}

	// zero currency item:
	public static ItemStack createZeroCurrencyItem() {
		ItemStack item = new ItemStack(Settings.zeroCurrencyItem, 1, Settings.zeroCurrencyItemData);
		return Utils.setItemStackNameAndLore(item, Settings.zeroCurrencyItemName, Settings.zeroCurrencyItemLore);
	}

	public static void setZeroCurrencyItem(ItemStack item) {
		if (item == null) return;
		item.setType(Settings.zeroCurrencyItem);
		item.setDurability(Settings.zeroCurrencyItemData);
		Utils.setItemStackNameAndLore(item, Settings.zeroCurrencyItemName, Settings.zeroCurrencyItemLore);
	}

	public static boolean isZeroCurrencyItem(ItemStack item) {
		return Utils.isSimilar(item, Settings.zeroCurrencyItem, Settings.zeroCurrencyItemData, Settings.zeroCurrencyItemName, Settings.zeroCurrencyItemLore);
	}

	// high zero currency item:
	public static ItemStack createHighZeroCurrencyItem() {
		ItemStack item = new ItemStack(Settings.highZeroCurrencyItem, 1, Settings.highZeroCurrencyItemData);
		return Utils.setItemStackNameAndLore(item, Settings.highZeroCurrencyItemName, Settings.highZeroCurrencyItemLore);
	}

	public static void setHighZeroCurrencyItem(ItemStack item) {
		if (item == null) return;
		item.setType(Settings.highZeroCurrencyItem);
		item.setDurability(Settings.highZeroCurrencyItemData);
		Utils.setItemStackNameAndLore(item, Settings.highZeroCurrencyItemName, Settings.highZeroCurrencyItemLore);
	}

	public static boolean isHighZeroCurrencyItem(ItemStack item) {
		return Settings.highZeroCurrencyItem != Material.AIR && Utils.isSimilar(item, Settings.highZeroCurrencyItem, Settings.highZeroCurrencyItemData, Settings.highZeroCurrencyItemName, Settings.highZeroCurrencyItemLore);
	}
}