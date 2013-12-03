package com.nisovin.shopkeepers.volatilecode;

import java.io.DataInput;
import java.io.DataOutput;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.shopkeepers.Shopkeeper;

@SuppressWarnings("rawtypes")
public class VolatileCode_Unknown implements VolatileCodeHandle {

	Class classWorld;
	
	Class classEntityVillager;
	Constructor classEntityVillagerConstructor;
	Field recipeListField;
	Method openTradeMethod;
	
	Class classEntityInsentient;
	Method setCustomNameMethod;
	
	Class classMerchantRecipeList;
	//Method clearMethod;
	//Method addMethod;
	
	Class classNMSItemStack;
	Field tagField;
	
	Class classCraftItemStack;
	Method asNMSCopyMethod;
	Method asBukkitCopyMethod;
	
	Class classMerchantRecipe;
	Constructor merchantRecipeConstructor;
	Field maxUsesField;
	
	Class classCraftPlayer;
	Method craftPlayerGetHandle;
	
	Class classEntity;
	Field worldField;
	
	Class classNbtBase;
	Class classNbtTagCompound;
	Method compoundWriteMethod;
	Method compoundLoadMethod;
	Method compoundHasKeyMethod;
	Method compoundGetCompoundMethod;
	Method compoundSetMethod;
	
	@SuppressWarnings("unchecked")
	public VolatileCode_Unknown() throws Exception {
		String versionString = Bukkit.getServer().getClass().getName().replace("org.bukkit.craftbukkit.", "").replace("CraftServer", "");
		String nmsPackageString = "net.minecraft.server." + versionString;
		String obcPackageString = "org.bukkit.craftbukkit." + versionString;

		classWorld = Class.forName(nmsPackageString + "World");
		
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
		Method[] methods = classEntityVillager.getDeclaredMethods();
		for (Method method : methods) {
			if (method.getReturnType() == boolean.class && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].getName().endsWith("EntityHuman")) {
				openTradeMethod = method;
				break;
			}
		}
		openTradeMethod.setAccessible(true);
		
		classEntityInsentient = Class.forName(nmsPackageString + "EntityInsentient");
		setCustomNameMethod = classEntityInsentient.getDeclaredMethod("setCustomName", String.class);
		
		classNMSItemStack = Class.forName(nmsPackageString + "ItemStack");
		tagField = classNMSItemStack.getDeclaredField("tag");
		
		classCraftItemStack = Class.forName(obcPackageString + "inventory.CraftItemStack");
		asNMSCopyMethod = classCraftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);
		asBukkitCopyMethod = classCraftItemStack.getDeclaredMethod("asBukkitCopy", classNMSItemStack);
		
		classMerchantRecipe = Class.forName(nmsPackageString + "MerchantRecipe");
		merchantRecipeConstructor = classMerchantRecipe.getConstructor(classNMSItemStack, classNMSItemStack, classNMSItemStack);
		maxUsesField = classMerchantRecipe.getDeclaredField("maxUses");
		maxUsesField.setAccessible(true);
		
		classMerchantRecipeList = Class.forName(nmsPackageString + "MerchantRecipeList");
		//clearMethod = classMerchantRecipeList.getMethod("clear");
		//addMethod = classMerchantRecipeList.getMethod("add", Object.class);
		
		classCraftPlayer = Class.forName(obcPackageString + "entity.CraftPlayer");
		craftPlayerGetHandle = classCraftPlayer.getDeclaredMethod("getHandle");
		
		classEntity = Class.forName(nmsPackageString + "Entity");
		worldField = classEntity.getDeclaredField("world");

		classNbtBase = Class.forName(nmsPackageString + "NBTBase");
		classNbtTagCompound = Class.forName(nmsPackageString + "NBTTagCompound");
		compoundWriteMethod = classNbtTagCompound.getDeclaredMethod("write", DataOutput.class);
		compoundLoadMethod = classNbtTagCompound.getDeclaredMethod("load", DataInput.class, int.class);
		compoundHasKeyMethod = classNbtTagCompound.getDeclaredMethod("hasKey", String.class);
		compoundGetCompoundMethod = classNbtTagCompound.getDeclaredMethod("getCompound", String.class);
		compoundSetMethod = classNbtTagCompound.getDeclaredMethod("set", String.class, classNbtBase);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean openTradeWindow(String name, List<ItemStack[]> recipes, Player player) {
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
			((ArrayList)recipeList).clear();
			for (ItemStack[] recipe : recipes) {
				Object r = createMerchantRecipe(recipe[0], recipe[1], recipe[2]);
				if (r != null) {
					((ArrayList)recipeList).add(r);
				}
			}
			
			openTradeMethod.invoke(villager, craftPlayerGetHandle.invoke(player));
			
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
	public void overwriteLivingEntityAI(LivingEntity entity) {
		/*try {
			EntityLiving ev = ((CraftLivingEntity)entity).getHandle();
			
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
			
			Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
			listField.setAccessible(true);
			List list = (List)listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			list = (List)listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat((EntityInsentient) ev));
			goals.a(1, new PathfinderGoalLookAtPlayer((EntityInsentient) ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Short.MAX_VALUE-1, 15, true));
	}
	
	@Override
	public void overwriteVillagerAI(LivingEntity villager) {
		/*try {
			EntityVillager ev = ((CraftVillager)villager).getHandle();
			
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
			
			Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
			listField.setAccessible(true);
			List list = (List)listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			list = (List)listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
			goals.a(1, new PathfinderGoalTradeWithPlayer(ev));
			goals.a(1, new PathfinderGoalLookAtTradingPlayer(ev));
			goals.a(2, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		overwriteLivingEntityAI(villager);
	}

	@Override
	public void setVillagerProfession(Villager villager, int profession) {
		try {
			@SuppressWarnings("deprecation")
			Profession prof = Profession.getProfession(profession);
			if (prof != null) {
				villager.setProfession(prof);
			} else {
				villager.setProfession(Profession.FARMER);
			}
		} catch (Exception e) {
		}
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

	@Override
	public ItemStack loadItemAttributesFromString(ItemStack item, String data) {
		/*try {
			Object attributesCompound = classNbtTagCompound.newInstance();
			ByteArrayInputStream bytes = new ByteArrayInputStream(data.getBytes(Settings.fileEncoding));
			DataInput in = new DataInputStream(bytes);
			compoundLoadMethod.invoke(attributesCompound, in, 0);
			Object nmsItem = asNMSCopyMethod.invoke(null, item);
			Object tag = tagField.get(nmsItem);
			compoundSetMethod.invoke(tag, "AttributeModifiers", attributesCompound);
			return (ItemStack)asBukkitCopyMethod.invoke(null, nmsItem);
		} catch (Exception e) {
			e.printStackTrace();
			return item;
		}*/
		return item;
	}

	@Override
	public String saveItemAttributesToString(ItemStack item) {
		/*try {
			Object nmsItem = asNMSCopyMethod.invoke(null, item);
			Object tag = tagField.get(nmsItem);
			if ((Boolean)compoundHasKeyMethod.invoke(tag, "AttributeModifiers")) {
				Object attributesCompound = compoundGetCompoundMethod.invoke(tag, "AttributeModifiers");
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				DataOutput out = new DataOutputStream(bytes);
				compoundWriteMethod.invoke(attributesCompound, out);
				return bytes.toString(Settings.fileEncoding);
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}*/
		return null;
	}

	
}
