package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

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

	private String shopkeeperId;

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
		if (this.shopkeeperId != null) {
			ShopkeepersPlugin sk = ShopkeepersPlugin.getInstance();
			if (sk != null) {
				Shopkeeper shopkeeper = sk.getShopkeeperById(this.shopkeeperId);
				if (shopkeeper != null) {
					shopkeeper.delete(); //TODO this will remove the npc when the trait is removed. Is this correct? Traits can only be added by admins, no?
				}
				this.shopkeeperId = null;
			} else {
				// TODO what if the trait gets removed and Shopkeepers is disabled?
				// -> does the npc get respawned when Shopkeepers enables again?
			}
		}
	}

	@Override
	public void onAttach() {
		LivingEntity entity = this.getNPC().getBukkitEntity();
		if (entity != null) {
			Location location = entity.getLocation();
			Shopkeeper shopkeeper = ShopkeepersPlugin.getInstance().createNewAdminShopkeeper(new ShopCreationData(null, DefaultShopTypes.ADMIN, location, DefaultShopObjectTypes.CITIZEN));
			if (shopkeeper != null) {
				this.shopkeeperId = shopkeeper.getId();
				((CitizensShop) shopkeeper.getShopObject()).setNPCId(this.getNPC().getId());
			} else {
				this.shopkeeperId = null;
			}
			
		}
	}
}