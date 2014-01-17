package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

public class BlockShop extends ShopObject {
	
	@Override
	public void load(ConfigurationSection config) {
	}

	@Override
	public void save(ConfigurationSection config) {
		config.set("object", "block");
	}

	@Override
	public boolean needsSpawned() {
		return false;
	}

	@Override
	public boolean spawn(String world, int x, int y, int z) {
		return true;
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public String getId() {
		return "block" + shopkeeper.getWorldName() + "," + shopkeeper.getX() + "," + shopkeeper.getY() + "," + shopkeeper.getZ();
	}
	
	@Override
	public Location getActualLocation() {
		World w = Bukkit.getWorld(shopkeeper.getWorldName());
		if (w == null) {
			return null;
		} else {
			return new Location(w, shopkeeper.getX(), shopkeeper.getY(), shopkeeper.getZ());
		}
	}
	
	@Override
	public void setName(String name) {
		Location loc = getActualLocation();
		if (loc != null) {
			Block block = loc.getBlock();
			Material type = block.getType();
			if (type == Material.WALL_SIGN || type == Material.SIGN_POST) {
				Sign sign = (Sign)block.getState();
				sign.setLine(0, ChatColor.translateAlternateColorCodes('&', Settings.signShopFirstLine));
				if (name != null) {
					name = ChatColor.translateAlternateColorCodes('&', name);
					if (name.length() > 15) name = name.substring(0, 15);
					sign.setLine(1, name);
				} else {
					sign.setLine(1, "");
				}
				if (shopkeeper instanceof PlayerShopkeeper) {
					sign.setLine(2, ((PlayerShopkeeper)shopkeeper).getOwner());
				}
				sign.update();
			}
		}
	}
	
	@Override
	public void setItem(ItemStack item) {
		
	}

	@Override
	public boolean check(String world, int x, int y, int z) {
		return false;
	}

	@Override
	public void despawn() {
	}
	
	@Override
	public void delete() {
		World w = Bukkit.getWorld(shopkeeper.getWorldName());
		if (w != null) {
			w.getBlockAt(shopkeeper.getX(), shopkeeper.getY(), shopkeeper.getZ()).setType(Material.AIR);
		}
	}

	@Override
	public ItemStack getTypeItem() {
		return null;
	}

	@Override
	public ShopObjectType getObjectType() {
		return ShopObjectType.SIGN;
	}

	@Override
	public void cycleType() {
	}

}
