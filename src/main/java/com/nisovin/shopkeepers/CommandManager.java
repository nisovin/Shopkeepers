package com.nisovin.shopkeepers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shoptypes.AdminShopkeeper;
import com.nisovin.shopkeepers.shoptypes.DefaultShopTypes;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

class CommandManager implements CommandExecutor {

	private static final int LIST_PAGE_SIZE = 6;

	private final ShopkeepersPlugin plugin;

	CommandManager(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	private void sendHelp(CommandSender sender) {
		if (sender == null) return;

		Utils.sendMessage(sender, Settings.msgHelpHeader);
		Utils.sendMessage(sender, Settings.msgCommandHelp);
		if (Utils.hasPermission(sender, ShopkeepersAPI.RELOAD_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandReload);
		}
		if (Utils.hasPermission(sender, ShopkeepersAPI.DEBUG_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandDebug);
		}
		if (Utils.hasPermission(sender, ShopkeepersAPI.LIST_OWN_PERMISSION)
				|| Utils.hasPermission(sender, ShopkeepersAPI.LIST_OTHERS_PERMISSION)
				|| Utils.hasPermission(sender, ShopkeepersAPI.LIST_ADMIN_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandList);
		}
		if (Utils.hasPermission(sender, ShopkeepersAPI.REMOVE_OWN_PERMISSION)
				|| Utils.hasPermission(sender, ShopkeepersAPI.REMOVE_OTHERS_PERMISSION)
				|| Utils.hasPermission(sender, ShopkeepersAPI.REMOVE_ALL_PERMISSION)
				|| Utils.hasPermission(sender, ShopkeepersAPI.REMOVE_ADMIN_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandRemove);
		}
		if (Utils.hasPermission(sender, ShopkeepersAPI.REMOTE_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandRemote);
		}
		if (Utils.hasPermission(sender, ShopkeepersAPI.TRANSFER_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandTransfer);
		}
		if (Utils.hasPermission(sender, ShopkeepersAPI.SETTRADEPERM_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandSettradeperm);
		}
		if (Utils.hasPermission(sender, ShopkeepersAPI.SETFORHIRE_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandSetforhire);
		}
		if (Settings.createPlayerShopWithCommand || Utils.hasPermission(sender, ShopkeepersAPI.ADMIN_PERMISSION)) {
			Utils.sendMessage(sender, Settings.msgCommandShopkeeper);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length > 0 && (args[0].equalsIgnoreCase("help") || args[0].equals("?"))) {
			if (!Utils.hasPermission(sender, ShopkeepersAPI.HELP_PERMISSION)) {
				Utils.sendMessage(sender, Settings.msgNoPermission);
				return true;
			}

			// help page:
			this.sendHelp(sender);
			return true;
		} else if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			if (!Utils.hasPermission(sender, ShopkeepersAPI.RELOAD_PERMISSION)) {
				Utils.sendMessage(sender, Settings.msgNoPermission);
				return true;
			}

			// reload:
			plugin.reload();
			sender.sendMessage(ChatColor.GREEN + "Shopkeepers plugin reloaded!");
			return true;
		} else if (args.length >= 1 && args[0].equalsIgnoreCase("debug")) {
			if (!Utils.hasPermission(sender, ShopkeepersAPI.DEBUG_PERMISSION)) {
				Utils.sendMessage(sender, Settings.msgNoPermission);
				return true;
			}

			// toggle debug mode:
			Log.setDebug(!Log.isDebug());
			sender.sendMessage(ChatColor.GREEN + "Debug mode " + (Log.isDebug() ? "enabled" : "disabled"));
			return true;
		} else if (args.length >= 1 && args[0].equals("check")) {
			if (!Utils.hasPermission(sender, ShopkeepersAPI.DEBUG_PERMISSION)) {
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
			final Player player = (Player) sender;

			if (args.length >= 1 && args[0].equals("checkitem")) {
				if (!Utils.hasPermission(sender, ShopkeepersAPI.DEBUG_PERMISSION)) {
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
				player.sendMessage("-Similar to next item: " + (Utils.isSimilar(nextItem, inHand) ? "yes" : "nope"));

				player.sendMessage("Next item:");
				player.sendMessage("-Is low currency: " + (PlayerShopkeeper.isCurrencyItem(nextItem)));
				player.sendMessage("-Is high currency: " + (PlayerShopkeeper.isHighCurrencyItem(nextItem)));
				player.sendMessage("-Is low zero currency: " + (PlayerShopkeeper.isZeroCurrencyItem(nextItem)));
				player.sendMessage("-Is high zero currency: " + (PlayerShopkeeper.isHighZeroCurrencyItem(nextItem)));

				return true;
			}

			// confirm previous command:
			if (args.length >= 1 && args[0].equals("confirm")) {
				plugin.onConfirm(player);
				return true;
			}

			// list shopkeepers:
			if (args.length >= 1 && args[0].equals("list")) {
				int page = 1;
				String playerName = player.getName();

				if (args.length >= 2) {
					String pageArg = null;
					String arg2 = args[1];
					if (arg2.equals("admin")) {
						// list admin shopkeepers:
						playerName = null;
						if (args.length >= 3) {
							pageArg = args[2];
						}
					} else {
						Integer pageInt = Utils.parseInt(arg2);
						if (pageInt != null) {
							// display different page:
							page = Math.max(1, pageInt.intValue());
						} else {
							playerName = arg2;
							if (args.length >= 3) {
								pageArg = args[2];
							}
						}
					}

					if (pageArg != null) {
						Integer pageInt = Utils.parseInt(pageArg);
						if (pageInt != null) {
							// display different page:
							page = Math.max(1, pageInt.intValue());
						}
					}
				}

				List<Shopkeeper> shops = new ArrayList<Shopkeeper>();

				if (playerName == null) {
					// permission check:
					if (!Utils.hasPermission(sender, ShopkeepersAPI.LIST_ADMIN_PERMISSION)) {
						Utils.sendMessage(sender, Settings.msgNoPermission);
						return true;
					}

					// searching admin shops:
					for (Shopkeeper shopkeeper : plugin.getAllShopkeepers()) {
						if (!(shopkeeper instanceof PlayerShopkeeper)) {
							shops.add(shopkeeper);
						}
					}
				} else {
					// permission check:
					if (playerName.equals(player.getName())) {
						// list own player shopkeepers:
						if (!Utils.hasPermission(sender, ShopkeepersAPI.LIST_OWN_PERMISSION)) {
							Utils.sendMessage(sender, Settings.msgNoPermission);
							return true;
						}
					} else {
						// list other player shopkeepers:
						if (!Utils.hasPermission(sender, ShopkeepersAPI.LIST_OTHERS_PERMISSION)) {
							Utils.sendMessage(sender, Settings.msgNoPermission);
							return true;
						}
					}

					// searching shops of specific player:
					Player listPlayer = Bukkit.getPlayerExact(playerName);
					UUID listPlayerUUID = (listPlayer != null ? listPlayer.getUniqueId() : null);

					for (Shopkeeper shopkeeper : plugin.getAllShopkeepers()) {
						if (shopkeeper instanceof PlayerShopkeeper) {
							PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
							if (playerShop.getOwnerName().equals(playerName)) {
								UUID shopOwnerUUID = playerShop.getOwnerUUID();
								if (shopOwnerUUID == null || shopOwnerUUID.equals(listPlayerUUID) || listPlayerUUID == null) {
									shops.add(playerShop);
								}
							}
						}
					}
				}

				int shopsCount = shops.size();
				int maxPage = (int) (shopsCount / LIST_PAGE_SIZE) + 1;
				page = Math.min(page, maxPage);

				if (playerName == null) {
					// listing admin shops:
					Utils.sendMessage(player, Settings.msgListAdminShopsHeader,
							"{shopsCount}", String.valueOf(shopsCount),
							"{page}", String.valueOf(page));
				} else {
					// listing player shops:
					Utils.sendMessage(player, Settings.msgListPlayerShopsHeader,
							"{player}", playerName,
							"{shopsCount}", String.valueOf(shopsCount),
							"{page}", String.valueOf(page));
				}

				int startIndex = (page - 1) * LIST_PAGE_SIZE;
				int endIndex = Math.min(startIndex + LIST_PAGE_SIZE, shopsCount);
				for (int index = startIndex; index < endIndex; index++) {
					Shopkeeper shopkeeper = shops.get(index);
					String shopName = shopkeeper.getName();
					boolean hasName = shopName != null && !shopName.isEmpty();
					Utils.sendMessage(player, Settings.msgListShopsEntry,
							"{shopIndex}", String.valueOf(index + 1),
							"{shopId}", shopkeeper.getUniqueId().toString(),
							"{shopSessionId}", String.valueOf(shopkeeper.getSessionId()),
							"{shopName}", (hasName ? (shopName + " ") : ""),
							"{location}", shopkeeper.getPositionString(),
							"{shopType}", shopkeeper.getType().getIdentifier(),
							"{objectType}", shopkeeper.getShopObject().getObjectType().getIdentifier());
				}

				return true;
			}

			// remove shopkeepers:
			if (args.length >= 1 && args[0].equals("remove")) {
				final String playerName;
				if (args.length >= 2) {
					playerName = args[1];
				} else {
					playerName = player.getName();
				}

				// permission checks:
				if (playerName.equals("admin")) {
					// remove admin shopkeepers:
					if (!Utils.hasPermission(sender, ShopkeepersAPI.REMOVE_ADMIN_PERMISSION)) {
						Utils.sendMessage(sender, Settings.msgNoPermission);
						return true;
					}
				} else if (playerName.equals("all")) {
					// remove all player shopkeepers:
					if (!Utils.hasPermission(sender, ShopkeepersAPI.REMOVE_ALL_PERMISSION)) {
						Utils.sendMessage(sender, Settings.msgNoPermission);
						return true;
					}
				} else if (playerName.equals(player.getName())) {
					// remove own player shopkeepers:
					if (!Utils.hasPermission(sender, ShopkeepersAPI.REMOVE_OWN_PERMISSION)) {
						Utils.sendMessage(sender, Settings.msgNoPermission);
						return true;
					}
				} else {
					// remove other player shopkeepers:
					if (!Utils.hasPermission(sender, ShopkeepersAPI.REMOVE_OTHERS_PERMISSION)) {
						Utils.sendMessage(sender, Settings.msgNoPermission);
						return true;
					}
				}

				// this is dangerous: let the player first confirm this action
				plugin.waitForConfirm(player, new Runnable() {

					@Override
					public void run() {
						List<Shopkeeper> shops = new ArrayList<Shopkeeper>();

						if (playerName.equals("admin")) {
							// searching admin shops:
							for (Shopkeeper shopkeeper : plugin.getAllShopkeepers()) {
								if (!(shopkeeper instanceof PlayerShopkeeper)) {
									shops.add(shopkeeper);
								}
							}
						} else if (playerName.equals("all")) {
							// searching all player shops:
							for (Shopkeeper shopkeeper : plugin.getAllShopkeepers()) {
								if (shopkeeper instanceof PlayerShopkeeper) {
									shops.add(shopkeeper);
								}
							}
						} else {
							// searching shops of specific player:
							Player listPlayer = Bukkit.getPlayerExact(playerName);
							UUID listPlayerUUID = (listPlayer != null ? listPlayer.getUniqueId() : null);

							for (Shopkeeper shopkeeper : plugin.getAllShopkeepers()) {
								if (shopkeeper instanceof PlayerShopkeeper) {
									PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
									if (playerShop.getOwnerName().equals(playerName)) {
										UUID shopOwnerUUID = playerShop.getOwnerUUID();
										// TODO really ignore owner uuid if the player is currently offline? - consider:
										// TODO * player A 'peter' creating shops
										// TODO * player A leaves, changes name, player B changes name to 'peter'
										// TODO * player B joins before player A has joined again yet, and creates shops
										// TODO * situation: shops with the same owner name, but different uuid.
										// Problem?
										if (shopOwnerUUID == null || listPlayerUUID == null || shopOwnerUUID.equals(listPlayerUUID)) {
											shops.add(playerShop);
										}
									}
								}
							}
						}

						// removing shops:
						for (Shopkeeper shopkeeper : shops) {
							shopkeeper.delete();
						}

						// trigger save:
						plugin.save();

						// printing result message:
						int shopsCount = shops.size();
						if (playerName.equals("admin")) {
							// removed admin shops:
							Utils.sendMessage(player, Settings.msgRemovedAdminShops,
									"{shopsCount}", String.valueOf(shopsCount));
						} else if (playerName.equals("all")) {
							// removed all player shops:
							Utils.sendMessage(player, Settings.msgRemovedAllPlayerShops,
									"{shopsCount}", String.valueOf(shopsCount));
						} else {
							// removed shops of specific player:
							Utils.sendMessage(player, Settings.msgRemovedPlayerShops,
									"{player}", playerName,
									"{shopsCount}", String.valueOf(shopsCount));
						}
					}
				}, 25 * 20); // 25 seconds time for confirmation

				// inform player about required confirmation:
				if (playerName.equals("admin")) {
					// removing admin shops:
					Utils.sendMessage(player, Settings.msgConfirmRemoveAdminShops);
				} else if (playerName.equals("all")) {
					// removing all player shops:
					Utils.sendMessage(player, Settings.msgConfirmRemoveAllPlayerShops);
				} else if (playerName.equals(player.getName())) {
					// removing own shops:
					Utils.sendMessage(player, Settings.msgConfirmRemoveOwnShops);
				} else {
					// removing shops of specific player:
					Utils.sendMessage(player, Settings.msgConfirmRemovePlayerShops,
							"{player}", playerName);
				}

				return true;
			}

			// open remote shop:
			if (args.length >= 1 && args[0].equalsIgnoreCase("remote")) {
				if (!Utils.hasPermission(sender, ShopkeepersAPI.REMOTE_PERMISSION)) {
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

				// find (player) shopkeeper by name or id:
				Shopkeeper shopkeeper = this.getShopkeeper(shopName);
				if (shopkeeper == null || shopkeeper.getType().isPlayerShopType()) {
					// only admin shops can be remotely opened:
					Utils.sendMessage(player, Settings.msgUnknownShopkeeper);
					return true;
				}

				// open shop trading window:
				shopkeeper.openTradingWindow(player);

				return true;
			}

			// get targeted block:
			Block block = null;
			try {
				block = player.getTargetBlock((Set<Material>) null, 10);
			} catch (Exception e) {
				// getTargetBlock might sometimes cause an exception
			}

			// transfer ownership:
			if (args.length >= 1 && args[0].equalsIgnoreCase("transfer")) {
				if (!Utils.hasPermission(sender, ShopkeepersAPI.TRANSFER_PERMISSION)) {
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

				if (block == null || !Utils.isChest(block.getType())) {
					Utils.sendMessage(player, Settings.msgMustTargetChest);
					return true;
				}

				List<PlayerShopkeeper> shopkeepers = plugin.getProtectedChests().getShopkeeperOwnersOfChest(block);
				if (shopkeepers.size() == 0) {
					Utils.sendMessage(player, Settings.msgUnusedChest);
					return true;
				}

				if (!Utils.hasPermission(player, ShopkeepersAPI.BYPASS_PERMISSION)) {
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

			// set trade permission for admin shops:
			if (args.length >= 1 && args[0].equalsIgnoreCase("setTradePerm")) {
				if (!Utils.hasPermission(sender, ShopkeepersAPI.SETTRADEPERM_PERMISSION)) {
					Utils.sendMessage(sender, Settings.msgNoPermission);
					return true;
				}

				String shopIdArg = null;
				String tradePermArg = null;
				if (args.length >= 2) {
					shopIdArg = args[1];
					if (args.length >= 3) {
						tradePermArg = args[2];
					}
				}

				// find (admin) shopkeeper by name or id:
				Shopkeeper shopkeeper = this.getShopkeeper(shopIdArg);
				if (shopkeeper == null || !(shopkeeper instanceof AdminShopkeeper)) {
					Utils.sendMessage(player, Settings.msgUnknownShopkeeper);
					return true;
				}

				// display current trade permission:
				if (tradePermArg == null || tradePermArg.equals("?")) {
					String currentTradePerm = ((AdminShopkeeper) shopkeeper).getTradePremission();
					if (currentTradePerm == null) currentTradePerm = "-";
					Utils.sendMessage(player, Settings.msgTradePermView, "{perm}", currentTradePerm);
					return true;
				}

				String newTradePerm;

				// remove trade permission:
				if (tradePermArg.equals("-")) {
					newTradePerm = null;
					Utils.sendMessage(player, Settings.msgTradePermRemoved);
				} else {
					newTradePerm = tradePermArg;
					Utils.sendMessage(player, Settings.msgTradePermSet);
				}

				// set trade permission:
				((AdminShopkeeper) shopkeeper).setTradePermission(newTradePerm);

				// save:
				plugin.save();

				return true;
			}

			// set for hire:
			if (args.length >= 1 && args[0].equalsIgnoreCase("setforhire")) {
				if (!Utils.hasPermission(sender, ShopkeepersAPI.SETFORHIRE_PERMISSION)) {
					Utils.sendMessage(sender, Settings.msgNoPermission);
					return true;
				}

				if (block == null || !Utils.isChest(block.getType())) {
					Utils.sendMessage(player, Settings.msgMustTargetChest);
					return true;
				}

				List<PlayerShopkeeper> shopkeepers = plugin.getProtectedChests().getShopkeeperOwnersOfChest(block);
				if (shopkeepers.size() == 0) {
					Utils.sendMessage(player, Settings.msgUnusedChest);
					return true;
				}

				if (!Utils.hasPermission(player, ShopkeepersAPI.BYPASS_PERMISSION)) {
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
				if (plugin.getProtectedChests().isChestProtected(block, null)) {
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
					ItemStack itemInHand = player.getItemInHand();
					player.setItemInHand(null);
					TestPlayerInteractEvent fakeInteractEvent = new TestPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP);
					Bukkit.getPluginManager().callEvent(fakeInteractEvent);
					boolean chestAccessDenied = (fakeInteractEvent.useInteractedBlock() == Result.DENY);

					// resetting item in hand:
					player.setItemInHand(itemInHand);

					if (chestAccessDenied) {
						return true;
					}
				}

				// create the player shopkeeper (with the default/first use-able player shop and shop object type)
				ShopType<?> shopType = plugin.getShopTypeRegistry().getDefaultSelection(player);
				ShopObjectType shopObjType = plugin.getShopObjectTypeRegistry().getDefaultSelection(player);

				if (shopType == null || shopObjType == null) {
					// TODO maybe print different kind of no-permission message,
					// because the player cannot create shops at all:
					Utils.sendMessage(player, Settings.msgNoPermission);
					return true;
				}

				if (args.length > 0) {
					if (args.length >= 1) {
						ShopType<?> matchedShopType = plugin.getShopTypeRegistry().match(args[0]);
						if (matchedShopType != null) {
							shopType = matchedShopType;
						} else {
							// check if an object type might be matching:
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

					assert shopType != null && shopObjType != null;

					// can the selected shop type be used?
					if (!shopType.hasPermission(player)) {
						Utils.sendMessage(player, Settings.msgNoPermission);
						return true;
					}
					if (!shopType.isEnabled()) {
						Utils.sendMessage(player, Settings.msgShopTypeDisabled, "{type}", shopType.getIdentifier());
						return true;
					}

					// can the selected shop object type be used?
					if (!shopObjType.hasPermission(player)) {
						Utils.sendMessage(player, Settings.msgNoPermission);
						return true;
					}
					if (!shopObjType.isEnabled()) {
						Utils.sendMessage(player, Settings.msgShopObjectTypeDisabled, "{type}", shopType.getIdentifier());
						return true;
					}
				}

				Block spawnBlock;
				Location spawnLocation;
				BlockFace signFacing = null;

				// TODO move somewhere else
				if (shopObjType == DefaultShopObjectTypes.SIGN()) {
					// determine wall sign facing: // TODO maybe also allow non-wall signs
					signFacing = Utils.getTargetBlockFace(player, block);
					if (signFacing == null || !Utils.isWallSignFace(signFacing)) {
						Utils.sendMessage(player, Settings.msgShopCreateFail);
						return true;
					}
					spawnBlock = block.getRelative(signFacing);
					spawnLocation = spawnBlock.getLocation();
				} else {
					// spawn on top of chest:
					spawnBlock = block.getRelative(BlockFace.UP);
					spawnLocation = spawnBlock.getLocation();
				}

				// check if there is enough space:
				if (spawnBlock.getType() != Material.AIR) {
					Utils.sendMessage(player, Settings.msgShopCreateFail);
					return true;
				}

				// create player shopkeeper:
				plugin.createNewPlayerShopkeeper(new ShopCreationData(player, shopType, shopObjType, spawnLocation, signFacing, block));
				return true;
			} else {
				// create admin shopkeeper:
				if (!Utils.hasPermission(player, ShopkeepersAPI.ADMIN_PERMISSION)) {
					Utils.sendMessage(sender, Settings.msgNoPermission);
					return true;
				}

				ShopObjectType shopObjType = plugin.getDefaultShopObjectType();

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
				}

				Block spawnBlock;
				Location spawnLocation;
				BlockFace signFacing = null;

				// TODO move somewhere else
				if (shopObjType == DefaultShopObjectTypes.SIGN()) {
					// determine wall sign facing: // TODO maybe also allow non-wall signs
					signFacing = Utils.getTargetBlockFace(player, block);
					if (signFacing == null || !Utils.isWallSignFace(signFacing)) {
						Utils.sendMessage(player, Settings.msgShopCreateFail);
						return true;
					}
					spawnBlock = block.getRelative(signFacing);
					spawnLocation = spawnBlock.getLocation();
				} else {
					spawnBlock = block.getRelative(BlockFace.UP);
					// spawn entity shops slightly above:
					spawnLocation = spawnBlock.getLocation().add(0.0D, 0.5D, 0.0D);
				}

				// check if there is enough space:
				if (spawnBlock.getType() != Material.AIR) {
					Utils.sendMessage(player, Settings.msgShopCreateFail);
					return true;
				}

				// create admin shopkeeper:
				plugin.createNewAdminShopkeeper(new ShopCreationData(player, DefaultShopTypes.ADMIN(), shopObjType, spawnLocation, signFacing));
				return true;
			}
		}
	}

	private Shopkeeper getShopkeeper(String shopIdArg) {
		if (shopIdArg == null) return null;

		// check if the argument is an uuid:
		UUID shopUniqueId = null;
		try {
			shopUniqueId = UUID.fromString(shopIdArg);
		} catch (IllegalArgumentException e) {
			// invalid uuid
		}

		if (shopUniqueId != null) {
			return plugin.getShopkeeper(shopUniqueId);
		}

		// check if the argument is an integer:
		int shopSessionId = -1;
		try {
			shopSessionId = Integer.parseInt(shopIdArg);
		} catch (NumberFormatException e) {
			// invalid integer
		}

		if (shopSessionId != -1) {
			return plugin.getShopkeeper(shopSessionId);
		}

		// try to get shopkeeper by name:
		return plugin.getShopkeeperByName(shopIdArg);
	}
}
