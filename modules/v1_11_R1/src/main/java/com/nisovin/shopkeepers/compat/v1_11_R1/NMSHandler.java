package com.nisovin.shopkeepers.compat.v1_11_R1;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.bukkit.craftbukkit.v1_11_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_11_R1.inventory.CraftInventoryMerchant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.v1_11_R1.*;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;

public final class NMSHandler implements NMSCallProvider {

	@Override
	public String getVersionId() {
		return "1_11_R1";
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean openTradeWindow(String title, List<ItemStack[]> recipes, Player player) {
		try {
			EntityVillager villager = new EntityVillager(((CraftPlayer) player).getHandle().world, 0);
			// custom name:
			if (title != null && !title.isEmpty()) {
				villager.setCustomName(title);
			}
			// career level (to prevent trade progression):
			Field careerLevelField = EntityVillager.class.getDeclaredField("bK");
			careerLevelField.setAccessible(true);
			careerLevelField.set(villager, 10);

			// recipes:
			Field recipeListField = EntityVillager.class.getDeclaredField("trades");
			recipeListField.setAccessible(true);
			MerchantRecipeList recipeList = (MerchantRecipeList) recipeListField.get(villager);
			if (recipeList == null) {
				recipeList = new MerchantRecipeList();
				recipeListField.set(villager, recipeList);
			}
			recipeList.clear();
			for (org.bukkit.inventory.ItemStack[] recipe : recipes) {
				recipeList.add(createMerchantRecipe(recipe[0], recipe[1], recipe[2]));
			}

			// set trading player:
			villager.setTradingPlayer(((CraftPlayer) player).getHandle());
			// open trade window:
			((CraftPlayer) player).getHandle().openTrade(villager);
			// trigger minecraft statistics:
			((CraftPlayer) player).getHandle().b(StatisticList.H);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public ItemStack[] getUsedTradingRecipe(Inventory merchantInventory) {
		try {
			InventoryMerchant handle = (InventoryMerchant) ((CraftInventoryMerchant) merchantInventory).getInventory();
			MerchantRecipe merchantRecipe = handle.getRecipe();
			ItemStack[] recipe = new ItemStack[3];
			recipe[0] = asBukkitCopy(merchantRecipe.getBuyItem1());
			recipe[1] = asBukkitCopy(merchantRecipe.getBuyItem2());
			recipe[2] = asBukkitCopy(merchantRecipe.getBuyItem3());
			return recipe;
		} catch (Exception e) {
			return null;
		}
	}

	private org.bukkit.inventory.ItemStack asBukkitCopy(net.minecraft.server.v1_11_R1.ItemStack nmsItem) {
		if (nmsItem == null || nmsItem.isEmpty()) return null;
		return CraftItemStack.asBukkitCopy(nmsItem);
	}

	private net.minecraft.server.v1_11_R1.ItemStack asNMSCopy(org.bukkit.inventory.ItemStack bukkitItem) {
		return org.bukkit.craftbukkit.v1_11_R1.inventory.CraftItemStack.asNMSCopy(bukkitItem);
	}

	private MerchantRecipe createMerchantRecipe(org.bukkit.inventory.ItemStack item1, org.bukkit.inventory.ItemStack item2, org.bukkit.inventory.ItemStack item3) {
		MerchantRecipe recipe = new MerchantRecipe(asNMSCopy(item1), asNMSCopy(item2), asNMSCopy(item3));
		try {
			// max uses:
			Field maxUsesField = MerchantRecipe.class.getDeclaredField("maxUses");
			maxUsesField.setAccessible(true);
			maxUsesField.set(recipe, 10000);

			// reward exp:
			Field rewardExpField = MerchantRecipe.class.getDeclaredField("rewardExp");
			rewardExpField.setAccessible(true);
			rewardExpField.set(recipe, false);
		} catch (Exception e) {
		}
		return recipe;
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			EntityLiving mcLivingEntity = ((CraftLivingEntity) entity).getHandle();
			// example: armor stands are living, but not insentient
			if (!(mcLivingEntity instanceof EntityInsentient)) return;

			// make goal selector items accessible:
			Field bField = PathfinderGoalSelector.class.getDeclaredField("b");
			bField.setAccessible(true);
			Field cField = PathfinderGoalSelector.class.getDeclaredField("c");
			cField.setAccessible(true);

			// overwrite goal selector:
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(mcLivingEntity);

			// clear old goals:
			Set<?> goals_b = (Set<?>) bField.get(goals);
			goals_b.clear();
			Set<?> goals_c = (Set<?>) cField.get(goals);
			goals_c.clear();

			// add new goals:
			goals.a(0, new PathfinderGoalFloat((EntityInsentient) mcLivingEntity));
			goals.a(1, new PathfinderGoalLookAtPlayer((EntityInsentient) mcLivingEntity, EntityHuman.class, 12.0F, 1.0F));

			// overwrite target selector:
			Field targetsField = EntityInsentient.class.getDeclaredField("targetSelector");
			targetsField.setAccessible(true);
			PathfinderGoalSelector targets = (PathfinderGoalSelector) targetsField.get(mcLivingEntity);

			// clear old goals:
			Set<?> targets_b = (Set<?>) bField.get(targets);
			targets_b.clear();
			Set<?> targets_c = (Set<?>) cField.get(targets);
			targets_c.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setEntitySilent(org.bukkit.entity.Entity entity, boolean silent) {
		entity.setSilent(silent);
	}

	@Override
	public void setNoAI(LivingEntity bukkitEntity) {
		bukkitEntity.setAI(false);
	}

	// TODO no longer needed once attribute saving and loading has been removed
	private NBTTagCompound getItemTag(net.minecraft.server.v1_11_R1.ItemStack itemStack) {
		if (itemStack == null) return null;
		try {
			Field tag = itemStack.getClass().getDeclaredField("tag");
			tag.setAccessible(true);
			return (NBTTagCompound) tag.get(itemStack);
		} catch (NoSuchFieldException e) {
			return null;
		} catch (IllegalAccessException e2) {
			return null;
		}
	}

	// TODO no longer needed once attribute saving and loading has been removed
	private void setItemTag(net.minecraft.server.v1_11_R1.ItemStack itemStack, NBTTagCompound newTag) {
		if (itemStack == null) return;
		try {
			Field tag = itemStack.getClass().getDeclaredField("tag");
			tag.setAccessible(true);
			tag.set(itemStack, newTag);
		} catch (NoSuchFieldException e) {
		} catch (IllegalAccessException e2) {
		}
	}

	@Override
	public org.bukkit.inventory.ItemStack loadItemAttributesFromString(org.bukkit.inventory.ItemStack item, String data) {
		// since somewhere in late bukkit 1.8, bukkit saves item attributes on its own (inside the internal data)
		// this is currently kept in, in case some old shopkeeper data gets imported, for which attributes weren't yet
		// serialized to the internal data by bukkit
		// TODO remove this in the future
		NBTTagList list = new NBTTagList();
		String[] attrs = data.split(";");
		for (String s : attrs) {
			if (!s.isEmpty()) {
				String[] attrData = s.split(",");
				NBTTagCompound attr = new NBTTagCompound();
				attr.setString("Name", attrData[0]);
				attr.setString("AttributeName", attrData[1]);
				attr.setDouble("Amount", Double.parseDouble(attrData[2]));
				attr.setInt("Operation", Integer.parseInt(attrData[3]));
				attr.setLong("UUIDLeast", Long.parseLong(attrData[4]));
				attr.setLong("UUIDMost", Long.parseLong(attrData[5]));
				// MC 1.9 addition: not needed, as Slot-serialization wasn't ever published
				/*if (attrData.length >= 7) {
					attr.setString("Slot", attrData[6]);
				}*/
				list.add(attr);
			}
		}
		net.minecraft.server.v1_11_R1.ItemStack i = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tag = this.getItemTag(i);
		if (tag == null) {
			tag = new NBTTagCompound();
			this.setItemTag(i, tag);
		}
		tag.set("AttributeModifiers", list);
		return CraftItemStack.asBukkitCopy(i);
	}

	@Override
	public String saveItemAttributesToString(org.bukkit.inventory.ItemStack item) {
		// since somewhere in late bukkit 1.8, bukkit saves item attributes on its own (inside the internal data)
		return null;
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEvent event) {
		return event.getHand() == EquipmentSlot.HAND;
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEntityEvent event) {
		return event.getHand() == EquipmentSlot.HAND;
	}
}
