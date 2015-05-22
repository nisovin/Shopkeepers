package com.nisovin.shopkeepers.compat.v1_8_R2;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftInventoryMerchant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.v1_8_R2.*;

import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;

public final class NMSHandler implements NMSCallProvider {

	@Override
	public String getVersionId() {
		return "1_8_R2";
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean openTradeWindow(String name, List<org.bukkit.inventory.ItemStack[]> recipes, Player player) {
		try {
			EntityVillager villager = new EntityVillager(((CraftPlayer) player).getHandle().world, 0);
			// custom name:
			if (name != null && !name.isEmpty()) {
				villager.setCustomName(name);
			}
			// career level (to prevent trade progression):
			Field careerLevelField = EntityVillager.class.getDeclaredField("by");
			careerLevelField.setAccessible(true);
			careerLevelField.set(villager, 10);

			// recipes:
			Field recipeListField = EntityVillager.class.getDeclaredField("br");
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

			// this will trigger the "create child" code of minecraft when the player is holding a spawn egg in his hands,
			// but bypasses craftbukkits interact events and therefore removes the spawn egg from the players hands
			// result: we have to prevent openTradeWindow if the shopkeeper entity is being clicking with a spawn egg in hands
			// villager.a(((CraftPlayer) player).getHandle());
			villager.a_(((CraftPlayer) player).getHandle()); // set trading player
			((CraftPlayer) player).getHandle().openTrade(villager); // open trade window
			((CraftPlayer) player).getHandle().b(StatisticList.F); // minecraft statistics

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
	public int getCurrentRecipePage(Inventory merchantInventory) {
		try {
			InventoryMerchant handle = (InventoryMerchant) ((CraftInventoryMerchant) merchantInventory).getInventory();
			Field field = InventoryMerchant.class.getDeclaredField("e");
			field.setAccessible(true);
			return field.getInt(handle);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			EntityLiving ev = ((CraftLivingEntity) entity).getHandle();

			// overwrite goal selector:
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);

			Field listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			List<?> list = (List<?>) listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("c");
			listField.setAccessible(true);
			list = (List<?>) listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat((EntityInsentient) ev));
			goals.a(1, new PathfinderGoalLookAtPlayer((EntityInsentient) ev, EntityHuman.class, 12.0F, 1.0F));

			// overwrite target selector:
			Field targetsField = EntityInsentient.class.getDeclaredField("targetSelector");
			targetsField.setAccessible(true);
			PathfinderGoalSelector targets = (PathfinderGoalSelector) targetsField.get(ev);

			Field listField2 = PathfinderGoalSelector.class.getDeclaredField("b");
			listField2.setAccessible(true);
			List<?> list2 = (List<?>) listField.get(goals);
			list2.clear();
			listField2 = PathfinderGoalSelector.class.getDeclaredField("c");
			listField2.setAccessible(true);
			list2 = (List<?>) listField.get(goals);
			list2.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void overwriteVillagerAI(LivingEntity villager) {
		try {
			EntityVillager ev = ((CraftVillager) villager).getHandle();

			// overwrite goal selector:
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);

			Field listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			List<?> list = (List<?>) listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("c");
			listField.setAccessible(true);
			list = (List<?>) listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
			goals.a(1, new PathfinderGoalTradeWithPlayer(ev));
			goals.a(1, new PathfinderGoalLookAtTradingPlayer(ev));
			goals.a(2, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 12.0F, 1.0F));

			// overwrite target selector:
			Field targetsField = EntityInsentient.class.getDeclaredField("targetSelector");
			targetsField.setAccessible(true);
			PathfinderGoalSelector targets = (PathfinderGoalSelector) targetsField.get(ev);

			Field listField2 = PathfinderGoalSelector.class.getDeclaredField("b");
			listField2.setAccessible(true);
			List<?> list2 = (List<?>) listField.get(goals);
			list2.clear();
			listField2 = PathfinderGoalSelector.class.getDeclaredField("c");
			listField2.setAccessible(true);
			list2 = (List<?>) listField.get(goals);
			list2.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getMaxVillagerProfession() {
		return 4;
	}

	@Override
	public void setVillagerProfession(Villager villager, int profession) {
		((CraftVillager) villager).getHandle().setProfession(profession);
	}

	@Override
	public void setEntitySilent(org.bukkit.entity.Entity entity, boolean silent) {
		Entity mcEntity = ((CraftEntity) entity).getHandle();
		mcEntity.b(silent);
	}

	@Override
	public void setNoAI(LivingEntity bukkitEntity) {
		net.minecraft.server.v1_8_R2.Entity nmsEntity = ((CraftEntity) bukkitEntity).getHandle();
		NBTTagCompound tag = nmsEntity.getNBTTag();
		if (tag == null) {
			tag = new NBTTagCompound();
		}
		nmsEntity.c(tag);
		tag.setInt("NoAI", 1);
		nmsEntity.f(tag);
	}

	private MerchantRecipe createMerchantRecipe(org.bukkit.inventory.ItemStack item1, org.bukkit.inventory.ItemStack item2, org.bukkit.inventory.ItemStack item3) {
		MerchantRecipe recipe = new MerchantRecipe(convertItemStack(item1), convertItemStack(item2), convertItemStack(item3));
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

	private net.minecraft.server.v1_8_R2.ItemStack convertItemStack(org.bukkit.inventory.ItemStack item) {
		if (item == null) return null;
		return org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack.asNMSCopy(item);
	}

	private NBTTagCompound getItemTag(net.minecraft.server.v1_8_R2.ItemStack itemStack) {
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

	private void setItemTag(net.minecraft.server.v1_8_R2.ItemStack itemStack, NBTTagCompound newTag) {
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
				list.add(attr);
			}
		}
		net.minecraft.server.v1_8_R2.ItemStack i = CraftItemStack.asNMSCopy(item);
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
		net.minecraft.server.v1_8_R2.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		if (nmsItem == null) return null;
		NBTTagCompound tag = this.getItemTag(nmsItem);
		if (tag == null || !tag.hasKey("AttributeModifiers")) {
			return null;
		}
		String data = "";
		NBTTagList list = tag.getList("AttributeModifiers", 10);
		for (int i = 0; i < list.size(); i++) {
			NBTTagCompound attr = list.get(i);
			data += attr.getString("Name") + ","
					+ attr.getString("AttributeName") + ","
					+ attr.getDouble("Amount") + ","
					+ attr.getInt("Operation") + ","
					+ attr.getLong("UUIDLeast") + ","
					+ attr.getLong("UUIDMost") + ";";
		}
		return data;
	}

	/*@Override
	public boolean areAttributesSimilar(ItemStack item1, ItemStack item2) {
		assert item1 != null && item2 != null;
		net.minecraft.server.v1_8_R2.ItemStack nmsItem1 = CraftItemStack.asNMSCopy(item1);
		net.minecraft.server.v1_8_R2.ItemStack nmsItem2 = CraftItemStack.asNMSCopy(item2);
		NBTTagCompound tag1 = this.getItemTag(nmsItem1);
		NBTTagCompound tag2 = this.getItemTag(nmsItem2);

		boolean item1NoAttributes = (tag1 == null || !tag1.hasKey("AttributeModifiers"));
		boolean item2NoAttributes = (tag2 == null || !tag2.hasKey("AttributeModifiers"));
		if (item1NoAttributes || item2NoAttributes) {
			return (item1NoAttributes == item2NoAttributes);
		}

		NBTTagList list1 = tag1.getList("AttributeModifiers", 10);
		NBTTagList list2 = tag2.getList("AttributeModifiers", 10);
		if (list1.size() != list2.size()) return false;
		for (int i = 0; i < list1.size(); i++) {
			NBTTagCompound attr1 = (NBTTagCompound) list1.get(i);
			NBTTagCompound attr2 = (NBTTagCompound) list2.get(i);
			String name1 = attr1.getString("Name");
			String name2 = attr2.getString("Name");
			if (!name1.equals(name2)) return false;
			String attributeName1 = attr1.getString("AttributeName");
			String attributeName2 = attr2.getString("AttributeName");
			if (!attributeName1.equals(attributeName2)) return false;
			double amount1 = attr1.getDouble("Amount");
			double amount2 = attr2.getDouble("Amount");
			if (amount1 != amount2) return false;
			int operation1 = attr1.getInt("Operation");
			int operation2 = attr2.getInt("Operation");
			if (operation1 != operation2) return false;

			// ignore uuid part
		}

		return true;
	}

	@Override
	public String areSimilarReasoned(ItemStack item1, ItemStack item2) {
		return null; // considered similar
	}

	@Override
	public String areSimilarReasoned(ItemMeta itemMeta1, ItemMeta itemMeta2) {
		if (!itemMeta1.getItemFlags().equals(itemMeta2.getItemFlags())) {
			return "differing item hide flags";
		}

		if (itemMeta1 instanceof BannerMeta) {
			// banner:
			assert itemMeta2 instanceof BannerMeta;
			BannerMeta banner1 = (BannerMeta) itemMeta1;
			BannerMeta banner2 = (BannerMeta) itemMeta2;

			// base color:
			if (!banner1.getBaseColor().equals(banner2.getBaseColor())) {
				return "differing base colors";
			}

			// patterns:
			if (banner1.numberOfPatterns() != banner2.numberOfPatterns()) {
				return "differing banner patterns (differing pattern counts)";
			}
			if (!banner1.getPatterns().equals(banner2.getPatterns())) {
				return "differing banner patterns";
			}
		} else if (itemMeta1 instanceof BlockStateMeta) {
			// block state:
			assert itemMeta2 instanceof BlockStateMeta;
			BlockStateMeta blockStateMeta1 = (BlockStateMeta) itemMeta1;
			BlockStateMeta blockStateMeta2 = (BlockStateMeta) itemMeta2;

			if (blockStateMeta1.hasBlockState() != blockStateMeta2.hasBlockState()) {
				return "differing block states (one has no block state)";
			}
			if (blockStateMeta1.hasBlockState()) {
				assert blockStateMeta2.hasBlockState();
				if (!blockStateMeta1.getBlockState().equals(blockStateMeta2.getBlockState())) {
					return "differing block state";
				}
			}
		}
		return null; // considered similar
	}*/

	@Override
	public boolean supportsPlayerUUIDs() {
		return true;
	}

	@Override
	public UUID getUUID(OfflinePlayer player) {
		return player.getUniqueId();
	}

	@Override
	public OfflinePlayer getOfflinePlayer(UUID uuid) {
		return Bukkit.getOfflinePlayer(uuid);
	}
}