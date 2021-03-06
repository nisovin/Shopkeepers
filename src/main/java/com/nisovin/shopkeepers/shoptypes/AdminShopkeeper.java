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

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
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
		public AdminShopkeeper getShopkeeper() {
			return (AdminShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean canOpen(Player player) {
			assert player != null;
			return super.canOpen(player) && this.getShopkeeper().getType().hasPermission(player);
		}

		@Override
		protected boolean openWindow(Player player) {
			final AdminShopkeeper shopkeeper = this.getShopkeeper();
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add the shopkeeper's trade offers:
			List<ItemStack[]> recipes = shopkeeper.getRecipes();
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
			final AdminShopkeeper shopkeeper = this.getShopkeeper();
			List<ItemStack[]> recipes = shopkeeper.recipes;
			recipes.clear();
			for (int column = 0; column < 8; column++) {
				ItemStack cost1 = Utils.getNullIfEmpty(inventory.getItem(column));
				ItemStack cost2 = Utils.getNullIfEmpty(inventory.getItem(column + 9));
				ItemStack result = Utils.getNullIfEmpty(inventory.getItem(column + 18));

				// handle cost2 item as cost1 item if there is no cost1 item:
				if (cost1 == null) {
					cost1 = cost2;
					cost2 = null;
				}

				if (cost1 != null && result != null) {
					// add trading recipe:
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
		public AdminShopkeeper getShopkeeper() {
			return (AdminShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean canOpen(Player player) {
			if (!super.canOpen(player)) return false;
			String tradePermission = this.getShopkeeper().getTradePremission();
			if (tradePermission != null && !Utils.hasPermission(player, tradePermission)) {
				Log.debug("Blocked trade window opening from " + player.getName() + ": missing custom trade permission");
				Utils.sendMessage(player, Settings.msgMissingCustomTradePerm);
				return false;
			}
			return true;
		}

		@Override
		protected boolean isShiftTradeAllowed(InventoryClickEvent event) {
			// admin shop has unlimited stock and we don't need to move items around, so we can safely allow shift
			// trading:
			return true;
		}
	}

	protected final List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
	// null indicates that no additional permission is required:
	protected String tradePermission = null;

	/**
	 * For use in extending classes.
	 */
	protected AdminShopkeeper() {
	}

	protected AdminShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.initOnLoad(config);
		this.onInitDone();
	}

	protected AdminShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
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
	protected void load(ConfigurationSection config) throws ShopkeeperCreateException {
		super.load(config);
		// load trade permission:
		tradePermission = config.getString("tradePerm", null);
		// load offers:
		recipes.clear();
		// legacy: load offers from old format
		recipes.addAll(this.loadRecipesOld(config, "recipes"));
		recipes.addAll(this.loadRecipes(config, "recipes"));
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		// save trade permission:
		config.set("tradePerm", tradePermission);
		// save offers:
		this.saveRecipes(config, "recipes", recipes);
	}

	@Override
	public ShopType<?> getType() {
		return DefaultShopTypes.ADMIN();
	}

	public String getTradePremission() {
		return tradePermission;
	}

	public void setTradePermission(String tradePermission) {
		if (tradePermission == null || tradePermission.isEmpty()) {
			tradePermission = null;
		}
		this.tradePermission = tradePermission;
	}

	@Override
	public List<ItemStack[]> getRecipes() {
		return recipes;
	}

	private void saveRecipes(ConfigurationSection config, String node, Collection<ItemStack[]> recipes) {
		ConfigurationSection recipesSection = config.createSection(node);
		int id = 0;
		for (ItemStack[] recipe : recipes) {
			// TODO temporary, due to a bukkit bug custom head item can currently not be saved
			if (Settings.skipCustomHeadSaving && (Utils.isCustomHeadItem(recipe[0])
					|| Utils.isCustomHeadItem(recipe[1])
					|| Utils.isCustomHeadItem(recipe[2]))) {
				Log.warning("Skipping saving of trade involving a head item with custom texture, which cannot be saved currently due to a bukkit bug.");
				continue;
			}
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
				recipe[0] = Utils.getNullIfEmpty(Utils.loadItem(recipeSection, "item1"));
				recipe[1] = Utils.getNullIfEmpty(Utils.loadItem(recipeSection, "item2"));
				recipe[2] = Utils.getNullIfEmpty(Utils.loadItem(recipeSection, "resultItem"));
				if (recipe[0] == null || recipe[2] == null) continue; // invalid recipe
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
						recipe[slot] = Utils.getNullIfEmpty(this.loadItemStackOld(recipeSection.getConfigurationSection(String.valueOf(slot))));
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
		if (item != null) {
			if (section.contains("attributes")) {
				String attributes = section.getString("attributes");
				if (attributes != null && !attributes.isEmpty()) {
					item = NMSManager.getProvider().loadItemAttributesFromString(item, attributes);
				}
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
