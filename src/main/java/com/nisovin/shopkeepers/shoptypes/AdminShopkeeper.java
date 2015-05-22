package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.Collection;
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
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;

/**
 * Represents a shopkeeper that is managed by an admin. This shopkeeper will have unlimited supply
 * and will not store earnings anywhere.
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
			List<ItemStack[]> recipes = ((AdminShopkeeper) shopkeeper).recipes;
			recipes.clear();
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

	protected final List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();

	/**
	 * For use in extending classes.
	 */
	protected AdminShopkeeper() {
	}

	public AdminShopkeeper(ConfigurationSection config) {
		this.initOnLoad(config);
		this.onInitDone();
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
		this.initOnCreation(creationData);
		this.onInitDone();
	}

	@Override
	protected void onInitDone() {
		super.onInitDone();
		this.registerUIHandler(new AdminShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new AdminShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		// load offers:
		recipes.clear();
		// legacy: load offers from old format
		recipes.addAll(this.loadRecipesOld(config, "recipes"));
		recipes.addAll(this.loadRecipes(config, "recipes"));
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		// save offers:
		this.saveRecipes(config, "recipes", recipes);
	}

	@Override
	public ShopType<AdminShopkeeper> getType() {
		return DefaultShopTypes.ADMIN;
	}

	@Override
	public List<ItemStack[]> getRecipes() {
		return recipes;
	}

	private void saveRecipes(ConfigurationSection config, String node, Collection<ItemStack[]> recipes) {
		ConfigurationSection recipesSection = config.createSection(node);
		int id = 0;
		for (ItemStack[] recipe : recipes) {
			ConfigurationSection recipeSection = recipesSection.createSection(String.valueOf(id));
			Utils.saveItem(recipeSection, "item1", recipe[0]);
			Utils.saveItem(recipeSection, "item2", recipe[1]);
			Utils.saveItem(recipeSection, "resultItem", recipe[2]);
			id++;
		}
	}

	private List<ItemStack[]> loadRecipes(ConfigurationSection config, String node) {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		ConfigurationSection recipesSection = config.getConfigurationSection(node);
		if (recipesSection != null) {
			for (String key : recipesSection.getKeys(false)) {
				ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
				ItemStack[] recipe = new ItemStack[3];
				recipe[0] = Utils.loadItem(recipeSection, "item1");
				recipe[1] = Utils.loadItem(recipeSection, "item2");
				recipe[2] = Utils.loadItem(recipeSection, "resultItem");
				recipes.add(recipe);
			}
		}
		return recipes;
	}

	// legacy code:

	/*private void saveRecipesOld(ConfigurationSection config, String node, Collection<ItemStack[]> recipes) {
		ConfigurationSection recipesSection = config.createSection(node);
		int count = 0;
		for (ItemStack[] recipe : recipes) {
			ConfigurationSection recipeSection = recipesSection.createSection(String.valueOf(count));
			for (int slot = 0; slot < 3; slot++) {
				if (recipe[slot] != null) {
					this.saveItemStackOld(recipe[slot], recipeSection.createSection(String.valueOf(slot)));
				}
			}
			count++;
		}
	}*/

	private List<ItemStack[]> loadRecipesOld(ConfigurationSection config, String node) {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		ConfigurationSection recipesSection = config.getConfigurationSection(node);
		if (recipesSection != null) {
			for (String key : recipesSection.getKeys(false)) {
				ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
				ItemStack[] recipe = new ItemStack[3];
				for (int slot = 0; slot < 3; slot++) {
					if (recipeSection.isConfigurationSection(String.valueOf(slot))) {
						recipe[slot] = this.loadItemStackOld(recipeSection.getConfigurationSection(String.valueOf(slot)));
					}
				}
				if (recipe[0] == null || recipe[2] == null) continue; // invalid recipe
				recipes.add(recipe);
			}
		}
		return recipes;
	}

	/**
	 * Loads an ItemStack from a config section.
	 * 
	 * @param section
	 * @return
	 */
	private ItemStack loadItemStackOld(ConfigurationSection section) {
		ItemStack item = section.getItemStack("item");
		if (section.contains("attributes")) {
			String attributes = section.getString("attributes");
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
	/*private void saveItemStackOld(ItemStack item, ConfigurationSection config) {
		config.set("item", item);
		String attr = NMSManager.getProvider().saveItemAttributesToString(item);
		if (attr != null && !attr.isEmpty()) {
			config.set("attributes", attr);
		}
	}*/
}