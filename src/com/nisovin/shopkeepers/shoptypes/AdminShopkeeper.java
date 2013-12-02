package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeeperType;
import com.nisovin.shopkeepers.shopobjects.ShopObject;

/**
 * Represents a shopkeeper that is managed by an admin. This shopkeeper will have unlimited supply
 * and will not save earnings anywhere.
 *
 */
public class AdminShopkeeper extends Shopkeeper {

	protected List<ItemStack[]> recipes;
	
	public AdminShopkeeper(ConfigurationSection config) {
		super(config);
	}
	
	/**
	 * Creates a new shopkeeper and spawns it in the world. This should be used when a player is
	 * creating a new shopkeeper.
	 * @param location the location to spawn at
	 * @param prof the id of the profession
	 */
	public AdminShopkeeper(Location location, ShopObject shopObject) {
		super(location, shopObject);
		recipes = new ArrayList<ItemStack[]>();
	}
	
	@Override
	public void load(ConfigurationSection config) {
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
	public void save(ConfigurationSection config) {
		super.save(config);
		config.set("type", "admin");
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
	public ShopkeeperType getType() {
		return ShopkeeperType.ADMIN;
	}
	
	@Override
	public boolean onEdit(Player player) {
		if (player.hasPermission("shopkeeper.admin")) {
			// get the shopkeeper's trade options
			Inventory inv = Bukkit.createInventory(player, 27, Settings.editorTitle);
			List<ItemStack[]> recipes = getRecipes();
			for (int i = 0; i < recipes.size() && i < 8; i++) {
				ItemStack[] recipe = recipes.get(i);
				inv.setItem(i, recipe[0]);
				inv.setItem(i + 9, recipe[1]);
				inv.setItem(i + 18, recipe[2]);
			}
			// add the special buttons
			setActionButtons(inv);
			// show editing inventory
			player.openInventory(inv);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onEditorClose(InventoryCloseEvent event) {
		Inventory inv = event.getInventory();
		saveEditor(inv, (Player)event.getPlayer());
	}
	
	@Override
	protected void saveEditor(Inventory inv, Player player) {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		for (int i = 0; i < 8; i++) {
			ItemStack cost1 = inv.getItem(i);
			ItemStack cost2 = inv.getItem(i + 9);
			ItemStack result = inv.getItem(i + 18);
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
		setRecipes(recipes);
	}
	
	@Override
	public void onPurchaseClick(InventoryClickEvent event) {
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
	 * @param config
	 * @return
	 */
	private ItemStack loadItemStack(ConfigurationSection config) {
		if (config.contains("item")) {
			return config.getItemStack("item");
		}
		ItemStack item = new ItemStack(config.getInt("id"), config.getInt("amt"), (short)config.getInt("data"));
		if (config.contains("name") || config.contains("lore") || config.contains("color")) {
			ItemMeta meta = item.getItemMeta();
			if (config.contains("name")) {
				meta.setDisplayName(config.getString("name"));
			}
			if (config.contains("lore")) {
				List<String> lore = config.getStringList("lore");
				meta.setLore(lore);
			}
			if (config.contains("color") && meta instanceof LeatherArmorMeta) {
				((LeatherArmorMeta)meta).setColor(Color.fromRGB(config.getInt("color")));
			}
			item.setItemMeta(meta);
		}
		if (config.contains("enchants")) {
			List<String> list = config.getStringList("enchants");
			for (String s : list) {
				String[] enchantData = s.split(" ");
				item.addUnsafeEnchantment(Enchantment.getById(Integer.parseInt(enchantData[0])), Integer.parseInt(enchantData[1]));
			}
		}
		if (item.getType() == Material.WRITTEN_BOOK && config.contains("title") && config.contains("author") && config.contains("pages")) {
			BookMeta meta = (BookMeta)item.getItemMeta();
			meta.setTitle(config.getString("title"));
			meta.setAuthor(config.getString("author"));
			meta.setPages(config.getStringList("pages"));
			item.setItemMeta(meta);
		}
		return item;
	}
	
	/**
	 * Saves an ItemStack to a config section.
	 * @param item
	 * @param config
	 */
	private void saveItemStack(ItemStack item, ConfigurationSection config) {
		config.set("item", item);
		/*config.set("id", item.getTypeId());
		config.set("data", item.getDurability());
		config.set("amt", item.getAmount());
		
		ItemMeta meta = item.getItemMeta();
		// basic meta
		if (meta.hasDisplayName()) {
			config.set("name", meta.getDisplayName());
		}
		if (meta.hasLore()) {
			config.set("lore", meta.getLore());
		}
		if (meta instanceof LeatherArmorMeta) {
			config.set("color", ((LeatherArmorMeta)meta).getColor().asRGB());
		}
		// book meta
		if (meta instanceof BookMeta) {
			BookMeta book = (BookMeta)meta;
			if (book.hasTitle()) {
				config.set("title", book.getTitle());
			}
			if (book.hasAuthor()) {
				config.set("author", book.getAuthor());
			}
			if (book.hasPages()) {
				config.set("pages", book.getPages());
			}
		}
		// enchants
		Map<Enchantment, Integer> enchants = item.getEnchantments();
		if (enchants.size() > 0) {
			List<String> list = new ArrayList<String>();
			for (Enchantment enchant : enchants.keySet()) {
				list.add(enchant.getId() + " " + enchants.get(enchant));
			}
			config.set("enchants", list);
		}*/
	}
	
}
