package com.nisovin.shopkeepers;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public class Settings {

	public static String fileEncoding = "UTF-8";
	public static boolean debug = false;

	public static boolean disableOtherVillagers = true;
	public static boolean hireOtherVillagers = false;
	public static boolean blockVillagerSpawns = false;
	public static boolean enableSpawnVerifier = false;
	public static boolean bypassSpawnBlocking = true;
	public static boolean bypassShopInteractionBlocking = false;
	public static boolean enablePurchaseLogging = false;
	public static boolean saveInstantly = true;
	public static boolean skipCustomHeadSaving = true;

	public static boolean enableWorldGuardRestrictions = false;
	public static boolean requireWorldGuardAllowShopFlag = false;
	public static boolean enableTownyRestrictions = false;

	public static boolean createPlayerShopWithCommand = false;
	public static boolean simulateRightClickOnCommand = true;
	public static boolean requireChestRecentlyPlaced = true;
	public static boolean protectChests = true; // TODO does it make sense to not protected shop chests?
	public static boolean deleteShopkeeperOnBreakChest = false;
	public static int maxShopsPerPlayer = 0;
	public static String maxShopsPermOptions = "10,15,25";
	public static int maxChestDistance = 15;
	public static int playerShopkeeperInactiveDays = 0;
	public static boolean preventTradingWithOwnShop = true;
	public static boolean preventTradingWhileOwnerIsOnline = false;
	public static boolean useStrictItemComparison = false;
	public static boolean enableChestOptionOnPlayerShop = false;

	public static int taxRate = 0;
	public static boolean taxRoundUp = false;

	public static Material shopCreationItem = Material.MONSTER_EGG;
	public static int shopCreationItemData = 0;
	public static String shopCreationItemName = "";
	public static List<String> shopCreationItemLore = new ArrayList<String>(0);
	public static boolean preventShopCreationItemRegularUsage = false;
	public static boolean deletingPlayerShopReturnsCreationItem = false;

	public static List<String> enabledLivingShops = Arrays.asList(
			EntityType.VILLAGER.name(), EntityType.COW.name(),
			EntityType.ENDERMAN.name(), EntityType.IRON_GOLEM.name(),
			EntityType.MUSHROOM_COW.name(), EntityType.OCELOT.name(),
			EntityType.PIG.name(), EntityType.PIG_ZOMBIE.name(),
			EntityType.SHEEP.name(), EntityType.SKELETON.name(),
			EntityType.SNOWMAN.name(), EntityType.WITCH.name(),
			EntityType.WOLF.name(), EntityType.ZOMBIE.name(),
			// added in MC 1.8:
			EntityType.RABBIT.name(),
			// added in MC 1.10:
			"POLAR_BEAR"
			);

	public static boolean silenceLivingShopEntities = true;

	public static boolean enableSignShops = true;
	public static boolean enableCitizenShops = false;

	public static String signShopFirstLine = "[SHOP]";
	public static boolean showNameplates = true;
	public static boolean alwaysShowNameplates = false;
	public static String nameplatePrefix = "&a";
	public static String nameRegex = "[A-Za-z0-9 ]{3,32}";
	public static boolean namingOfPlayerShopsViaItem = false;
	public static boolean allowRenamingOfPlayerNpcShops = false;

	public static String editorTitle = "Shopkeeper Editor";
	public static Material nameItem = Material.NAME_TAG;
	public static int nameItemData = 0;
	public static String nameItemName = "";
	public static List<String> nameItemLore = new ArrayList<String>(0);
	public static Material chestItem = Material.CHEST;
	public static int chestItemData = 0;
	public static Material deleteItem = Material.BONE;
	public static int deleteItemData = 0;

	public static Material hireItem = Material.EMERALD;
	public static int hireItemData = 0;
	public static String hireItemName = "";
	public static List<String> hireItemLore = new ArrayList<String>(0);
	public static int hireOtherVillagersCosts = 1;
	public static String forHireTitle = "For Hire";

	public static Material currencyItem = Material.EMERALD;
	public static short currencyItemData = 0;
	public static String currencyItemName = "";
	public static List<String> currencyItemLore = new ArrayList<String>(0);
	public static Material zeroCurrencyItem = Material.BARRIER;
	public static short zeroCurrencyItemData = 0;
	public static String zeroCurrencyItemName = "";
	public static List<String> zeroCurrencyItemLore = new ArrayList<String>(0);

	public static Material highCurrencyItem = Material.EMERALD_BLOCK;
	public static short highCurrencyItemData = 0;
	public static String highCurrencyItemName = "";
	public static List<String> highCurrencyItemLore = new ArrayList<String>(0);
	public static int highCurrencyValue = 9;
	public static int highCurrencyMinCost = 20;
	public static Material highZeroCurrencyItem = Material.BARRIER;
	public static short highZeroCurrencyItemData = 0;
	public static String highZeroCurrencyItemName = "";
	public static List<String> highZeroCurrencyItemLore = new ArrayList<String>(0);

	public static String language = "en";

	public static String msgCreationItemSelected = "&aRight-click to select the shop type.\n"
			+ "&aSneak + right-click to select the object type.\n"
			+ "&aRight-click a chest to select it.\n"
			+ "&aThen right-click a block to place the shopkeeper.";

	public static String msgButtonName = "&aSet Shop Name";
	public static List<String> msgButtonNameLore = Arrays.asList("Lets you rename", "your shopkeeper");
	public static String msgButtonChest = "&aView Chest Inventory";
	public static List<String> msgButtonChestLore = Arrays.asList("Lets you view the inventory", " your shopkeeper is using");
	public static String msgButtonType = "&aChoose Appearance";
	public static List<String> msgButtonTypeLore = Arrays.asList("Changes the look", "of your shopkeeper");
	public static String msgButtonDelete = "&4Delete";
	public static List<String> msgButtonDeleteLore = Arrays.asList("Closes and removes", "this shopkeeper");
	public static String msgButtonHire = "&aHire";
	public static List<String> msgButtonHireLore = Arrays.asList("Buy this shop");

	public static String msgSelectedNormalShop = "&aNormal shopkeeper selected (sells items to players).";
	public static String msgSelectedBookShop = "&aBook shopkeeper selected (sell books).";
	public static String msgSelectedBuyShop = "&aBuying shopkeeper selected (buys items from players).";
	public static String msgSelectedTradeShop = "&aTrading shopkeeper selected (trade items with players).";

	public static String msgSelectedLivingShop = "&aYou selected: &f{type}";
	public static String msgSelectedSignShop = "&aYou selected: &fsign shop";
	public static String msgSelectedCitizenShop = "&aYou selected: &fcitizen npc shop";

	public static String msgSelectedChest = "&aChest selected! Right click a block to place your shopkeeper.";
	public static String msgMustSelectChest = "&aYou must right-click a chest before placing your shopkeeper.";
	public static String msgChestTooFar = "&aThe shopkeeper's chest is too far away!";
	public static String msgChestNotPlaced = "&aYou must select a chest you have recently placed.";
	public static String msgTooManyShops = "&aYou have too many shops.";
	public static String msgShopCreateFail = "&aYou cannot create a shopkeeper there.";
	public static String msgTypeNewName = "&aPlease type the shop's name into the chat.\n"
			+ "  &aType a dash (-) to remove the name.";
	public static String msgNameSet = "&aThe shop's name has been set!";
	public static String msgNameInvalid = "&aThat name is not valid!";
	public static String msgUnknownShopkeeper = "&7No shopkeeper found with that name or id.";
	public static String msgUnknownPlayer = "&7No player found with that name.";
	public static String msgUnknowShopType = "&7Unknown shop type '{type}'.";
	public static String msgShopTypeDisabled = "&7The shop type '{type}' is disabled.";
	public static String msgUnknowShopObjectType = "&7Unknown shop object type '{type}'.";
	public static String msgShopObjectTypeDisabled = "&7The shop object type '{type}' is disabled.";
	public static String msgMustTargetChest = "&7You have to target a chest.";
	public static String msgUnusedChest = "&7No shopkeeper is using this chest.";
	public static String msgNotOwner = "&7You are not the owner of this shopkeeper.";
	// placeholders: {owner} -> new owners name
	public static String msgOwnerSet = "&aNew owner was set to &e{owner}";

	public static String msgTradePermSet = "&aThe shop's trading permission has been set!";
	public static String msgTradePermRemoved = "&aThe shop's trading permission has been removed!";
	public static String msgTradePermView = "&aThe shop's current trading permission is '&e{perm}&a'.";

	public static String msgMustHoldHireItem = "&7You have to hold the required hire item in your hand.";
	public static String msgSetForHire = "&aThe Shopkeeper was set for hire.";
	public static String msgHired = "&aYou have hired this shopkeeper!";
	public static String msgCantHire = "&aYou cannot afford to hire this shopkeeper.";
	// placeholders: {costs}, {hire-item}
	public static String msgVillagerForHire = "&aThe villager offered his services as a shopkeeper in exchange for &6{costs}x {hire-item}&a.";

	public static String msgMissingTradePerm = "&7You do not have the permission to trade with this shop.";
	public static String msgMissingCustomTradePerm = "&7You do not have the permission to trade with this shop.";
	public static String msgCantTradeWhileOwnerOnline = "&7You cannot trade while the owner of this shop ('{owner}') is online.";

	public static String msgPlayerShopCreated = "&aShopkeeper created!\n"
			+ "&aAdd items you want to sell to your chest, then\n"
			+ "&aright-click the shop while sneaking to modify costs.";
	public static String msgBookShopCreated = "&aShopkeeper created!\n"
			+ "&aAdd written books and blank books to your chest, then\n"
			+ "&aright-click the shop while sneaking to modify costs.";
	public static String msgBuyShopCreated = "&aShopkeeper created!\n"
			+ "&aAdd one of each item you want to buy to your chest, then\n"
			+ "&aright-click the shop while sneaking to modify costs.";
	public static String msgTradeShopCreated = "&aShopkeeper created!\n"
			+ "&aAdd items you want to sell to your chest, then\n"
			+ "&aright-click the shop while sneaking to modify costs.";
	public static String msgAdminShopCreated = "&aShopkeeper created!\n"
			+ "&aRight-click the shop while sneaking to modify trades.";

	public static String msgListAdminShopsHeader = "&9There are &e{shopsCount} &9admin shops: &e(Page {page})";
	public static String msgListPlayerShopsHeader = "&9Player '&e{player}&9' has &e{shopsCount} &9shops: &e(Page {page})";
	public static String msgListShopsEntry = "  &e{shopIndex}) &8{shopName}&r&7at &8({location})&7, type: &8{shopType}&7, object type: &8{objectType}";

	public static String msgRemovedAdminShops = "&e{shopsCount} &aadmin shops were removed.";
	public static String msgRemovedPlayerShops = "&e{shopsCount} &ashops of player '&e{player}&a' were removed.";
	public static String msgRemovedAllPlayerShops = "&aAll &e{shopsCount} &aplayer shops were removed.";

	public static String msgConfirmRemoveAdminShops = "&cYou are about to irrevocable remove all admin shops!\n"
			+ "&7Please confirm this action by typing &6/shopkeepers confirm";
	public static String msgConfirmRemoveOwnShops = "&cYou are about to irrevocable remove all your shops!\n"
			+ "&7Please confirm this action by typing &6/shopkeepers confirm";
	public static String msgConfirmRemovePlayerShops = "&cYou are about to irrevocable remove all shops of player &6{player}&c!\n"
			+ "&7Please confirm this action by typing &6/shopkeepers confirm";
	public static String msgConfirmRemoveAllPlayerShops = "&cYou are about to irrevocable remove all player shops of all players!\n"
			+ "&7Please confirm this action by typing &6/shopkeepers confirm";

	public static String msgConfirmationExpired = "&cConfirmation expired.";
	public static String msgNothingToConfirm = "&cThere is nothing to confirm currently.";

	public static String msgNoPermission = "&cYou don't have the permission to do that.";

	public static String msgHelpHeader = "&9***** &8[&6Shopkeepers Help&8] &9*****";
	public static String msgCommandHelp = "&a/shopkeepers help &8- &7Shows this help page.";
	public static String msgCommandReload = "&a/shopkeepers reload &8- &7Reloads this plugin.";
	public static String msgCommandDebug = "&a/shopkeepers debug &8- &7Toggles debug mode on and off.";
	public static String msgCommandList = "&a/shopkeepers list [player|admin] [page] &8- &7Lists all shops for the specified player, or all admin shops.";
	public static String msgCommandRemove = "&a/shopkeepers remove [player|all|admin] &8- &7Removes all shops for the specified player, all players, or all admin shops.";
	public static String msgCommandRemote = "&a/shopkeepers remote <shopName> &8- &7Remotely opens a shop.";
	public static String msgCommandTransfer = "&a/shopkeepers transfer <newOwner> &8- &7Transfers the ownership of a shop.";
	public static String msgCommandSettradeperm = "&a/shopkeepers setTradePerm <shopId> <tradePerm|-|?> &8- &7Sets, removes (-) or displays (?) the trading permission.";
	public static String msgCommandSetforhire = "&a/shopkeepers setForHire &8- &7Sets one of your shops for sale.";
	public static String msgCommandShopkeeper = "&a/shopkeepers [shop type] [object type] &8- &7Creates a shop.";

	// returns true, if the config misses values which need to be saved
	public static boolean loadConfiguration(Configuration config) {
		boolean misses = false;
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				Class<?> typeClass = field.getType();
				String configKey = field.getName().replaceAll("([A-Z][a-z]+)", "-$1").toLowerCase();

				// initialize the setting with the default value, if it is missing in the config
				if (!config.isSet(configKey)) {
					if (typeClass == Material.class) {
						config.set(configKey, ((Material) field.get(null)).name());
					} else if (typeClass == String.class) {
						config.set(configKey, Utils.decolorize((String) field.get(null)));
					} else if (typeClass == List.class && (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] == String.class) {
						config.set(configKey, Utils.decolorize((List<String>) field.get(null)));
					} else {
						config.set(configKey, field.get(null));
					}
					misses = true;
				}

				if (typeClass == String.class) {
					field.set(null, Utils.colorize(config.getString(configKey, (String) field.get(null))));
				} else if (typeClass == int.class) {
					field.set(null, config.getInt(configKey, field.getInt(null)));
				} else if (typeClass == short.class) {
					field.set(null, (short) config.getInt(configKey, field.getShort(null)));
				} else if (typeClass == boolean.class) {
					field.set(null, config.getBoolean(configKey, field.getBoolean(null)));
				} else if (typeClass == Material.class) {
					if (config.contains(configKey)) {
						if (config.isInt(configKey)) {
							@SuppressWarnings("deprecation")
							Material mat = Material.getMaterial(config.getInt(configKey));
							if (mat != null) {
								field.set(null, mat);
							}
						} else if (config.isString(configKey)) {
							Material mat = Material.matchMaterial(config.getString(configKey));
							if (mat != null) {
								field.set(null, mat);
							}
						}
					}
				} else if (typeClass == List.class) {
					Class<?> genericType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
					if (genericType == String.class) {
						field.set(null, Utils.colorize(config.getStringList(configKey)));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// validation:

		if (maxChestDistance > 50) maxChestDistance = 50;
		if (highCurrencyValue <= 0) highCurrencyItem = Material.AIR;
		// certain items cannot be of type AIR:
		if (shopCreationItem == Material.AIR) shopCreationItem = Material.MONSTER_EGG;
		if (hireItem == Material.AIR) hireItem = Material.EMERALD;
		if (currencyItem == Material.AIR) currencyItem = Material.EMERALD;

		return misses;
	}

	public static void loadLanguageConfiguration(Configuration config) {
		try {
			Field[] fields = Settings.class.getDeclaredFields();
			for (Field field : fields) {
				if (field.getType() == String.class && field.getName().startsWith("msg")) {
					String configKey = field.getName().replaceAll("([A-Z][a-z]+)", "-$1").toLowerCase();
					field.set(null, Utils.colorize(config.getString(configKey, (String) field.get(null))));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// item utilities:

	// creation item:
	public static ItemStack createCreationItem() {
		return Utils.createItemStack(shopCreationItem, 1, (short) shopCreationItemData, shopCreationItemName, shopCreationItemLore);
	}

	public static boolean isCreationItem(ItemStack item) {
		return Utils.isSimilar(item, Settings.shopCreationItem, (short) Settings.shopCreationItemData, Settings.shopCreationItemName, Settings.shopCreationItemLore);
	}

	// naming item:
	public static ItemStack createNameButtonItem() {
		return Utils.createItemStack(nameItem, 1, (short) nameItemData, msgButtonName, msgButtonNameLore);
	}

	public static boolean isNamingItem(ItemStack item) {
		return Utils.isSimilar(item, nameItem, (short) nameItemData, Settings.nameItemName, Settings.nameItemLore);
	}

	// chest button:
	public static ItemStack createChestButtonItem() {
		return Utils.createItemStack(chestItem, 1, (short) chestItemData, msgButtonChest, msgButtonChestLore);
	}

	// delete button:
	public static ItemStack createDeleteButtonItem() {
		return Utils.createItemStack(deleteItem, 1, (short) deleteItemData, msgButtonDelete, msgButtonDeleteLore);
	}

	// hire item:
	public static ItemStack createHireButtonItem() {
		return Utils.createItemStack(hireItem, 1, (short) hireItemData, msgButtonHire, msgButtonHireLore);
	}

	public static boolean isHireItem(ItemStack item) {
		return Utils.isSimilar(item, hireItem, (short) hireItemData, hireItemName, hireItemLore);
	}

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
		if (Settings.highCurrencyItem == Material.AIR) return null;
		return Utils.createItemStack(Settings.highCurrencyItem, amount, Settings.highCurrencyItemData,
				Settings.highCurrencyItemName, Settings.highCurrencyItemLore);
	}

	public static boolean isHighCurrencyItem(ItemStack item) {
		if (Settings.highCurrencyItem == Material.AIR) {
			return item == null || item.getType() == Material.AIR;
		}
		return Utils.isSimilar(item, Settings.highCurrencyItem, Settings.highCurrencyItemData,
				Settings.highCurrencyItemName, Settings.highCurrencyItemLore);
	}

	// zero currency item:
	public static ItemStack createZeroCurrencyItem() {
		if (Settings.zeroCurrencyItem == Material.AIR) return null;
		return Utils.createItemStack(Settings.zeroCurrencyItem, 1, Settings.zeroCurrencyItemData,
				Settings.zeroCurrencyItemName, Settings.zeroCurrencyItemLore);
	}

	public static boolean isZeroCurrencyItem(ItemStack item) {
		if (Settings.zeroCurrencyItem == Material.AIR) {
			return item == null || item.getType() == Material.AIR;
		}
		return Utils.isSimilar(item, Settings.zeroCurrencyItem, Settings.zeroCurrencyItemData,
				Settings.zeroCurrencyItemName, Settings.zeroCurrencyItemLore);
	}

	// high zero currency item:
	public static ItemStack createHighZeroCurrencyItem() {
		if (Settings.highZeroCurrencyItem == Material.AIR) return null;
		return Utils.createItemStack(Settings.highZeroCurrencyItem, 1, Settings.highZeroCurrencyItemData,
				Settings.highZeroCurrencyItemName, Settings.highZeroCurrencyItemLore);
	}

	public static boolean isHighZeroCurrencyItem(ItemStack item) {
		if (Settings.highZeroCurrencyItem == Material.AIR) {
			return item == null || item.getType() == Material.AIR;
		}
		return Utils.isSimilar(item, Settings.highZeroCurrencyItem, Settings.highZeroCurrencyItemData,
				Settings.highZeroCurrencyItemName, Settings.highZeroCurrencyItemLore);
	}
}
