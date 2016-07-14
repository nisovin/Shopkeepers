package com.nisovin.shopkeepers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

public class ProtectedChests {

	public static final BlockFace[] CHEST_PROTECTED_FACES = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
	public static final BlockFace[] HOPPER_PROTECTED_FACES = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };

	private final Map<String, List<PlayerShopkeeper>> protectedChests = new HashMap<String, List<PlayerShopkeeper>>();

	public ProtectedChests() {
	}

	void onEnable(ShopkeepersPlugin plugin) {
	}

	void onDisable(ShopkeepersPlugin plugin) {
		// cleanup:
		protectedChests.clear();
	}

	private String getKey(String worldName, int x, int y, int z) {
		return worldName + ";" + x + ";" + y + ";" + z;
	}

	public void addChest(String worldName, int x, int y, int z, PlayerShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper);
		String key = this.getKey(worldName, x, y, z);
		List<PlayerShopkeeper> shopkeepers = protectedChests.get(key);
		if (shopkeepers == null) {
			shopkeepers = new ArrayList<PlayerShopkeeper>(1);
			protectedChests.put(key, shopkeepers);
		}
		shopkeepers.add(shopkeeper);
	}

	public void removeChest(String worldName, int x, int y, int z, PlayerShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper);
		String key = this.getKey(worldName, x, y, z);
		List<PlayerShopkeeper> shopkeepers = protectedChests.get(key);
		if (shopkeepers == null) return;
		shopkeepers.remove(shopkeeper);
		if (shopkeepers.isEmpty()) {
			protectedChests.remove(key);
		}
	}

	//

	// checks if this exact block is protected
	private boolean isThisChestProtected(String worldName, int x, int y, int z, Player player) {
		String key = this.getKey(worldName, x, y, z);
		List<PlayerShopkeeper> shopkeepers = protectedChests.get(key);
		if (shopkeepers == null) return false;
		for (PlayerShopkeeper shopkeeper : shopkeepers) {
			if (player == null || !shopkeeper.isOwner(player)) {
				return true;
			}
		}
		return false;
	}

	//

	// checks if this block or any directly adjacent blocks are protected:
	private boolean isChestDirectlyProtected(String worldName, int x, int y, int z, Player player) {
		// checking if this exact block is protected:
		if (this.isThisChestProtected(worldName, x, y, z, player)) return true;
		// the adjacent blocks are always protected as well:
		for (BlockFace face : CHEST_PROTECTED_FACES) {
			if (this.isThisChestProtected(worldName, x + face.getModX(), y + face.getModY(), z + face.getModZ(), player)) {
				return true;
			}
		}
		return false;
	}

	private boolean isChestDirectlyProtected(Block block, Player player) {
		assert block != null;
		return this.isChestDirectlyProtected(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), player);
	}

	//

	// checks if this chest block is protected, either because itself or a neighboring chest is protected:
	public boolean isChestProtected(Block chest, Player player) {
		Validate.notNull(chest);
		// checking if this block is directly protected:
		if (this.isChestDirectlyProtected(chest, player)) return true;

		// further adjacent blocks are only protected if the an adjacent block to that is directly protected chest:
		for (BlockFace face : CHEST_PROTECTED_FACES) {
			Block adjacentBlock = chest.getRelative(face);
			if (Utils.isChest(adjacentBlock.getType()) && this.isChestDirectlyProtected(adjacentBlock, player)) {
				return true;
			}
		}

		return false;
	}

	//

	public boolean isProtectedChestAroundHopper(Block hopper, Player player) {
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) return false;
		for (BlockFace face : HOPPER_PROTECTED_FACES) {
			Block adjacentBlock = hopper.getRelative(face);
			if (Utils.isChest(adjacentBlock.getType()) && this.isChestProtected(adjacentBlock, player)) {
				return true;
			}
		}
		return false;
	}

	//

	public List<PlayerShopkeeper> getShopkeeperOwnersOfChest(String worldName, int x, int y, int z) {
		String key = this.getKey(worldName, x, y, z);
		List<PlayerShopkeeper> shopkeepers = protectedChests.get(key);
		if (shopkeepers == null) return Collections.emptyList();
		return Collections.unmodifiableList(shopkeepers);
	}

	public List<PlayerShopkeeper> getShopkeeperOwnersOfChest(Block chest) {
		if (chest == null) return Collections.emptyList();
		return this.getShopkeeperOwnersOfChest(chest.getWorld().getName(), chest.getX(), chest.getY(), chest.getZ());
	}
}
