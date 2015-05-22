package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;

/**
 * Represents a shopkeeper that is managed by an admin. This shopkeeper will have unlimited supply
 * and will not save earnings anywhere.
 * 
 */
public class AdminShopkeeper extends Shopkeeper {

	protected static class AdminShopEditorHandler extends EditorHandler {

		protected AdminShopEditorHandler(UIType uiType, AdminShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected boolean openWindow(Player player) {
			// add the shopkeeper's trade offers:
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);
			List<ItemStack[]> recipes = ((AdminShopkeeper) shopkeeper).getRecipes();
			for (int column = 0; column < recipes.size() && column < 8; column++) {
				ItemStack[] recipe = recipes.get(column);
				inventory.setItem(column, recipe[0]);
				inventory.setItem(column + 9, recipe[1]);
				inventory.setItem(column + 18, recipe[2]);
			}
			// add the special buttons:
			this.setActionButtons(inventory);
			// show editing inventory:
			player.openInventory(inventory);
			return true;
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
			for (int column = 0; column < 8; column++) {
				ItemStack cost1 = inventory.getItem(column);
				ItemStack cost2 = inventory.getItem(column + 9);
				ItemStack result = inventory.getItem(column + 18);
				if (cost1 != null && result != null) {
					// save trade recipe:
					ItemStack[] recipe = new ItemStack[3];
					recipe[0] = cost1;
					recipe[1] = cost2;
					recipe[2] = result;
					recipes.add(recipe);
				} else if (player != null) {
					// return unused items to inventory:
					if (cost1 != null) {
						player.getInventory().addItem(cost1);
					}
					if (cost2 != null) {
						player.getInventory().addItem(cost2);
					}
					if (result != null) {
						player.getInventory().addItem(result);
					}
				}
			}
			((AdminShopkeeper) shopkeeper).setRecipes(recipes);
		}
	}

	protected static class AdminShopTradingHandler extends TradingHandler {

		protected AdminShopTradingHandler(UIType uiType, AdminShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected boolean isShiftTradeAllowed(InventoryClickEvent event) {
			// admin shop has unlimited stock and we don't need to move items aound, so we can safely allow shift trading:
			return true;
		}
	}

	protected List<ItemStack[]> recipes;

	public AdminShopkeeper(ConfigurationSection config) {
		super(config);
		this.onConstruction();
	}

	/**
	 * Creates a new shopkeeper and spawns it in the world. This should be used when a player is
	 * creating a new shopkeeper.
	 * 
	 * @param location
	 *            the location to spawn at
	 * @param prof
	 *            the id of the profession
	 */
	public AdminShopkeeper(ShopCreationData creationData) {
		super(creationData);
		this.recipes = new ArrayList<ItemStack[]>();
		this.onConstruction();
	}

	private final void onConstruction() {
		this.registerUIHandler(new AdminShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new AdminShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		recipes = new ArrayList<ItemStack[]>();
		ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
		if (recipesSection != null) {
			for (String key : recipesSection.getKeys(false)) {
				ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
				ItemStack[] recipe = new ItemStack[3];
				for (int i = 0; i < 3; i++) {
					if (recipeSection.contains(i + "")) {
						recipe[i] = loadItemStack(recipeSection.getConfigurationSection(i + ""));
					}
				}
				recipes.add(recipe);
			}
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		ConfigurationSection recipesSection = config.createSection("recipes");
		int count = 0;
		for (ItemStack[] recipe : recipes) {
			ConfigurationSection recipeSection = recipesSection.createSection(count + "");
			for (int slot = 0; slot < 3; slot++) {
				if (recipe[slot] != null) {
					saveItemStack(recipe[slot], recipeSection.createSection(slot + ""));
				}
			}
			count++;
		}
	}

	@Override
	public ShopType<AdminShopkeeper> getType() {
		return DefaultShopTypes.ADMIN;
	}

	@Override
	public List<ItemStack[]> getRecipes() {
		return recipes;
	}

	private void setRecipes(List<ItemStack[]> recipes) {
		this.recipes = recipes;
	}

	/**
	 * Loads an ItemStack from a config section.
	 * 
	 * @param config
	 * @return
	 */
	private ItemStack loadItemStack(ConfigurationSection config) {
		ItemStack item = config.getItemStack("item");
		if (config.contains("attributes")) {
			String attributes = config.getString("attributes");
			if (attributes != null && !attributes.isEmpty()) {
				item = NMSManager.getProvider().loadItemAttributesFromString(item, attributes);
			}
		}
		return item;
	}

	/**
	 * Saves an ItemStack to a config section.
	 * 
	 * @param item
	 * @param config
	 */
	private void saveItemStack(ItemStack item, ConfigurationSection config) {
		config.set("item", item);
		String attr = NMSManager.getProvider().saveItemAttributesToString(item);
		if (attr != null && !attr.isEmpty()) {
			config.set("attributes", attr);
		}
	}
}