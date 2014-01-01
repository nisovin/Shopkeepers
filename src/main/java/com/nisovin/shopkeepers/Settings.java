package com.nisovin.shopkeepers;

import java.lang.reflect.Field;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Settings {
	
	public static boolean disableOtherVillagers = true;
	public static boolean hireOtherVillagers = true;
	public static boolean blockVillagerSpawns = false;
	public static boolean enableSpawnVerifier = false;
	public static boolean bypassSpawnBlocking = true;
	public static boolean enablePurchaseLogging = false;
	public static boolean saveInstantly = true;
	
	public static boolean enableWorldGuardRestrictions = false;
	public static boolean enableTownyRestrictions = false;	
	
	public static boolean requireChestRecentlyPlaced = true;
	public static boolean createPlayerShopWithCommand = false;
	public static boolean simulateRightClickOnCommand = true;
	public static boolean protectChests = true;
	public static boolean deleteShopkeeperOnBreakChest = false;
	public static int maxShopsPerPlayer = 0;
	public static String maxShopsPermOptions = "10,15,25";
	public static int maxChestDistance = 15;
	public static int playerShopkeeperInactiveDays = 0;
	public static boolean preventTradingWithOwnShop = true;
	
	public static int taxRate = 0;
	public static boolean taxRoundUp = false;
	
	public static Material shopCreationItem = Material.MONSTER_EGG;
	public static int shopCreationItemData = 120;
	public static String shopCreationItemName = "";
	public static boolean preventShopCreationItemRegularUsage = false;
	public static boolean deletingPlayerShopReturnsEgg = false;

	public static boolean enableVillagerShops = true;	
	public static boolean enableSignShops = true;
	public static boolean enableWitchShops = true;
	public static boolean enableCreeperShops = false;
	
	public static String signShopFirstLine = "[SHOP]";
	public static boolean showNameplates = true;
	public static boolean alwaysShowNameplates = false;
	public static String nameplatePrefix = "&a";
	public static String nameRegex = "[A-Za-z0-9 ]{3,32}";
	
	//public static boolean enableBlockShops = true;
	//public static int blockShopType = 0;

	public static String editorTitle = "Shopkeeper Editor";
	public static Material nameItem = Material.ANVIL;
	public static Material deleteItem = Material.FIRE;
	
	public static Material hireItem = Material.EMERALD;
	public static String hireItemText = "&aHire This Shopkeeper";
	public static String forHireTitle = "For Hire";
	
	public static Material currencyItem = Material.EMERALD;
	public static short currencyItemData = 0;
	//public static String currencyItemName = "";
	public static Material zeroItem = Material.SLIME_BALL;
	
	public static Material highCurrencyItem = Material.EMERALD_BLOCK;
	public static short highCurrencyItemData = 0;
	//public static String highCurrencyItemName = "";
	public static int highCurrencyValue = 9;
	public static int highCurrencyMinCost = 20;
	public static Material highZeroItem = Material.SLIME_BALL;
	
	public static String msgButtonName = "&aSet Shop Name";
	public static String msgButtonType = "&aChoose Appearance";
	public static String msgButtonDelete = "&4Delete";
	public static String msgButtonHire = "&aHire";
	
	public static String msgSelectedNormalShop = "&aNormal shopkeeper selected (sells items to players).";
	public static String msgSelectedBookShop = "&aBook shopkeeper selected (sell books).";
	public static String msgSelectedBuyShop = "&aBuying shopkeeper selected (buys items from players).";
	public static String msgSelectedTradeShop = "&aTrading shopkeeper selected (trade items with players).";
	
	public static String msgSelectedVillagerShop = "&aVillager shop selected.";
	public static String msgSelectedSignShop = "&aSign shop selected.";
	public static String msgSelectedWitchShop = "&aWitch shop selected.";
	public static String msgSelectedCreeperShop = "&aCreeper shop selected.";
	
	public static String msgSelectedChest = "&aChest selected! Right click a block to place your shopkeeper.";
	public static String msgMustSelectChest = "&aYou must right-click a chest before placing your shopkeeper.";
	public static String msgChestTooFar = "&aThe shopkeeper's chest is too far away!";
	public static String msgChestNotPlaced = "&aYou must select a chest you have recently placed.";
	public static String msgTypeNewName = "&aPlease type the shop's name into the chat.\n  &aType a dash (-) to remove the name.";
	public static String msgNameSet = "&aThe shop's name has been set!";
	public static String msgNameInvalid = "&aThat name is not valid!";
	
	public static String msgHired = "&aYou have hired this shopkeeper!";
	public static String msgCantHire = "&aYou cannot afford to hire this shopkeeper.";
	public static String msgForHire = "&aThe villager offered his services as a shopkeeper in exchange for " + hireItem.toString() + ".";
	
	public static String msgPlayerShopCreated = "&aShopkeeper created!\n&aAdd items you want to sell to your chest, then\n&aright-click the shop while sneaking to modify costs.";
	public static String msgBookShopCreated = "&aShopkeeper created!\n&aAdd written books and blank books to your chest, then\n&aright-click the shop while sneaking to modify costs.";
	public static String msgBuyShopCreated = "&aShopkeeper created!\n&aAdd one of each item you want to sell to your chest, then\n&aright-click the shop while sneaking to modify costs.";
	public static String msgTradeShopCreated = "&aShopkeeper created!\n&aAdd items you want to sell to your chest, then\n&aright-click the shop while sneaking to modify costs.";
	public static String msgAdminShopCreated = "&aShopkeeper created!\n&aRight-click the shop while sneaking to modify trades.";
	public static String msgShopCreateFail = "&aYou cannot create a shopkeeper there.";
	public static String msgTooManyShops = "&aYou have too many shops.";

	public static String fileEncoding = "";

	// returns true, if the config misses values which need to be saved
	public static boolean loadConfiguration(Configuration config) {
		boolean misses = false;
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				String configKey = field.getName().replaceAll("([A-Z][a-z]+)", "-$1").toLowerCase();
				// initialize the setting with the default value, if it is missing in the config
				if (!config.isSet(configKey)) {
					if (field.getType() == Material.class) {
						config.set(configKey, ((Material)field.get(null)).name());
					} else {
						config.set(configKey, field.get(null));
					}
					misses = true;
				}
				if (field.getType() == String.class) {
					field.set(null, config.getString(configKey, (String)field.get(null)));
				} else if (field.getType() == int.class) {
					field.set(null, config.getInt(configKey, field.getInt(null)));
				} else if (field.getType() == short.class) {
					field.set(null, (short)config.getInt(configKey, field.getShort(null)));
				} else if (field.getType() == boolean.class) {
					field.set(null, config.getBoolean(configKey, field.getBoolean(null)));
				} else if (field.getType() == Material.class) {
					if (config.contains(configKey)) {
						if (config.isInt(configKey)) {
							@SuppressWarnings("deprecation")
							Material mat = Material.getMaterial(config.getInt(configKey));
							if (mat != null) {
								field.set(null, mat);
							}
						} else if (config.isString(configKey)) {
							Material mat = Material.getMaterial(config.getString(configKey));
							if (mat != null) {
								field.set(null, mat);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (maxChestDistance > 50) maxChestDistance = 50;
		if (highCurrencyValue <= 0) highCurrencyItem = Material.AIR;

		return misses;
	}
	
	public static void loadLanguageConfiguration(Configuration config) {
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				if (field.getType() == String.class && field.getName().startsWith("msg")) {
					String configKey = field.getName().replaceAll("([A-Z][a-z]+)", "-$1").toLowerCase();
					field.set(null, config.getString(configKey, (String)field.get(null)));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static ItemStack createCreationItem() {
		ItemStack creationItem = new ItemStack(shopCreationItem, 1, (short)shopCreationItemData);
		if (shopCreationItemName != null && !shopCreationItemName.isEmpty()) {
			ItemMeta meta = creationItem.getItemMeta();
			meta.setDisplayName(shopCreationItemName);
			creationItem.setItemMeta(meta);
		}
		return creationItem;
	}
	
}
