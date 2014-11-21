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
import com.nisovin.shopkeepers.ui.UIManager;
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

		protected AdminShopEditorHandler(UIManager uiManager, AdminShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
		}

		@Override
		protected boolean openWindow(Player player) {
			// get the shopkeeper's trade options
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);
			List<ItemStack[]> recipes = ((AdminShopkeeper) shopkeeper).getRecipes();
			for (int i = 0; i < recipes.size() && i < 8; i++) {
				ItemStack[] recipe = recipes.get(i);
				inventory.setItem(i, recipe[0]);
				inventory.setItem(i + 9, recipe[1]);
				inventory.setItem(i + 18, recipe[2]);
			}
			// add the special buttons
			this.setActionButtons(inventory);
			// show editing inventory
			player.openInventory(inventory);
			return true;
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
			for (int i = 0; i < 8; i++) {
				ItemStack cost1 = inventory.getItem(i);
				ItemStack cost2 = inventory.getItem(i + 9);
				ItemStack result = inventory.getItem(i + 18);
				if (cost1 != null && result != null) {
					// save trade recipe
					ItemStack[] recipe = new ItemStack[3];
					recipe[0] = cost1;
					recipe[1] = cost2;
					recipe[2] = result;
					recipes.add(recipe);
				} else if (player != null) {
					// return unused items to inventory
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

		protected AdminShopTradingHandler(UIManager uiManager, AdminShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
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
			for (int i = 0; i < 3; i++) {
				if (recipe[i] != null) {
					saveItemStack(recipe[i], recipeSection.createSection(i + ""));
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
			String attr = config.getString("attributes");
			if (attr != null && !attr.isEmpty()) {
				item = NMSManager.getProvider().loadItemAttributesFromString(item, attr);
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