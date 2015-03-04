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

	private void sendHelp(CommandSender sender) {
		if (sender == null) return;

		Utils.sendMessage(sender, Settings.msgHelpHeader);
		Utils.sendMessage(sender, Settings.msgCommandHelp);
		if (sender.hasPermission(ShopkeepersAPI.RELOAD_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandReload);
		}
		if (sender.hasPermission(ShopkeepersAPI.DEBUG_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandDebug);
		}
		if (sender.hasPermission(ShopkeepersAPI.REMOTE_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandRemote);
		}
		if (sender.hasPermission(ShopkeepersAPI.TRANSFER_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandTransfer);
		}
		if (sender.hasPermission(ShopkeepersAPI.SETFORHIRE_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandSetforhire);
		}
		if (Settings.createPlayerShopWithCommand || sender.hasPermission(ShopkeepersAPI.ADMIN_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandShopkeeper);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length > 0 && (args[0].equalsIgnoreCase("help") || args[0].equals("?"))) {
			if (!sender.hasPermission(ShopkeepersAPI.HELP_PERMISSION)) {
				Utils.sendMessage(sender, Settings.msgNoPermission);
				return true;
			}

			// help page:
			this.sendHelp(sender);
			return true;
		} else if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission(ShopkeepersAPI.RELOAD_PERMISSION)) {
				Utils.sendMessage(sender, Settings.msgNoPermission);
				return true;
			}

			// reload:
			plugin.reload();
			sender.sendMessage(ChatColor.GREEN + "Shopkeepers plugin reloaded!");
			return true;
		} else if (args.length >= 1 && args[0].equalsIgnoreCase("debug")) {
			if (!sender.hasPermission(ShopkeepersAPI.DEBUG_PERMISSION)) {
				Utils.sendMessage(sender, Settings.msgNoPermission);
				return true;
			}

			// toggle debug mode:
			Log.setDebug(!Log.isDebug());
			sender.sendMessage(ChatColor.GREEN + "Debug mode " + (Log.isDebug() ? "enabled" : "disabled"));
			return true;
		} else if (args.length >= 1 && args[0].equals("check")) {
			if (!sender.hasPermission(ShopkeepersAPI.DEBUG_PERMISSION)) {
				Utils.sendMessage(sender, Settings.msgNoPermission);
				return true;
			}

			for (Shopkeeper shopkeeper : plugin.getActiveShopkeepers()) {
				if (shopkeeper.isActive()) {
					Location loc = shopkeeper.getActualLocation();
					sender.sendMessage("Shopkeeper at " + shopkeeper.getPositionString() + ": active (" + (loc != null ? loc.toString() : "maybe not?!?") + ")");
				} else {
					sender.sendMessage("Shopkeeper at " + shopkeeper.getPositionString() + ": INACTIVE!");
				}
			}
			return true;
		} else if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player in order to do that.");
			sender.sendMessage("See 'shopkeepers help' for all available commands.");
			return true;
		} else {
			// all player-only commands:
			Player player = (Player) sender;

			if (args.length >= 1 && args[0].equals("checkitem")) {
				if (!sender.hasPermission(ShopkeepersAPI.DEBUG_PERMISSION)) {
					Utils.sendMessage(sender, Settings.msgNoPermission);
					return true;
				}

				ItemStack inHand = player.getItemInHand();
				int holdSlot = player.getInventory().getHeldItemSlot();
				ItemStack nextItem = player.getInventory().getItem(holdSlot == 8 ? 0 : holdSlot + 1);

				player.sendMessage("Item in hand:");
				player.sendMessage("-Is low currency: " + (PlayerShopkeeper.isCurrencyItem(inHand)));
				player.sendMessage("-Is high currency: " + (PlayerShopkeeper.isHighCurrencyItem(inHand)));
				player.sendMessage("-Is low zero currency: " + (PlayerShopkeeper.isZeroCurrencyItem(inHand)));
				player.sendMessage("-Is high zero currency: " + (PlayerShopkeeper.isHighZeroCurrencyItem(inHand)));
				player.sendMessage("-Similar to next item: " + (Utils.areSimilarReasoned(nextItem, inHand)));

				player.sendMessage("Next item:");
				player.sendMessage("-Is low currency: " + (PlayerShopkeeper.isCurrencyItem(nextItem)));
				player.sendMessage("-Is high currency: " + (PlayerShopkeeper.isHighCurrencyItem(nextItem)));
				player.sendMessage("-Is low zero currency: " + (PlayerShopkeeper.isZeroCurrencyItem(nextItem)));
				player.sendMessage("-Is high zero currency: " + (PlayerShopkeeper.isHighZeroCurrencyItem(nextItem)));

				return true;
			}

			// open remote shop:
			if (args.length >= 1 && args[0].equalsIgnoreCase("remote")) {
				if (!sender.hasPermission(ShopkeepersAPI.REMOTE_PERMISSION)) {
					Utils.sendMessage(sender, Settings.msgNoPermission);
					return true;
				}

				String shopName = null;
				if (args.length >= 2) {
					shopName = args[1];
					for (int i = 2; i < args.length; i++) {
						shopName += " " + args[i];
					}
				}

				boolean opened = false;
				if (shopName != null) {
					for (Shopkeeper shopkeeper : plugin.getAllShopkeepers()) {
						if (!shopkeeper.getType().isPlayerShopType() && shopkeeper.getName() != null && ChatColor.stripColor(shopkeeper.getName()).equalsIgnoreCase(shopName)) {
							shopkeeper.openTradingWindow(player);
							break;
						}
					}
				}

				if (!opened) {
					Utils.sendMessage(player, Settings.msgUnknownShopkeeper);
				}
				return true;
			}

			Block block = player.getTargetBlock(null, 10);

			// transfer ownership:
			if (args.length >= 1 && args[0].equalsIgnoreCase("transfer")) {
				if (!sender.hasPermission(ShopkeepersAPI.TRANSFER_PERMISSION)) {
					Utils.sendMessage(sender, Settings.msgNoPermission);
					return true;
				}

				Player newOwner = null;
				if (args.length >= 2) {
					newOwner = Bukkit.getPlayer(args[1]);
				}

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

			// set for hire:
			if (args.length >= 1 && args[0].equalsIgnoreCase("setforhire")) {
				if (!sender.hasPermission(ShopkeepersAPI.SETFORHIRE_PERMISSION)) {
					Utils.sendMessage(sender, Settings.msgNoPermission);
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

				if (!player.hasPermission(ShopkeepersAPI.BYPASS_PERMISSION)) {
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

			// creating new shopkeeper:

			// check for valid spawn location:
			if (block == null || block.getType() == Material.AIR) {
				Utils.sendMessage(player, Settings.msgShopCreateFail);
				return true;
			}

			if (Settings.createPlayerShopWithCommand && Utils.isChest(block.getType())) {
				// create player shopkeeper:

				// check if this chest is already used by some other shopkeeper:
				if (plugin.isChestProtected(null, block)) {
					Utils.sendMessage(player, Settings.msgShopCreateFail);
					return true;
				}

				// check for recently placed:
				if (Settings.requireChestRecentlyPlaced) {
					if (!plugin.wasRecentlyPlaced(player, block)) {
						Utils.sendMessage(player, Settings.msgChestNotPlaced);
						return true;
					}
				}

				// check for permission:
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

				if (args.length > 0) {
					if (args.length >= 1) {
						ShopType<?> matchedShopType = plugin.getShopTypeRegistry().match(args[0]);
						if (matchedShopType != null) {
							shopType = matchedShopType;
						} else {
							// check if an object type is matching:
							ShopObjectType matchedObjectType = plugin.getShopObjectTypeRegistry().match(args[0]);
							if (matchedObjectType != null) {
								shopObjType = matchedObjectType;
							} else {
								Utils.sendMessage(player, Settings.msgUnknowShopType, "{type}", args[0]);
								return true;
							}
						}
					}
					if (args.length >= 2) {
						ShopObjectType matchedObjectType = plugin.getShopObjectTypeRegistry().match(args[1]);
						if (matchedObjectType != null) {
							shopObjType = matchedObjectType;
						} else {
							Utils.sendMessage(player, Settings.msgUnknowShopObjectType, "{type}", args[1]);
							return true;
						}
					}

					if (shopType != null) {
						if (!shopType.hasPermission(player)) {
							Utils.sendMessage(player, Settings.msgNoPermission);
							return true;
						}
						if (!shopType.isEnabled()) {
							Utils.sendMessage(player, Settings.msgShopTypeDisabled, "{type}", shopType.getIdentifier());
							return true;
						}
					}
					if (shopObjType != null) {
						if (!shopObjType.hasPermission(player)) {
							Utils.sendMessage(player, Settings.msgNoPermission);
							return true;
						}
						if (!shopObjType.isEnabled()) {
							Utils.sendMessage(player, Settings.msgShopObjectTypeDisabled, "{type}", shopType.getIdentifier());
							return true;
						}
					}
				}

				if (shopType == null || shopObjType == null) {
					Utils.sendMessage(player, Settings.msgShopCreateFail);
					return true;
				}

				// create player shopkeeper:
				plugin.createNewPlayerShopkeeper(new ShopCreationData(player, shopType, block, block.getLocation().add(0, 1.5, 0), shopObjType));
				return true;
			} else {
				// create admin shopkeeper:
				if (!player.hasPermission(ShopkeepersAPI.ADMIN_PERMISSION)) {
					Utils.sendMessage(sender, Settings.msgNoPermission);
					return true;
				}

				ShopObjectType shopObjType = LivingEntityType.VILLAGER.getObjectType();
				Location location = block.getLocation().add(0, 1.5, 0);

				if (args.length > 0) {
					ShopObjectType matchedObjectType = plugin.getShopObjectTypeRegistry().match(args[0]);
					if (matchedObjectType == null) {
						Utils.sendMessage(player, Settings.msgUnknowShopObjectType, "{type}", args[0]);
						return true;
					}
					if (!matchedObjectType.isEnabled()) {
						Utils.sendMessage(player, Settings.msgShopObjectTypeDisabled, "{type}", matchedObjectType.getIdentifier());
						return true;
					}

					shopObjType = matchedObjectType;
					if (shopObjType == DefaultShopObjectTypes.SIGN) location = block.getLocation(); // TODO do this in an object type independent way?
				}

				// create admin shopkeeper:
				plugin.createNewAdminShopkeeper(new ShopCreationData(player, DefaultShopTypes.ADMIN, location, shopObjType));
				return true;
			}
		}
	}
}