package com.nisovin.shopkeepers;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityType;
import com.nisovin.shopkeepers.shoptypes.DefaultShopTypes;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

class CommandManager implements CommandExecutor {

	private final ShopkeepersPlugin plugin;

	CommandManager(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("shopkeeper.reload")) {
			// reload command
			plugin.reload();
			sender.sendMessage(ChatColor.GREEN + "Shopkeepers plugin reloaded!");
			return true;
		} else if (args.length == 1 && args[0].equalsIgnoreCase("debug") && sender.isOp()) {
			// toggle debug command
			Log.setDebug(!Log.isDebug());
			sender.sendMessage(ChatColor.GREEN + "Debug mode " + (Log.isDebug() ? "enabled" : "disabled"));
			return true;

		} else if (args.length == 1 && args[0].equals("check") && sender.isOp()) {
			for (Shopkeeper shopkeeper : plugin.getActiveShopkeepers()) {
				if (shopkeeper.isActive()) {
					Location loc = shopkeeper.getActualLocation();
					sender.sendMessage("Shopkeeper at " + shopkeeper.getPositionString() + ": active (" + (loc != null ? loc.toString() : "maybe not?!?") + ")");
				} else {
					sender.sendMessage("Shopkeeper at " + shopkeeper.getPositionString() + ": INACTIVE!");
				}
			}
			return true;

		} else if (sender instanceof Player) {
			Player player = (Player) sender;
			Block block = player.getTargetBlock(null, 10); // TODO: fix this when API becomes available

			// transfer ownership
			if (args.length == 2 && args[0].equalsIgnoreCase("transfer") && player.hasPermission("shopkeeper.transfer")) {
				Player newOwner = Bukkit.getPlayer(args[1]);
				if (newOwner == null) {
					Utils.sendMessage(player, Settings.msgUnknownPlayer);
					return true;
				}
				if (!Utils.isChest(block.getType())) {
					Utils.sendMessage(player, Settings.msgMustTargetChest);
					return true;
				}
				List<PlayerShopkeeper> shopkeepers = plugin.getShopkeeperOwnersOfChest(block);
				if (shopkeepers.size() == 0) {
					Utils.sendMessage(player, Settings.msgUnusedChest);
					return true;
				}
				if (!player.hasPermission("shopkeeper.bypass")) {
					for (PlayerShopkeeper shopkeeper : shopkeepers) {
						if (!shopkeeper.isOwner(player)) {
							Utils.sendMessage(player, Settings.msgNotOwner);
							return true;
						}
					}
				}
				for (PlayerShopkeeper shopkeeper : shopkeepers) {
					shopkeeper.setOwner(newOwner);
				}
				plugin.save();
				Utils.sendMessage(player, Settings.msgOwnerSet.replace("{owner}", newOwner.getName()));
				return true;
			}

			// set for hire
			if (args.length == 1 && args[0].equalsIgnoreCase("setforhire") && player.hasPermission("shopkeeper.setforhire")) {
				if (!Utils.isChest(block.getType())) {
					Utils.sendMessage(player, Settings.msgMustTargetChest);
					return true;
				}
				List<PlayerShopkeeper> shopkeepers = plugin.getShopkeeperOwnersOfChest(block);
				if (shopkeepers.size() == 0) {
					Utils.sendMessage(player, Settings.msgUnusedChest);
					return true;
				}
				if (!player.hasPermission("shopkeeper.bypass")) {
					for (PlayerShopkeeper shopkeeper : shopkeepers) {
						if (!shopkeeper.isOwner(player)) {
							Utils.sendMessage(player, Settings.msgNotOwner);
							return true;
						}
					}
				}
				ItemStack hireCost = player.getItemInHand();
				if (hireCost == null || hireCost.getType() == Material.AIR || hireCost.getAmount() == 0) {
					Utils.sendMessage(player, Settings.msgMustHoldHireItem);
					return true;
				}
				for (PlayerShopkeeper shopkeeper : shopkeepers) {
					shopkeeper.setForHire(true, hireCost.clone());
				}
				plugin.save();
				Utils.sendMessage(player, Settings.msgSetForHire);
				return true;
			}

			// open remote shop
			if (args.length >= 2 && args[0].equalsIgnoreCase("remote") && player.hasPermission("shopkeeper.remote")) {
				String shopName = args[1];
				for (int i = 2; i < args.length; i++) {
					shopName += " " + args[i];
				}
				boolean opened = false;
				for (List<Shopkeeper> list : plugin.getAllShopkeepersByChunks()) {
					for (Shopkeeper shopkeeper : list) {
						if (!shopkeeper.getType().isPlayerShopType() && shopkeeper.getName() != null && ChatColor.stripColor(shopkeeper.getName()).equalsIgnoreCase(shopName)) {
							shopkeeper.openTradingWindow(player);
							opened = true;
							break;
						}
					}
					if (opened) break;
				}
				if (!opened) {
					Utils.sendMessage(player, Settings.msgUnknownShopkeeper);
				}
				return true;
			}

			// get the spawn location for the shopkeeper
			if (block != null && block.getType() != Material.AIR) {
				if (Settings.createPlayerShopWithCommand && Utils.isChest(block.getType())) {
					// check if this chest is already used by some other shopkeeper:
					if (plugin.isChestProtected(null, block)) {
						Utils.sendMessage(player, Settings.msgShopCreateFail);
						return true;
					}
					// check for recently placed
					if (Settings.requireChestRecentlyPlaced) {
						if (!plugin.wasRecentlyPlaced(player, block)) {
							Utils.sendMessage(player, Settings.msgChestNotPlaced);
							return true;
						}
					}
					// check for permission
					if (Settings.simulateRightClickOnCommand) {
						PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), block, BlockFace.UP);
						Bukkit.getPluginManager().callEvent(event);
						if (event.isCancelled()) {
							return true;
						}
					}
					// create the player shopkeeper (with the default/first use-able player shop and shop object type)
					ShopType<?> shopType = plugin.getShopTypeRegistry().getDefaultSelection(player);
					ShopObjectType shopObjType = plugin.getShopObjectTypeRegistry().getDefaultSelection(player);
					if (args != null && args.length > 0) {
						if (args.length >= 1) {
							ShopType<?> matchedShopType = plugin.getShopTypeRegistry().match(args[0]);
							if (matchedShopType != null) {
								shopType = matchedShopType;
							} else {
								// check if an object type is matching:
								ShopObjectType matchedObjectType = plugin.getShopObjectTypeRegistry().match(args[0]);
								if (matchedObjectType != null) {
									shopObjType = matchedObjectType;
								}
							}
						}
						if (args.length >= 2) {
							ShopObjectType matchedObjectType = plugin.getShopObjectTypeRegistry().match(args[0]);
							if (matchedObjectType != null) {
								shopObjType = matchedObjectType;
							}
						}
						if (shopType != null && (!shopType.isEnabled() || shopType.hasPermission(player))) {
							shopType = null;
						}
						if (shopObjType != null && (!shopObjType.isEnabled() || shopObjType.hasPermission(player))) {
							shopObjType = null;
						}
					}
					if (shopType != null && shopObjType != null) {
						plugin.createNewPlayerShopkeeper(new ShopCreationData(player, shopType, block, block.getLocation().add(0, 1.5, 0), shopObjType));
					} else {
						// TODO print some message about invalid shopType/shopObjType here?
					}
				} else if (player.hasPermission("shopkeeper.admin")) {
					// create the admin shopkeeper
					ShopObjectType shopObjType = LivingEntityType.VILLAGER.getObjectType();
					Location location = block.getLocation().add(0, 1.5, 0);
					if (args.length > 0) {
						ShopObjectType matchedObjectType = plugin.getShopObjectTypeRegistry().match(args[0]);
						if (matchedObjectType == null) {
							Log.debug("Unknown shop object type: " + args[0]);
							// TODO maybe print message to player and stop shop creation?
						} else if (!matchedObjectType.isEnabled()) {
							Log.debug("Shop object type '" + matchedObjectType.getIdentifier() + "' is disabled!");
							// TODO maybe print message to player and stop shop creation?
						} else {
							shopObjType = matchedObjectType;
							if (shopObjType == DefaultShopObjectTypes.SIGN) location = block.getLocation(); // TODO do this in an object type independent way
						}
					}
					plugin.createNewAdminShopkeeper(new ShopCreationData(player, DefaultShopTypes.ADMIN, location, shopObjType));
				}
			} else {
				Utils.sendMessage(player, Settings.msgShopCreateFail);
			}

			return true;
		} else {
			sender.sendMessage("You must be a player to create a shopkeeper.");
			sender.sendMessage("Use 'shopkeeper reload' to reload the plugin.");
			return true;
		}
	}
}