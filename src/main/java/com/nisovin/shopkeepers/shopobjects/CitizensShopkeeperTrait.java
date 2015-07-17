package com.nisovin.shopkeepers.shopobjects;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.util.DataKey;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shoptypes.DefaultShopTypes;

public class CitizensShopkeeperTrait extends Trait {

	public static final String TRAIT_NAME = "shopkeeper";

	public static void registerTrait() {
		CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(CitizensShopkeeperTrait.class).withName(TRAIT_NAME));
	}

	private String shopkeeperId = null;

	public CitizensShopkeeperTrait() {
		super("shopkeeper");
	}

	public void load(DataKey key) {
		this.shopkeeperId = key.getString("ShopkeeperId", null);
	}

	public void save(DataKey key) {
		key.setString("ShopkeeperId", shopkeeperId);
	}

	public Shopkeeper getShopkeeper() {
		if (shopkeeperId == null || ShopkeepersPlugin.getInstance() == null) {
			return null;
		}
		return ShopkeepersPlugin.getInstance().getActiveShopkeeperByObjectId(shopkeeperId);
	}

	@Override
	public void onRemove() {
		Shopkeeper shopkeeper = this.getShopkeeper();
		if (shopkeeper != null) {
			assert shopkeeper.getShopObject().getObjectType() == DefaultShopObjectTypes.CITIZEN;
			CitizensShop shopObject = (CitizensShop) shopkeeper.getShopObject();
			shopObject.onTraitRemoval();
			// this should keep the citizens npc and only remove the shopkeeper data:
			shopkeeper.delete();
		} else {
			// TODO what if the trait gets removed and Shopkeepers is disabled?
			// -> does a new npc get created when Shopkeepers enables again?

			// citizens currently seems to call this on shutdown as well,
			// Shopkeepers seems to get shutdown before that though
			// Log.warning("Shopkeeper trait removed while Shopkeepers plugin is disabled.");
		}
	}

	void onShopkeeperRemove() {
		shopkeeperId = null;
		this.getNPC().removeTrait(CitizensShopkeeperTrait.class);
	}

	@Override
	public void onAttach() {
		Log.debug("Shopkeeper trait attached to NPC " + npc.getId());
		// trait was attached after a reload:
		// TODO what if Shopkeepers plugin is disabled?
		if (this.getShopkeeper() != null) {
			return;
		}

		// trait was freshly created:
		NPC npc = this.getNPC();
		assert npc != null;

		Location location = null;
		Entity entity = npc.getEntity();
		if (entity != null) {
			location = entity.getLocation();
		} else {
			location = npc.getStoredLocation();
		}

		if (location != null) {
			ShopCreationData creationData = new ShopCreationData(null, DefaultShopTypes.ADMIN, DefaultShopObjectTypes.CITIZEN, location, null);
			creationData.npcId = npc.getId();
			Shopkeeper shopkeeper = ShopkeepersPlugin.getInstance().createNewAdminShopkeeper(creationData);
			if (shopkeeper != null) {
				shopkeeperId = shopkeeper.getObjectId();
			} else {
				Log.warning("Shopkeeper creation via trait failed. Removing trait again.");
				shopkeeperId = null;
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