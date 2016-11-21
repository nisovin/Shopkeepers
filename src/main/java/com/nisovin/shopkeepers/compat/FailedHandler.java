package com.nisovin.shopkeepers.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;

@SuppressWarnings("rawtypes")
public final class FailedHandler implements NMSCallProvider {

	Class classWorld;

	Class classEntityHuman;
	Class classIMerchant;
	Class classEntityVillager;
	Constructor classEntityVillagerConstructor;
	Field recipeListField;

	Method setTradingPlayer;
	Method openTrade;

	Class classEntityInsentient;
	Method setCustomNameMethod;

	Class classMerchantRecipeList;

	Class craftInventory;
	Method craftInventoryGetInventory;
	Class classNMSItemStack;
	Field tagField;

	Class classCraftItemStack;
	Method asNMSCopyMethod;
	Method asBukkitCopyMethod;

	Class classMerchantRecipe;
	Constructor merchantRecipeConstructor;
	Field maxUsesField;
	Method getBuyItem1Method;
	Method getBuyItem2Method;
	Method getBuyItem3Method;

	Class classInventoryMerchant;
	Method getRecipeMethod;

	Class classCraftPlayer;
	Method craftPlayerGetHandle;

	Class classEntity;
	Field worldField;

	@SuppressWarnings("unchecked")
	public FailedHandler() throws Exception {
		String versionString = Bukkit.getServer().getClass().getName().replace("org.bukkit.craftbukkit.", "").replace("CraftServer", "");
		String nmsPackageString = "net.minecraft.server." + versionString;
		String obcPackageString = "org.bukkit.craftbukkit." + versionString;

		classWorld = Class.forName(nmsPackageString + "World");

		classEntityHuman = Class.forName(nmsPackageString + "EntityHuman");
		classIMerchant = Class.forName(nmsPackageString + "IMerchant");
		classEntityVillager = Class.forName(nmsPackageString + "EntityVillager");
		classEntityVillagerConstructor = classEntityVillager.getConstructor(classWorld);
		Field[] fields = classEntityVillager.getDeclaredFields();
		for (Field field : fields) {
			if (field.getType().getName().endsWith("MerchantRecipeList")) {
				recipeListField = field;
				break;
			}
		}
		recipeListField.setAccessible(true);

		setTradingPlayer = classEntityVillager.getDeclaredMethod("setTradingPlayer", classEntityHuman);
		setTradingPlayer.setAccessible(true);
		openTrade = classEntityHuman.getDeclaredMethod("openTrade", classIMerchant);
		openTrade.setAccessible(true);

		classEntityInsentient = Class.forName(nmsPackageString + "EntityInsentient");
		setCustomNameMethod = classEntityInsentient.getMethod("setCustomName", String.class);

		classNMSItemStack = Class.forName(nmsPackageString + "ItemStack");
		tagField = classNMSItemStack.getDeclaredField("tag");

		classCraftItemStack = Class.forName(obcPackageString + "inventory.CraftItemStack");
		asNMSCopyMethod = classCraftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);
		asBukkitCopyMethod = classCraftItemStack.getDeclaredMethod("asBukkitCopy", classNMSItemStack);

		classMerchantRecipe = Class.forName(nmsPackageString + "MerchantRecipe");
		merchantRecipeConstructor = classMerchantRecipe.getConstructor(classNMSItemStack, classNMSItemStack, classNMSItemStack);
		maxUsesField = classMerchantRecipe.getDeclaredField("maxUses");
		maxUsesField.setAccessible(true);
		getBuyItem1Method = classMerchantRecipe.getDeclaredMethod("getBuyItem1");
		getBuyItem2Method = classMerchantRecipe.getDeclaredMethod("getBuyItem2");
		getBuyItem3Method = classMerchantRecipe.getDeclaredMethod("getBuyItem3");

		craftInventory = Class.forName(obcPackageString + "inventory.CraftInventory");
		craftInventoryGetInventory = craftInventory.getDeclaredMethod("getInventory");
		classInventoryMerchant = Class.forName(nmsPackageString + "InventoryMerchant");
		getRecipeMethod = classInventoryMerchant.getDeclaredMethod("getRecipe");

		classMerchantRecipeList = Class.forName(nmsPackageString + "MerchantRecipeList");
		// clearMethod = classMerchantRecipeList.getMethod("clear");
		// addMethod = classMerchantRecipeList.getMethod("add", Object.class);

		classCraftPlayer = Class.forName(obcPackageString + "entity.CraftPlayer");
		craftPlayerGetHandle = classCraftPlayer.getDeclaredMethod("getHandle");

		classEntity = Class.forName(nmsPackageString + "Entity");
		worldField = classEntity.getDeclaredField("world");
	}

	@Override
	public String getVersionId() {
		return "FailedHandler";
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean openTradeWindow(String name, List<ItemStack[]> recipes, Player player) {
		ShopkeepersPlugin.getInstance().getLogger().warning(ChatColor.AQUA + "Shopkeepers needs an update.");
		try {

			Object villager = classEntityVillagerConstructor.newInstance(worldField.get(craftPlayerGetHandle.invoke(player)));
			if (name != null && !name.isEmpty()) {
				setCustomNameMethod.invoke(villager, name);
			}

			Object recipeList = recipeListField.get(villager);
			if (recipeList == null) {
				recipeList = classMerchantRecipeList.newInstance();
				recipeListField.set(villager, recipeList);
			}
			((ArrayList) recipeList).clear();
			for (ItemStack[] recipe : recipes) {
				Object mcRecipe = createMerchantRecipe(recipe[0], recipe[1], recipe[2]);
				if (mcRecipe != null) {
					((ArrayList) recipeList).add(mcRecipe);
				}
			}

			// set trading player:
			setTradingPlayer.invoke(villager, craftPlayerGetHandle.invoke(player));
			// open trade window:
			openTrade.invoke(craftPlayerGetHandle.invoke(player), villager);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean openTradeWindow(Shopkeeper shopkeeper, Player player) {
		return openTradeWindow(shopkeeper.getName(), shopkeeper.getRecipes(), player);
	}

	@Override
	public ItemStack[] getUsedTradingRecipe(Inventory merchantInventory) {
		try {
			Object inventoryMerchant = craftInventoryGetInventory.invoke(merchantInventory);
			Object merchantRecipe = getRecipeMethod.invoke(inventoryMerchant);
			ItemStack[] recipe = new ItemStack[3];
			recipe[0] = asBukkitCopy(getBuyItem1Method.invoke(merchantRecipe));
			recipe[1] = asBukkitCopy(getBuyItem2Method.invoke(merchantRecipe));
			recipe[2] = asBukkitCopy(getBuyItem3Method.invoke(merchantRecipe));
			return recipe;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Short.MAX_VALUE - 1, 15, true));
	}

	@Override
	public void setEntitySilent(Entity entity, boolean silent) {
	}

	@Override
	public void setNoAI(LivingEntity bukkitEntity) {
	}

	private Object createMerchantRecipe(ItemStack item1, ItemStack item2, ItemStack item3) {
		try {
			Object recipe = merchantRecipeConstructor.newInstance(convertItemStack(item1), convertItemStack(item2), convertItemStack(item3));
			maxUsesField.set(recipe, 10000);
			return recipe;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private Object convertItemStack(org.bukkit.inventory.ItemStack item) {
		if (item == null) return null;
		try {
			return asNMSCopyMethod.invoke(null, item);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private ItemStack asBukkitCopy(Object nmsItem) {
		if (nmsItem == null) return null;
		try {
			return (ItemStack) asBukkitCopyMethod.invoke(null, nmsItem);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ItemStack loadItemAttributesFromString(ItemStack item, String data) {
		return null;
	}

	@Override
	public String saveItemAttributesToString(ItemStack item) {
		return null;
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEvent event) {
		return true;
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEntityEvent event) {
		return true;
	}
}
