package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

public class SignShop extends ShopObject {

	public static String getId(Block block) {
		if (block == null) return null;
		return "block" + block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
	}

	private BlockFace signFacing;

	// update the sign content at least once after plugin start, in case some settings have changed which affect the
	// sign content:
	private boolean updateSign = true;

	protected SignShop(Shopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.signFacing = creationData.blockFace;
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		if (config.isString("signFacing")) {
			String signFacingName = config.getString("signFacing");
			if (signFacingName != null) {
				try {
					signFacing = BlockFace.valueOf(signFacingName);
				} catch (IllegalArgumentException e) {
				}
			}
		}

		// in case no sign facing is stored: try getting the current sign facing from sign in the world
		// if it is not possible (for ex. because the world isn't loaded yet), we will reattempt this
		// during the periodically checks
		if (signFacing == null) {
			signFacing = this.getSignFacingFromWorld();
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		if (signFacing != null) {
			config.set("signFacing", signFacing.name());
		}
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.spawn();
	}

	@Override
	public ShopObjectType getObjectType() {
		return DefaultShopObjectTypes.SIGN();
	}

	public Sign getSign() {
		Location signLocation = this.getActualLocation();
		if (signLocation == null) return null;
		Block signBlock = signLocation.getBlock();
		if (!Utils.isSign(signBlock.getType())) return null;
		return (Sign) signBlock.getState();
	}

	private BlockFace getSignFacingFromWorld() {
		// try getting the current sign facing from the sign in the world:
		Sign sign = this.getSign();
		if (sign != null) {
			return ((Attachable) sign.getData()).getFacing();
		}
		return null;
	}

	@Override
	protected void onChunkLoad() {
		super.onChunkLoad();
		// get the sign facing, in case we weren't able yet, for example because the world wasn't loaded earlier:
		if (signFacing == null) {
			signFacing = this.getSignFacingFromWorld();
		}

		// update sign content if requested:
		if (updateSign) {
			updateSign = false;
			this.updateSign();
		}
	}

	@Override
	public boolean spawn() {
		Location signLocation = this.getActualLocation();
		if (signLocation == null) return false;

		Block signBlock = signLocation.getBlock();
		if (signBlock.getType() != Material.AIR) {
			return false;
		}

		// place sign: // TODO maybe also allow non-wall signs?
		// cancel block physics for this placed sign if needed:
		ShopkeepersPlugin.getInstance().cancelNextBlockPhysics(signLocation);
		signBlock.setType(Material.WALL_SIGN);
		// cleanup state if no block physics were triggered:
		ShopkeepersPlugin.getInstance().cancelNextBlockPhysics(null);

		// in case sign placement has failed for some reason:
		if (!Utils.isSign(signBlock.getType())) {
			return false;
		}

		// set sign facing:
		if (signFacing != null) {
			Sign signState = (Sign) signBlock.getState();
			((Attachable) signState.getData()).setFacingDirection(signFacing);
			// apply facing:
			signState.update();
		}

		// init sign content:
		updateSign = false;
		this.updateSign();

		return true;
	}

	@Override
	public boolean isActive() {
		Location signLocation = this.getActualLocation();
		if (signLocation == null) return false;
		Block signBlock = signLocation.getBlock();
		return Utils.isSign(signBlock.getType());
	}

	@Override
	public String getId() {
		Location location = shopkeeper.getLocation();
		if (location == null) return null;
		return getId(location.getBlock());
	}

	@Override
	public Location getActualLocation() {
		return shopkeeper.getLocation();
	}

	@Override
	public void setName(String name) {
		// always uses the name of the shopkeeper:
		this.updateSign();
	}

	@Override
	public int getNameLengthLimit() {
		return 15;
	}

	@Override
	public void setItem(ItemStack item) {
	}

	public void updateSign() {
		Sign sign = this.getSign();
		if (sign == null) {
			updateSign = true; // request update, once the sign is available again
			return;
		}

		// line 0: header
		sign.setLine(0, Settings.signShopFirstLine);

		// line 1: shop name
		String name = shopkeeper.getName();
		String line1 = "";
		if (name != null) {
			name = this.trimToNameLength(name);
			line1 = name;
		}
		sign.setLine(1, line1);

		// line 2: owner name
		String line2 = "";
		if (shopkeeper instanceof PlayerShopkeeper) {
			line2 = ((PlayerShopkeeper) shopkeeper).getOwnerName();
		}
		sign.setLine(2, line2);

		// line 3: empty
		sign.setLine(3, "");

		// apply sign changes:
		sign.update();
	}

	@Override
	public boolean check() {
		if (!shopkeeper.getChunkData().isChunkLoaded()) {
			// only verify sign, if the chunk is currently loaded:
			return false;
		}

		Sign sign = this.getSign();
		if (sign == null) {
			String worldName = shopkeeper.getWorldName();
			int x = shopkeeper.getX();
			int y = shopkeeper.getY();
			int z = shopkeeper.getZ();

			// removing the shopkeeper, because re-spawning might fail (ex. attached block missing) or could be abused
			// (sign drop farming):
			Log.debug("Shopkeeper sign at (" + worldName + "," + x + "," + y + "," + z + ") is no longer existing! Attempting respawn now.");
			if (!this.spawn()) {
				Log.warning("Shopkeeper sign at (" + worldName + "," + x + "," + y + "," + z + ") could not be replaced! Removing shopkeeper now!");
				// delayed removal:
				Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), new Runnable() {

					@Override
					public void run() {
						shopkeeper.delete();
					}
				});
			}
			return true;
		}

		// update sign content if requested:
		if (updateSign) {
			updateSign = false;
			this.updateSign();
		}

		return false;
	}

	@Override
	public void despawn() {
	}

	@Override
	public void delete() {
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		if (world != null) {
			// this should load the chunk if necessary, making sure that the block gets removed (though, might not work
			// on server stops..):
			Block signBlock = world.getBlockAt(shopkeeper.getX(), shopkeeper.getY(), shopkeeper.getZ());
			if (Utils.isSign(signBlock.getType())) {
				// remove sign:
				signBlock.setType(Material.AIR);
			}
			// TODO trigger an unloadChunkRequest if the chunk had to be loaded? (for now let's assume that the server
			// handles that kind of thing automatically)
		} else {
			// well: world unloaded and we didn't get an event.. not our fault
			// TODO actually, we are not removing the sign on world unloads..
		}
	}

	@Override
	public ItemStack getSubTypeItem() {
		return null;
	}

	@Override
	public void cycleSubType() {
	}
}
