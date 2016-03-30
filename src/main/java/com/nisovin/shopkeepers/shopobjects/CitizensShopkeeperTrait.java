package com.nisovin.shopkeepers.shopobjects;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
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
		return ShopkeepersPlugin.getInstance().getActiveShopkeeper(shopkeeperId);
	}

	@Override
	public void onRemove() {
		// this is also called when citizens reloads or disables..
		// we detect trait removal by listening to specific citizens events
	}

	void onShopkeeperRemove() {
		shopkeeperId = null;
		this.getNPC().removeTrait(CitizensShopkeeperTrait.class);
	}

	public void onTraitDeletion() {
		Shopkeeper shopkeeper = this.getShopkeeper();
		if (shopkeeper != null) {
			assert shopkeeper.getShopObject().getObjectType() == DefaultShopObjectTypes.CITIZEN();
			CitizensShop shopObject = (CitizensShop) shopkeeper.getShopObject();
			shopObject.onTraitRemoval();
			// this should keep the citizens npc and only remove the shopkeeper data:
			shopkeeper.delete();
			// save:
			ShopkeepersPlugin.getInstance().save();
		} else {
			// TODO what if the trait gets removed and Shopkeepers is disabled?
			// -> does a new npc get created when Shopkeepers enables again?
		}
	}

	private boolean isMissingShopkeeper() {
		NPC npc = this.getNPC();
		if (npc == null || !npc.hasTrait(CitizensShopkeeperTrait.class)) {
			// citizens not running or trait got already removed again?
			return false;
		}
		if (ShopkeepersPlugin.getInstance() == null) {
			// shopkeepers not running:
			return false;
		}

		if (ShopkeepersPlugin.getInstance().getActiveShopkeeper(CitizensShop.getId(npc.getId())) != null) {
			// there is already a shopkeeper for this npc:
			// the trait was probably re-attached after a reload of citizens:
			return false;
		}

		return true;
	}

	@Override
	public void onAttach() {
		// Log.debug("Shopkeeper trait attached to NPC " + npc.getId());

		// giving citizens some time to properly initialize the trait and npc:
		Bukkit.getScheduler().runTaskLater(ShopkeepersPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				// create a new shopkeeper if there isn't one already for this npc:
				if (!isMissingShopkeeper()) {
					return;
				}

				Log.debug("Creating shopkeeper for NPC " + npc.getId());

				NPC npc = getNPC();
				Location location = null;
				Entity entity = npc.getEntity();
				if (entity != null) {
					location = entity.getLocation();
				} else {
					location = npc.getStoredLocation();
				}

				if (location != null) {
					ShopCreationData creationData = new ShopCreationData(null, DefaultShopTypes.ADMIN(), DefaultShopObjectTypes.CITIZEN(), location, null);
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
		}, 5L);
	}
}