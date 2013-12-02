package com.nisovin.shopkeepers;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Attachable;

import com.nisovin.shopkeepers.shopobjects.ShopObject;

public class CreateListener implements Listener {

	ShopkeepersPlugin plugin;
	
	public CreateListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) return;
		
		// get player, ignore creative mode
		final Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE) return;
		
		// make sure item in hand is the creation item
		final ItemStack inHand = player.getItemInHand();
		if (inHand == null || inHand.getType() != Settings.shopCreationItem || inHand.getDurability() != Settings.shopCreationItemData) {
			return;
		}
		if (Settings.shopCreationItemName != null && !Settings.shopCreationItemName.isEmpty()) {
			ItemMeta meta = inHand.getItemMeta();
			if (!meta.hasDisplayName() || !meta.getDisplayName().equals(Settings.shopCreationItemName)) {
				return;
			}
		}
		
		// prevent regular usage
		if (Settings.preventShopCreationItemRegularUsage && !player.isOp() && !player.hasPermission("shopkeeper.bypass")) {
			event.setUseItemInHand(Result.DENY);
		}
		
		// check for player shop spawn
		String playerName = player.getName();
		if (event.getAction() == Action.RIGHT_CLICK_AIR) {
			if (player.isSneaking()) {
				// cycle shop objects
				ShopObjectType shopObjectType = plugin.selectedShopObjectType.get(playerName);
				shopObjectType = ShopObjectType.next(player, shopObjectType);
				if (shopObjectType != null) {
					plugin.selectedShopObjectType.put(playerName, shopObjectType);
					if (shopObjectType == ShopObjectType.VILLAGER) {
						plugin.sendMessage(player, Settings.msgSelectedVillagerShop);
					} else if (shopObjectType == ShopObjectType.SIGN) {
						plugin.sendMessage(player, Settings.msgSelectedSignShop);
					} else if (shopObjectType == ShopObjectType.WITCH) {
						plugin.sendMessage(player, Settings.msgSelectedWitchShop);
					} else if (shopObjectType == ShopObjectType.CREEPER) {
						plugin.sendMessage(player, Settings.msgSelectedCreeperShop);
					}
				}
			} else {
				// cycle shopkeeper types
				ShopkeeperType shopType = plugin.selectedShopType.get(playerName);
				shopType = ShopkeeperType.next(player, shopType);
				if (shopType != null) {
					plugin.selectedShopType.put(playerName, shopType);
					if (shopType == ShopkeeperType.PLAYER_NORMAL) {
						plugin.sendMessage(player, Settings.msgSelectedNormalShop);
					} else if (shopType == ShopkeeperType.PLAYER_BOOK) {
						plugin.sendMessage(player, Settings.msgSelectedBookShop);
					} else if (shopType == ShopkeeperType.PLAYER_BUY) {
						plugin.sendMessage(player, Settings.msgSelectedBuyShop);
					} else if (shopType == ShopkeeperType.PLAYER_TRADE) {
						plugin.sendMessage(player, Settings.msgSelectedTradeShop);
					}
				}
			}
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block block = event.getClickedBlock();
			if (block.getType() == Material.CHEST && (!plugin.selectedChest.containsKey(playerName) || !plugin.selectedChest.get(playerName).equals(block))) {
				// selecting a chest
				if (event.useInteractedBlock() != Result.DENY) {
					// check if it's recently placed
					List<String> list = plugin.recentlyPlacedChests.get(playerName);
					if (Settings.requireChestRecentlyPlaced && (list == null || !list.contains(block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ()))) {
						// chest not recently placed
						plugin.sendMessage(player, Settings.msgChestNotPlaced);
					} else {
						// select chest
						plugin.selectedChest.put(playerName, event.getClickedBlock());
						plugin.sendMessage(player, Settings.msgSelectedChest);
					}
				} else {
					ShopkeepersPlugin.debug("Right-click on chest prevented, player " + player.getName() + " at " + block.getLocation().toString());
				}
				event.setCancelled(true);
				
			} else if (plugin.selectedChest.containsKey(playerName)) {
				// placing shop
				Block chest = plugin.selectedChest.get(playerName);
				
				// check for too far
				if (!chest.getWorld().equals(block.getWorld()) || (int)chest.getLocation().distance(block.getLocation()) > Settings.maxChestDistance) {
					plugin.sendMessage(player, Settings.msgChestTooFar);
				} else {
					// get shop type
					ShopkeeperType shopType = plugin.selectedShopType.get(playerName);
					if (shopType == null) shopType = ShopkeeperType.next(player, null);
					ShopObjectType objType = plugin.selectedShopObjectType.get(playerName);
					if (objType == null) objType = ShopObjectType.next(player, null);
					
					if (objType == ShopObjectType.SIGN && !validSignFace(event.getBlockFace())) {
						return;
					}
					
					if (shopType != null && objType != null) {
						ShopObject obj = objType.createObject();
						if (obj != null) {
							// create player shopkeeper
							Block sign = event.getClickedBlock().getRelative(event.getBlockFace());
							if (sign.getType() == Material.AIR) {
								Shopkeeper shopkeeper = plugin.createNewPlayerShopkeeper(player, chest, sign.getLocation(), shopType, obj);
								if (shopkeeper != null) {
									// perform special setup
									if (objType == ShopObjectType.SIGN) {
										// set sign
										sign.setType(Material.WALL_SIGN);
										Sign signState = (Sign)sign.getState();
										((Attachable)signState.getData()).setFacingDirection(event.getBlockFace());
										signState.setLine(0, Settings.signShopFirstLine);
										signState.setLine(2, playerName);
										signState.update();
										event.setCancelled(true);
									}									
									// send message
									plugin.sendCreatedMessage(player, shopType);
								}								
								// clear selection vars
								plugin.selectedShopType.remove(playerName);
								plugin.selectedChest.remove(playerName);
								// remove creation item manually
								event.setCancelled(true);
								Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
									public void run() {
										if (inHand.getAmount() <= 1) {
											player.setItemInHand(null);
										} else {
											inHand.setAmount(inHand.getAmount() - 1);
											player.setItemInHand(inHand);
										}
									}
								});
							}
						}
					}
				}
			}
		}
	}
	
	private boolean validSignFace(BlockFace face) {
		return face == BlockFace.NORTH || face == BlockFace.SOUTH || face == BlockFace.EAST || face == BlockFace.WEST;
	}
	
}
