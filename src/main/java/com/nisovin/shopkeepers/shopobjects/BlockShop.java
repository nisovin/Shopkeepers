package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

public class BlockShop extends ShopObject {

	protected BlockShop(Shopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
	}

	@Override
	public boolean needsSpawning() {
		return false;
	}

	@Override
	public boolean spawn() {
		return true;
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public String getId() {
		return "block" + this.shopkeeper.getWorldName() + "," + this.shopkeeper.getX() + "," + this.shopkeeper.getY() + "," + this.shopkeeper.getZ();
	}

	@Override
	public Location getActualLocation() {
		World w = Bukkit.getWorld(this.shopkeeper.getWorldName());
		if (w == null) {
			return null;
		} else {
			return new Location(w, this.shopkeeper.getX(), this.shopkeeper.getY(), this.shopkeeper.getZ());
		}
	}

	@Override
	protected void setName(String name) {
		Location loc = getActualLocation();
		if (loc != null) {
			Block block = loc.getBlock();
			Material type = block.getType();
			if (type == Material.WALL_SIGN || type == Material.SIGN_POST) {
				Sign sign = (Sign) block.getState();
				sign.setLine(0, ChatColor.translateAlternateColorCodes('&', Settings.signShopFirstLine));
				if (name != null) {
					name = ChatColor.translateAlternateColorCodes('&', name);
					name = this.trimToNameLength(name);
					sign.setLine(1, name);
				} else {
					sign.setLine(1, "");
				}
				if (this.shopkeeper instanceof PlayerShopkeeper) {
					sign.setLine(2, ((PlayerShopkeeper) this.shopkeeper).getOwnerName());
				}
				sign.update();
			}
		}
	}

	@Override
	protected int getNameLengthLimit() {
		return 15;
	}

	@Override
	public void setItem(ItemStack item) {

	}

	@Override
	public boolean check() {
		return false;
	}

	@Override
	public void despawn() {
	}

	@Override
	public void delete() {
		World world = Bukkit.getWorld(this.shopkeeper.getWorldName());
		if (world != null) {
			// this should load the chunk if necessary, making sure that the block gets removed (though, might not work on server stops..):
			world.getBlockAt(this.shopkeeper.getX(), this.shopkeeper.getY(), this.shopkeeper.getZ()).setType(Material.AIR);
			// TODO trigger an unloadChunkRequest if the chunk had to be loaded? (for now let's assume that the server handles that kind of thing automatically)
		} else {
			// well: world unloaded and we didn't get an event.. not our fault
		}
	}

	@Override
	public ItemStack getSubTypeItem() {
		return null;
	}

	@Override
	public ShopObjectType getObjectType() {
		return DefaultShopObjectTypes.SIGN;
	}

	@Override
	public void cycleSubType() {
	}
}