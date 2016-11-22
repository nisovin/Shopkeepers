package com.nisovin.shopkeepers.shoptypes;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.Filter;
import com.nisovin.shopkeepers.ItemCount;
import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.ShopkeepersAPI;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.events.PlayerShopkeeperHiredEvent;
import com.nisovin.shopkeepers.shopobjects.CitizensShop;
import com.nisovin.shopkeepers.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.SignShop;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.HiringHandler;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;

/**
 * A shopkeeper that is managed by a player. This shopkeeper draws its supplies from a chest and will deposit earnings
 * back into that chest.
 */
public abstract class PlayerShopkeeper extends Shopkeeper {

	protected static abstract class PlayerShopEditorHandler extends EditorHandler {

		protected PlayerShopEditorHandler(UIType uiType, PlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		public PlayerShopkeeper getShopkeeper() {
			return (PlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean canOpen(Player player) {
			return super.canOpen(player) && (this.getShopkeeper().isOwner(player) || Utils.hasPermission(player, ShopkeepersAPI.BYPASS_PERMISSION));
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
				int column = slot - 18;
				ItemStack soldItem = event.getInventory().getItem(column);
				if (Utils.isEmpty(soldItem)) return;
				this.handleUpdateTradeCostItemOnClick(event, Settings.createCurrencyItem(1), Settings.createZeroCurrencyItem());
			} else if (slot >= 9 && slot <= 16) {
				// change high cost:
				int column = slot - 9;
				ItemStack soldItem = event.getInventory().getItem(column);
				if (Utils.isEmpty(soldItem)) return;
				this.handleUpdateTradeCostItemOnClick(event, Settings.createHighCurrencyItem(1), Settings.createHighZeroCurrencyItem());
			} else {
				super.onInventoryClick(event, player);
			}
		}

		protected void handleUpdateItemAmountOnClick(InventoryClickEvent event, int minAmount) {
			// cancel event:
			event.setCancelled(true);
			// ignore in certain situations:
			ItemStack clickedItem = event.getCurrentItem();
			if (Utils.isEmpty(clickedItem)) return;

			// get new item amount:
			int currentItemAmount = clickedItem.getAmount();
			if (minAmount <= 0) minAmount = 0;
			int newItemAmount = this.getNewAmountAfterEditorClick(event, currentItemAmount, minAmount, clickedItem.getMaxStackSize());
			assert newItemAmount >= minAmount;
			assert newItemAmount <= clickedItem.getMaxStackSize();

			// update item in inventory:
			if (newItemAmount == 0) {
				// empty item slot:
				event.setCurrentItem(null);
			} else {
				clickedItem.setAmount(newItemAmount);
			}
		}

		protected void handleUpdateTradeCostItemOnClick(InventoryClickEvent event, ItemStack currencyItem, ItemStack zeroCurrencyItem) {
			// cancel event:
			event.setCancelled(true);
			// ignore in certain situations:
			if (Utils.isEmpty(currencyItem)) return;

			// get new item amount:
			ItemStack clickedItem = event.getCurrentItem(); // can be null
			int currentItemAmount = 0;
			boolean isCurrencyItem = Utils.isSimilar(clickedItem, currencyItem);
			if (isCurrencyItem) {
				assert clickedItem != null;
				currentItemAmount = clickedItem.getAmount();
			}
			int newItemAmount = this.getNewAmountAfterEditorClick(event, currentItemAmount, 0, currencyItem.getMaxStackSize());
			assert newItemAmount >= 0;
			assert newItemAmount <= currencyItem.getMaxStackSize();

			// update item in inventory:
			if (newItemAmount == 0) {
				// place zero-currency item:
				event.setCurrentItem(zeroCurrencyItem);
			} else {
				if (isCurrencyItem) {
					// only update item amount of already existing currency item:
					clickedItem.setAmount(newItemAmount);
				} else {
					// place currency item with new amount:
					currencyItem.setAmount(newItemAmount);
					event.setCurrentItem(currencyItem);
				}
			}
		}

		protected void setEditColumnCost(Inventory inventory, int column, int cost) {
			if (cost > 0) {
				if (Settings.highCurrencyItem != Material.AIR && cost > Settings.highCurrencyMinCost) {
					int highCost = cost / Settings.highCurrencyValue;
					int lowCost = cost % Settings.highCurrencyValue;
					if (highCost > 0) {
						ItemStack item = Settings.createHighCurrencyItem(highCost);
						if (highCost > item.getMaxStackSize()) {
							lowCost += (highCost - item.getMaxStackSize()) * Settings.highCurrencyValue;
							item.setAmount(item.getMaxStackSize());
						}
						inventory.setItem(column + 9, item);
					} else {
						inventory.setItem(column + 9, Settings.createHighZeroCurrencyItem());
					}
					if (lowCost > 0) {
						ItemStack item = Settings.createCurrencyItem(lowCost);
						inventory.setItem(column + 18, item);
					} else {
						inventory.setItem(column + 18, Settings.createZeroCurrencyItem());
					}
				} else {
					ItemStack item = Settings.createCurrencyItem(cost);
					inventory.setItem(column + 18, item);
					if (Settings.highCurrencyItem != Material.AIR) {
						inventory.setItem(column + 9, Settings.createHighZeroCurrencyItem());
					}
				}
			} else {
				inventory.setItem(column + 18, Settings.createZeroCurrencyItem());
				if (Settings.highCurrencyItem != Material.AIR) {
					inventory.setItem(column + 9, Settings.createHighZeroCurrencyItem());
				}
			}
		}

		protected int getPriceFromColumn(Inventory inventory, int column) {
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
		public PlayerShopkeeper getShopkeeper() {
			return (PlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean canOpen(Player player) {
			if (!super.canOpen(player)) return false;
			final PlayerShopkeeper shopkeeper = this.getShopkeeper();

			// stop opening if trading shall be prevented while the owner is offline:
			if (Settings.preventTradingWhileOwnerIsOnline && !Utils.hasPermission(player, ShopkeepersAPI.BYPASS_PERMISSION)) {
				Player ownerPlayer = shopkeeper.getOwner();
				if (ownerPlayer != null) {
					Log.debug("Blocked trade window opening from " + player.getName() + " because the owner is online");
					Utils.sendMessage(player, Settings.msgCantTradeWhileOwnerOnline, "{owner}", ownerPlayer.getName());
					return false;
				}
			}
			return true;
		}

		@Override
		protected void onPurchaseClick(InventoryClickEvent event, Player player, ItemStack[] usedRecipe, ItemStack offered1, ItemStack offered2) {
			super.onPurchaseClick(event, player, usedRecipe, offered1, offered2);
			if (event.isCancelled()) return;
			final PlayerShopkeeper shopkeeper = this.getShopkeeper();

			if (Settings.preventTradingWithOwnShop && shopkeeper.isOwner(player) && !player.isOp()) {
				event.setCancelled(true);
				Log.debug("Cancelled trade from " + player.getName() + " because he can't trade with his own shop");
				return;
			}

			if (Settings.preventTradingWhileOwnerIsOnline && !Utils.hasPermission(player, ShopkeepersAPI.BYPASS_PERMISSION)) {
				Player ownerPlayer = shopkeeper.getOwner();
				if (ownerPlayer != null && !shopkeeper.isOwner(player)) {
					Utils.sendMessage(player, Settings.msgCantTradeWhileOwnerOnline, "{owner}", ownerPlayer.getName());
					event.setCancelled(true);
					Log.debug("Cancelled trade from " + event.getWhoClicked().getName() + " because the owner is online");
					return;
				}
			}
		}
	}

	protected static class PlayerShopHiringHandler extends HiringHandler {

		protected PlayerShopHiringHandler(UIType uiType, PlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		public PlayerShopkeeper getShopkeeper() {
			return (PlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean canOpen(Player player) {
			return this.getShopkeeper().isForHire() && super.canOpen(player);
		}

		@Override
		protected boolean openWindow(Player player) {
			final PlayerShopkeeper shopkeeper = this.getShopkeeper();
			Inventory inventory = Bukkit.createInventory(player, 9, Settings.forHireTitle);

			ItemStack hireItem = Settings.createHireButtonItem();
			inventory.setItem(2, hireItem);
			inventory.setItem(6, hireItem);

			ItemStack hireCost = shopkeeper.getHireCost();
			if (hireCost == null) return false;
			inventory.setItem(4, hireCost);

			player.openInventory(inventory);
			return true;
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, final Player player) {
			super.onInventoryClick(event, player);
			final PlayerShopkeeper shopkeeper = this.getShopkeeper();
			int slot = event.getRawSlot();
			if (slot == 2 || slot == 6) {
				ItemStack[] inventory = player.getInventory().getContents();
				ItemStack hireCost = shopkeeper.getHireCost().clone();
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
					int maxShops = ShopkeepersPlugin.getInstance().getMaxShops(player);

					// call event:
					PlayerShopkeeperHiredEvent hireEvent = new PlayerShopkeeperHiredEvent(player, shopkeeper, maxShops);
					Bukkit.getPluginManager().callEvent(hireEvent);
					if (hireEvent.isCancelled()) {
						// close window for this player:
						this.closeDelayed(player);
						return;
					}

					// check max shops limit:
					maxShops = hireEvent.getMaxShopsForPlayer();
					if (maxShops > 0) {
						int count = ShopkeepersPlugin.getInstance().countShopsOfPlayer(player);
						if (count >= maxShops) {
							Utils.sendMessage(player, Settings.msgTooManyShops);
							this.closeDelayed(player);
							return;
						}
					}

					// hire it:
					player.getInventory().setContents(inventory); // apply inventory changes
					shopkeeper.setForHire(false, null);
					shopkeeper.setOwner(player);
					ShopkeepersPlugin.getInstance().save();
					Utils.sendMessage(player, Settings.msgHired);

					// close all open windows for this shopkeeper:
					shopkeeper.closeAllOpenWindows();
					return;
				} else {
					// not enough money:
					Utils.sendMessage(player, Settings.msgCantHire);
					// close window for this player:
					this.closeDelayed(player);
				}
			}
		}
	}

	protected UUID ownerUUID; // not null after successful initialization
	protected String ownerName;
	protected int chestX;
	protected int chestY;
	protected int chestZ;
	protected boolean forHire = false;
	protected ItemStack hireCost = null;

	/**
	 * For use in extending classes.
	 */
	protected PlayerShopkeeper() {
	}

	@Override
	protected void initOnCreation(ShopCreationData creationData) throws ShopkeeperCreateException {
		super.initOnCreation(creationData);
		Player owner = creationData.creator;
		Block chest = creationData.chest;
		Validate.notNull(owner);
		Validate.notNull(chest);

		this.ownerUUID = owner.getUniqueId();
		this.ownerName = owner.getName();
		this.setChest(chest.getX(), chest.getY(), chest.getZ());
	}

	@Override
	protected void onInitDone() {
		super.onInitDone();
		this.registerUIHandler(new PlayerShopHiringHandler(DefaultUIs.HIRING_WINDOW, this));
	}

	@Override
	protected void onRegistration(int sessionId) {
		super.onRegistration(sessionId);

		// register protected chest:
		ShopkeepersPlugin.getInstance().getProtectedChests().addChest(worldName, chestX, chestY, chestZ, this);
	}

	@Override
	protected void onDeletion() {
		super.onDeletion();

		// unregister previously protected chest:
		ShopkeepersPlugin.getInstance().getProtectedChests().removeChest(worldName, chestX, chestY, chestZ, this);
	}

	@Override
	public boolean openChestWindow(Player player) {
		Log.debug("checking open chest window ..");
		// make sure the chest still exists
		Block chest = this.getChest();
		if (Utils.isChest(chest.getType())) {
			// open the chest directly as the player (no need for a custom UI)
			Log.debug("opening chest inventory window");
			Inventory inv = ((Chest) chest.getState()).getInventory();
			player.openInventory(inv);
			return true;
		}
		return false;
	}

	@Override
	protected void onPlayerInteraction(final Player player) {
		// naming via item:
		ItemStack itemInHand = player.getItemInHand();
		if (Settings.namingOfPlayerShopsViaItem && Settings.isNamingItem(itemInHand)) {
			// check if player can edit this shopkeeper:
			PlayerShopEditorHandler editorHandler = (PlayerShopEditorHandler) this.getUIHandler(DefaultUIs.EDITOR_WINDOW.getIdentifier());
			if (editorHandler.canOpen(player)) {
				// rename with the player's item in hand:
				String newName;
				ItemMeta itemMeta;
				if (!itemInHand.hasItemMeta() || (itemMeta = itemInHand.getItemMeta()) == null || !itemMeta.hasDisplayName()) {
					newName = "";
				} else {
					newName = itemMeta.getDisplayName();
				}

				// handled name changing:
				if (this.requestNameChange(player, newName)) {
					// manually remove rename item from player's hand after this event is processed:
					Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), new Runnable() {
						public void run() {
							ItemStack itemInHand = player.getItemInHand();
							if (itemInHand.getAmount() <= 1) {
								player.setItemInHand(null);
							} else {
								itemInHand.setAmount(itemInHand.getAmount() - 1);
								player.setItemInHand(itemInHand);
							}
						}
					});
				}

				return;
			}
		}

		// TODO what if something is replacing the default PlayerShopHiringHandler with some other kind of handler?
		PlayerShopHiringHandler hiringHandler = (PlayerShopHiringHandler) this.getUIHandler(DefaultUIs.HIRING_WINDOW.getIdentifier());
		if (!player.isSneaking() && hiringHandler.canOpen(player)) {
			// show hiring interface:
			this.openHireWindow(player);
		} else {
			super.onPlayerInteraction(player);
		}
	}

	@Override
	protected void load(ConfigurationSection config) throws ShopkeeperCreateException {
		super.load(config);
		try {
			ownerUUID = UUID.fromString(config.getString("owner uuid"));
		} catch (Exception e) {
			// uuid invalid or non-existent:
			throw new ShopkeeperCreateException("Missing owner uuid!");
		}
		ownerName = config.getString("owner", "unknown");

		if (!config.isInt("chestx") || !config.isInt("chesty") || !config.isInt("chestz")) {
			throw new ShopkeeperCreateException("Missing chest coordinate(s)");
		}

		// update chest:
		this.setChest(config.getInt("chestx"), config.getInt("chesty"), config.getInt("chestz"));

		forHire = config.getBoolean("forhire");
		hireCost = config.getItemStack("hirecost");
		if (forHire && hireCost == null) {
			Log.warning("Couldn't load hire cost! Disabling 'for hire' for shopkeeper at " + this.getPositionString());
			forHire = false;
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		config.set("owner uuid", ownerUUID.toString());
		config.set("owner", ownerName);
		config.set("chestx", chestX);
		config.set("chesty", chestY);
		config.set("chestz", chestZ);
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
		if (!Settings.allowRenamingOfPlayerNpcShops && this.getShopObject().getObjectType() == DefaultShopObjectTypes.CITIZEN()) {
			// update the npc's name:
			((CitizensShop) this.getShopObject()).setName(ownerName);
		} else if (this.getShopObject().getObjectType() == DefaultShopObjectTypes.SIGN()) {
			// update sign:
			((SignShop) this.getShopObject()).updateSign();
		}
	}

	/**
	 * Gets the uuid of the player who owns this shop.
	 * 
	 * @return the owners player uuid
	 */
	public UUID getOwnerUUID() {
		return ownerUUID;
	}

	/**
	 * Gets the last known name of the player who owns this shop.
	 * 
	 * @return the owners last known player name
	 */
	public String getOwnerName() {
		return ownerName;
	}

	public String getOwnerAsString() {
		return ownerName + "(" + ownerUUID.toString() + ")";
	}

	/**
	 * Checks if the given owner is owning this shop.
	 * 
	 * @param player
	 *            the player to check
	 * @return <code>true</code> if the given player owns this shop
	 */
	public boolean isOwner(Player player) {
		return player.getUniqueId().equals(ownerUUID);
	}

	/**
	 * Gets the owner of this shop IF he is online.
	 * 
	 * @return the owner of this shop, or <code>null</code> if the owner is offline
	 */
	public Player getOwner() {
		return Bukkit.getPlayer(ownerUUID);
	}

	public boolean isForHire() {
		return forHire;
	}

	public void setForHire(boolean forHire, ItemStack hireCost) {
		if (forHire) {
			Validate.notNull(hireCost);
		} else {
			Validate.isTrue(hireCost == null);
		}

		this.forHire = forHire;
		this.hireCost = hireCost;
		if (forHire) {
			this.setName(Settings.forHireTitle);
		} else {
			this.setName("");
		}
	}

	public ItemStack getHireCost() {
		return hireCost;
	}

	private void setChest(int chestX, int chestY, int chestZ) {
		if (this.isValid()) {
			// unregister previously protected chest:
			ShopkeepersPlugin.getInstance().getProtectedChests().removeChest(worldName, chestX, chestY, chestZ, this);
		}

		// update chest:
		this.chestX = chestX;
		this.chestY = chestY;
		this.chestZ = chestZ;

		if (this.isValid()) {
			// register new protected chest:
			ShopkeepersPlugin.getInstance().getProtectedChests().addChest(worldName, chestX, chestY, chestZ, this);
		}
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
		if (x == chestX && y == chestY && z == chestZ) return true;
		if (x == chestX + 1 && y == chestY && z == chestZ) return true;
		if (x == chestX - 1 && y == chestY && z == chestZ) return true;
		if (x == chestX && y == chestY && z == chestZ + 1) return true;
		if (x == chestX && y == chestY && z == chestZ - 1) return true;
		return false;
	}

	public Block getChest() {
		return Bukkit.getWorld(worldName).getBlockAt(chestX, chestY, chestZ);
	}

	protected void setRecipeCost(ItemStack[] recipe, int cost) {
		int lowCostSlot = 0;
		int lowCost = cost;

		if (Settings.highCurrencyItem != Material.AIR && cost > Settings.highCurrencyMinCost) {
			int highCost = cost / Settings.highCurrencyValue;
			lowCost = cost % Settings.highCurrencyValue;
			if (highCost > 0) {
				lowCostSlot = 1; // we put the high cost in the first slot instead
				ItemStack item = Settings.createHighCurrencyItem(highCost);
				recipe[0] = item;
				int maxStackSize = item.getMaxStackSize();
				if (highCost > maxStackSize) {
					item.setAmount(maxStackSize);
					lowCost += (highCost - maxStackSize) * Settings.highCurrencyValue;
				}
			}
		}

		if (lowCost > 0) {
			ItemStack item = Settings.createCurrencyItem(lowCost);
			recipe[lowCostSlot] = item;
			int maxStackSize = item.getMaxStackSize();
			if (lowCost > maxStackSize) {
				Log.warning("Shopkeeper at " + worldName + "," + x + "," + y + "," + z + " owned by " + ownerName + " has an invalid cost!");
			}
		}
	}

	protected int getCurrencyInChest() {
		int total = 0;
		Block chest = this.getChest();
		if (Utils.isChest(chest.getType())) {
			Inventory inv = ((Chest) chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (ItemStack item : contents) {
				if (Settings.isCurrencyItem(item)) {
					total += item.getAmount();
				} else if (Settings.isHighCurrencyItem(item)) {
					total += item.getAmount() * Settings.highCurrencyValue;
				}
			}
		}
		return total;
	}

	protected List<ItemCount> getItemsFromChest(Filter<ItemStack> filter) {
		Inventory chestInventory = null;
		Block chest = this.getChest();
		if (Utils.isChest(chest.getType())) {
			chestInventory = ((Chest) chest.getState()).getInventory();
		}
		return Utils.getItemCountsFromInventory(chestInventory, filter);
	}
}
