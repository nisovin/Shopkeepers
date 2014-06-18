package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shoptypes.DefaultShopTypes;

import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

public class CitizensShopkeeperTrait extends Trait {

	public static void registerTrait() {
		net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(CitizensShopkeeperTrait.class).withName("shopkeeper"));
	}

	private String shopkeeperId = null;

	public CitizensShopkeeperTrait() {
		super("shopkeeper");
	}

	public void load(DataKey key) {
		this.shopkeeperId = key.getString("ShopkeeperId", null);
	}

	public void save(DataKey key) {
		key.setString("ShopkeeperId", this.shopkeeperId);
	}

	@Override
	public void onRemove() {
		if (this.shopkeeperId == null) return;
		ShopkeepersPlugin sk = ShopkeepersPlugin.getInstance();
		if (sk != null) {
			Shopkeeper shopkeeper = sk.getShopkeeperById(this.shopkeeperId);
			if (shopkeeper != null) {
				assert shopkeeper.getShopObject().getObjectType() == DefaultShopObjectTypes.CITIZEN;
				CitizensShop shopObject = (CitizensShop) shopkeeper.getShopObject();
				shopObject.onTraitRemoval();
				// this should keep the citizens npc and only remove the shopkeeper data:
				shopkeeper.delete();
			}
			this.shopkeeperId = null;
		} else {
			// TODO what if the trait gets removed and Shopkeepers is disabled?
			// -> does a new npc get created when Shopkeepers enables again?
			
			// citizens currently seem to call this on shutdown as well, Shopkeepers seems to get shutdown before that though
			//Log.warning("Shopkeeper trait removed while Shopkeepers plugin id disabled.");
		}
	}

	void onShopkeeperRemove() {
		this.shopkeeperId = null;
		this.getNPC().removeTrait(CitizensShopkeeperTrait.class);
	}

	@Override
	public void onAttach() {
		assert this.getNPC() != null;
		Location location = null;
		LivingEntity entity = this.getNPC().getBukkitEntity();
		if (entity != null) location = entity.getLocation();
		else this.getNPC().getStoredLocation();
		if (location != null) {
			ShopCreationData creationData = new ShopCreationData(null, DefaultShopTypes.ADMIN, location, DefaultShopObjectTypes.CITIZEN);
			creationData.npcId = this.getNPC().getId();
			Shopkeeper shopkeeper = ShopkeepersPlugin.getInstance().createNewAdminShopkeeper(creationData);
			if (shopkeeper != null) {
				this.shopkeeperId = shopkeeper.getId();
			} else {
				Log.warning("Shopkeeper creation via trait failed. Removing trait again.");
				this.shopkeeperId = null;
				Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), new Runnable() {

					@Override
					public void run() {
						getNPC().removeTrait(CitizensShopkeeperTrait.class);
					}
				});
			}
		} else {
			// well.. no idea what to do in that case.. we cannot create a shopkeeper without a location, right?
			Log.debug("Shopkeeper NPC Trait: Failed to create shopkeeper due to missing npc location.");
		}
	}
}