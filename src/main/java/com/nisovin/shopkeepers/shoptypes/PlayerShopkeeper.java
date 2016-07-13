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
 * A shopkeeper that is managed by a player. This shopkeeper draws its supplies from a chest and will deposit earnings back into that chest.
 */
public abstract class PlayerShopkeeper extends Shopkeeper {

	protected static abstract class PlayerShopEditorHandler extends EditorHandler {

		protected PlayerShopEditorHandler(UIType uiType, PlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected boolean canOpen(Player player) {
			return super.canOpen(player) && (((PlayerShopkeeper) shopkeeper).isOwner(player) || Utils.hasPermission(player, ShopkeepersAPI.BYPASS_PERMISSION));
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

				int column = slot - 18;
				ItemStack soldItem = event.getInventory().getItem(column);
				if (soldItem != null && soldItem.getType() != Material.AIR) {
					ItemStack item = event.getCurrentItem(); // can be null
					Material itemType = item == null ? Material.AIR : item.getType();
					if (itemType == Settings.currencyItem) {
						assert Settings.currencyItem != Material.AIR;
						assert item != null;
						int itemAmount = item.getAmount();
						itemAmount = this.getNewAmountAfterEditorClick(event, itemAmount);
						if (itemAmount > 64) itemAmount = 64;
						if (itemAmount <= 0) {
							event.setCurrentItem(createZeroCurrencyItem());
						} else {
							item.setAmount(itemAmount);
						}
					} else if (itemType == Settings.zeroCurrencyItem) {
						// note: item might be null
						event.setCurrentItem(createCurrencyItem(1));
					}
				}
			} else if (slot >= 9 && slot <= 16) {
				// change high cost:
				event.setCancelled(true);

				int column = slot - 9;
				ItemStack soldItem = event.getInventory().getItem(column);
				if (soldItem != null && soldItem.getType() != Material.AIR) {
					ItemStack item = event.getCurrentItem(); // can be null
					if (Settings.highCurrencyItem != Material.AIR) {
						Material itemType = item == null ? Material.AIR : item.getType();
						if (itemType == Settings.highCurrencyItem) {
							assert Settings.highCurrencyItem != Material.AIR;
							assert item != null;
							int itemAmount = item.getAmount();
							itemAmount = this.getNewAmountAfterEditorClick(event, itemAmount);
							if (itemAmount > 64) itemAmount = 64;
							if (itemAmount <= 0) {
								event.setCurrentItem(createHighZeroCurrencyItem());
							} else {
								item.setAmount(itemAmount);
							}
						} else if (itemType == Settings.highZeroCurrencyItem) {
							// note: item might be null
							event.setCurrentItem(createHighCurrencyItem(1));
						}
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
		protected boolean canOpen(Player player) {
			if (!super.canOpen(player)) return false;

			// stop opening if trading shall be prevented while the owner is offline:
			if (Settings.preventTradingWhileOwnerIsOnline && !Utils.hasPermission(player, ShopkeepersAPI.BYPASS_PERMISSION)) {
				Player ownerPlayer = ((PlayerShopkeeper) shopkeeper).getOwner();
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

			if (Settings.preventTradingWithOwnShop && ((PlayerShopkeeper) shopkeeper).isOwner(player) && !player.isOp()) {
				event.setCancelled(true);
				Log.debug("Cancelled trade from " + player.getName() + " because he can't trade with his own shop");
				return;
			}

			if (Settings.preventTradingWhileOwnerIsOnline && !Utils.hasPermission(player, ShopkeepersAPI.BYPASS_PERMISSION)) {
				Player ownerPlayer = ((PlayerShopkeeper) shopkeeper).getOwner();
				if (ownerPlayer != null && !((PlayerShopkeeper) shopkeeper).isOwner(player)) {
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
		protected boolean canOpen(Player player) {
			return ((PlayerShopkeeper) shopkeeper).isForHire() && super.canOpen(player);
		}

		@Override
		protected boolean openWindow(Player player) {
			Inventory inventory = Bukkit.createInventory(player, 9, Settings.forHireTitle);

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
		protected void onInventoryClick(InventoryClickEvent event, final Player player) {
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
					int maxShops = ShopkeepersPlugin.getInstance().getMaxShops(player);

					// call event:
					PlayerShopkeeperHiredEvent hireEvent = new PlayerShopkeeperHiredEvent(player, (PlayerShopkeeper) shopkeeper, maxShops);
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
					((PlayerShopkeeper) shopkeeper).setForHire(false, null);
					((PlayerShopkeeper) shopkeeper).setOwner(player);
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
	protected int chestx;
	protected int chesty;
	protected int chestz;
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
		this.chestx = chest.getX();
		this.chesty = chest.getY();
		this.chestz = chest.getZ();
	}

	@Override
	protected void onInitDone() {
		super.onInitDone();
		this.registerUIHandler(new PlayerShopHiringHandler(DefaultUIs.HIRING_WINDOW, this));
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
		chestx = config.getInt("chestx");
		chesty = config.getInt("chesty");
		chestz = config.getInt("chestz");
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
		int lowCostSlot = 0;
		int lowCost = cost;

		if (Settings.highCurrencyItem != Material.AIR && cost > Settings.highCurrencyMinCost) {
			int highCost = cost / Settings.highCurrencyValue;
			lowCost = cost % Settings.highCurrencyValue;
			if (highCost > 0) {
				lowCostSlot = 1; // we put the high cost in the first slot instead
				ItemStack item = createHighCurrencyItem(highCost);
				recipe[0] = item;
				int maxStackSize = item.getMaxStackSize();
				if (highCost > maxStackSize) {
					item.setAmount(maxStackSize);
					lowCost += (highCost - maxStackSize) * Settings.highCurrencyValue;
				}
			}
		}

		if (lowCost > 0) {
			ItemStack item = createCurrencyItem(lowCost);
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
				if (isCurrencyItem(item)) {
					total += item.getAmount();
				} else if (isHighCurrencyItem(item)) {
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

	// item utilities:

	// currency item:
	public static ItemStack createCurrencyItem(int amount) {
		return Utils.createItemStack(Settings.currencyItem, amount, Settings.currencyItemData,
				Settings.currencyItemName, Settings.currencyItemLore);
	}

	public static boolean isCurrencyItem(ItemStack item) {
		return Utils.isSimilar(item, Settings.currencyItem, Settings.currencyItemData,
				Settings.currencyItemName, Settings.currencyItemLore);
	}

	// high currency item:
	public static ItemStack createHighCurrencyItem(int amount) {
		return Utils.createItemStack(Settings.highCurrencyItem, amount, Settings.highCurrencyItemData,
				Settings.highCurrencyItemName, Settings.highCurrencyItemLore);
	}

	public static boolean isHighCurrencyItem(ItemStack item) {
		return Utils.isSimilar(item, Settings.highCurrencyItem, Settings.highCurrencyItemData,
				Settings.highCurrencyItemName, Settings.highCurrencyItemLore);
	}

	// zero currency item:
	public static ItemStack createZeroCurrencyItem() {
		return Utils.createItemStack(Settings.zeroCurrencyItem, 1, Settings.zeroCurrencyItemData,
				Settings.zeroCurrencyItemName, Settings.zeroCurrencyItemLore);
	}

	public static boolean isZeroCurrencyItem(ItemStack item) {
		if (Settings.zeroCurrencyItem == Material.AIR && item == null) return true;
		return Utils.isSimilar(item, Settings.zeroCurrencyItem, Settings.zeroCurrencyItemData,
				Settings.zeroCurrencyItemName, Settings.zeroCurrencyItemLore);
	}

	// high zero currency item:
	public static ItemStack createHighZeroCurrencyItem() {
		return Utils.createItemStack(Settings.highZeroCurrencyItem, 1, Settings.highZeroCurrencyItemData,
				Settings.highZeroCurrencyItemName, Settings.highZeroCurrencyItemLore);
	}

	public static boolean isHighZeroCurrencyItem(ItemStack item) {
		if (Settings.highZeroCurrencyItem == Material.AIR && item == null) return true;
		return Utils.isSimilar(item, Settings.highZeroCurrencyItem, Settings.highZeroCurrencyItemData,
				Settings.highZeroCurrencyItemName, Settings.highZeroCurrencyItemLore);
	}
}
