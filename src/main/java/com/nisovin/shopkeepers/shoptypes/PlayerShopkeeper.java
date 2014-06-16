package com.nisovin.shopkeepers.shoptypes;

import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import com.nisovin.shopkeepers.ui.UIManager;
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

	protected abstract class PlayerShopEditorHandler extends EditorHandler {

		protected PlayerShopEditorHandler(UIManager uiManager, PlayerShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
		}

		@Override
		protected boolean canOpen(Player player) {
			return super.canOpen(player) && (((PlayerShopkeeper) this.shopkeeper).isOwner(player) || player.hasPermission("shopkeeper.bypass"));
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			int slot = event.getRawSlot();
			// prevent shift clicks on player inventory items:
			if (slot > 27 && event.isShiftClick()) {
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
							item.setType(Settings.zeroItem);
							item.setDurability((short) 0);
							item.setAmount(1);
						} else {
							item.setAmount(amount);
						}
					} else if (item.getType() == Settings.zeroItem) {
						item.setType(Settings.currencyItem);
						item.setDurability(Settings.currencyItemData);
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
							item.setType(Settings.highZeroItem);
							item.setDurability((short) 0);
							item.setAmount(1);
						} else {
							item.setAmount(amount);
						}
					} else if (item.getType() == Settings.highZeroItem) {
						item.setType(Settings.highCurrencyItem);
						item.setDurability(Settings.highCurrencyItemData);
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
						ItemStack item = new ItemStack(Settings.highCurrencyItem, highCost, Settings.highCurrencyItemData);
						if (highCost > item.getMaxStackSize()) {
							lowCost += (highCost - item.getMaxStackSize()) * Settings.highCurrencyValue;
							item.setAmount(item.getMaxStackSize());
						}
						inventory.setItem(column + 9, item);
					} else {
						inventory.setItem(column + 9, new ItemStack(Settings.highZeroItem));
					}
					if (lowCost > 0) {
						inventory.setItem(column + 18, new ItemStack(Settings.currencyItem, lowCost, Settings.currencyItemData));
					} else {
						inventory.setItem(column + 18, new ItemStack(Settings.zeroItem));
					}
				} else {
					inventory.setItem(column + 18, new ItemStack(Settings.currencyItem, cost, Settings.currencyItemData));
					if (Settings.highCurrencyItem != Material.AIR) {
						inventory.setItem(column + 9, new ItemStack(Settings.highZeroItem));
					}
				}
			} else {
				inventory.setItem(column + 18, new ItemStack(Settings.zeroItem));
				if (Settings.highCurrencyItem != Material.AIR) {
					inventory.setItem(column + 9, new ItemStack(Settings.highZeroItem));
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

	protected abstract class PlayerShopTradingHandler extends TradingHandler {

		protected PlayerShopTradingHandler(UIManager uiManager, PlayerShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
		}

		@Override
		protected void onPurchaseClick(InventoryClickEvent event, Player player) {
			if (Settings.preventTradingWithOwnShop && ((PlayerShopkeeper) this.shopkeeper).isOwner((Player) event.getWhoClicked()) && !event.getWhoClicked().isOp()) {
				event.setCancelled(true);
				Log.debug("Cancelled trade from " + event.getWhoClicked().getName() + " because he can't trade with his own shop");
				return;
			}

			// prevent unwanted special clicks
			if (!event.isLeftClick() || event.isShiftClick()) {
				event.setCancelled(true);
				return;
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

	protected class PlayerShopHiringHandler extends HiringHandler {

		protected PlayerShopHiringHandler(UIManager uiManager, PlayerShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
		}

		@Override
		protected boolean canOpen(Player player) {
			return ((PlayerShopkeeper) this.shopkeeper).isForHire() && super.canOpen(player);
		}

		@Override
		protected boolean openWindow(Player player) {
			Inventory inventory = Bukkit.createInventory(player, 9, ChatColor.translateAlternateColorCodes('&', Settings.forHireTitle));

			ItemStack hireItem = Settings.createHireButtonItem();
			inventory.setItem(2, hireItem);
			inventory.setItem(6, hireItem);

			ItemStack hireCost = ((PlayerShopkeeper) this.shopkeeper).getHireCost();
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
				ItemStack hireCost = ((PlayerShopkeeper) this.shopkeeper).getHireCost().clone();
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
					((PlayerShopkeeper) this.shopkeeper).setForHire(false, null);
					((PlayerShopkeeper) this.shopkeeper).setOwner(player);
					ShopkeepersPlugin.getInstance().save();
					Utils.sendMessage(player, Settings.msgHired);
				} else {
					// not enough money
					Utils.sendMessage(player, Settings.msgCantHire);
				}
				this.onClose(player);
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
			this.ownerUUID = UUID.fromString(config.getString("owner uuid"));
		} catch (Exception e) {
			// uuid invalid or non-existent, yet
			this.ownerUUID = null;
		}

		this.ownerName = config.getString("owner");
		this.chestx = config.getInt("chestx");
		this.chesty = config.getInt("chesty");
		this.chestz = config.getInt("chestz");
		this.forHire = config.getBoolean("forhire");
		this.hireCost = config.getItemStack("hirecost");
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		config.set("owner uuid", this.ownerUUID == null ? "unknown" : this.ownerUUID.toString());
		config.set("owner", this.ownerName);
		config.set("chestx", this.chestx);
		config.set("chesty", this.chesty);
		config.set("chestz", this.chestz);
		config.set("forhire", this.forHire);
		config.set("hirecost", this.hireCost);
	}

	/**
	 * Sets the owner of this shop (sets name and uuid).
	 * 
	 * @param player
	 *            the owner of this shop
	 */
	public void setOwner(Player player) {
		this.ownerUUID = player.getUniqueId();
		this.ownerName = player.getName();
		// TODO do this in a more abstract way
		if (!Settings.allowRenamingOfPlayerNpcShops && this.getShopObject().getObjectType() == DefaultShopObjectTypes.CITIZEN) {
			// update the npc's name:
			((CitizensShop) this.getShopObject()).setName(this.ownerName);
		}
	}

	/**
	 * Gets the uuid of the player who owns this shop.
	 * 
	 * @return the owners player uuid, or null if unknown
	 */
	public UUID getOwnerUUID() {
		return this.ownerUUID;
	}

	/**
	 * Gets the name of the player who owns this shop.
	 * 
	 * @return the owners player name
	 */
	public String getOwnerName() {
		return this.ownerName;
	}

	public String getOwnerAsString() {
		return this.ownerName + "(" + (this.ownerUUID != null ? this.ownerUUID.toString() : "unknown uuid") + ")";
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
		return this.ownerUUID != null && player.getUniqueId().equals(this.ownerUUID);
	}

	public boolean isForHire() {
		return this.forHire;
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
		return this.hireCost;
	}

	/**
	 * Checks whether this shop uses the indicated chest.
	 * 
	 * @param chest
	 *            the chest to check
	 * @return
	 */
	public boolean usesChest(Block chest) {
		if (!chest.getWorld().getName().equals(this.worldName)) return false;
		int x = chest.getX();
		int y = chest.getY();
		int z = chest.getZ();
		if (x == this.chestx && y == this.chesty && z == this.chestz) return true;
		if (x == this.chestx + 1 && y == this.chesty && z == this.chestz) return true;
		if (x == this.chestx - 1 && y == this.chesty && z == this.chestz) return true;
		if (x == this.chestx && y == this.chesty && z == this.chestz + 1) return true;
		if (x == this.chestx && y == this.chesty && z == this.chestz - 1) return true;
		return false;
	}

	public Block getChest() {
		return Bukkit.getWorld(this.worldName).getBlockAt(this.chestx, this.chesty, this.chestz);
	}

	protected void setRecipeCost(ItemStack[] recipe, int cost) {
		if (Settings.highCurrencyItem != Material.AIR && cost > Settings.highCurrencyMinCost) {
			int highCost = cost / Settings.highCurrencyValue;
			int lowCost = cost % Settings.highCurrencyValue;
			if (highCost > 0) {
				recipe[0] = new ItemStack(Settings.highCurrencyItem, highCost, Settings.highCurrencyItemData);
				if (highCost > recipe[0].getMaxStackSize()) {
					lowCost += (highCost - recipe[0].getMaxStackSize()) * Settings.highCurrencyValue;
					recipe[0].setAmount(recipe[0].getMaxStackSize());
				}
			}
			if (lowCost > 0) {
				recipe[1] = new ItemStack(Settings.currencyItem, lowCost, Settings.currencyItemData);
				if (lowCost > recipe[1].getMaxStackSize()) {
					Log.warning("Shopkeeper at " + this.worldName + "," + this.x + "," + this.y + "," + this.z + " owned by " + this.ownerName + " has an invalid cost!");
				}
			}
		} else {
			recipe[0] = new ItemStack(Settings.currencyItem, cost, Settings.currencyItemData);
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
}