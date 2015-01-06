package com.nisovin.shopkeepers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

import com.nisovin.shopkeepers.shopobjects.DefaultShopObjectTypes;

class CreateListener implements Listener {

	private static class ClickData {

		private ItemStack itemInHand = null;
		private boolean selectingChest = false;

		ClickData(ItemStack itemInHand, boolean selectingChest) {
			this.itemInHand = itemInHand;
			this.selectingChest = selectingChest;
		}
	}

	private final ShopkeepersPlugin plugin;

	// <playerUUID -> ClickData>
	private final Map<UUID, ClickData> clickingWithCreationItem = new HashMap<UUID, ClickData>();

	CreateListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	void onPlayerInteractEarly(PlayerInteractEvent event) {
		if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) return;

		// get player, ignore creative mode
		final Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE) return;

		// make sure item in hand is the creation item
		final ItemStack inHand = player.getItemInHand();
		if (inHand == null || inHand.getType() != Settings.shopCreationItem || inHand.getDurability() != Settings.shopCreationItemData) {
			return;
		}
		ItemMeta meta = inHand.getItemMeta();
		if (Settings.shopCreationItemName != null && !Settings.shopCreationItemName.isEmpty()) {
			if (!meta.hasDisplayName() || !meta.getDisplayName().equals(Settings.shopCreationItemName)) {
				return;
			}
		}
		if (Settings.shopCreationItemLore != null && !Settings.shopCreationItemLore.isEmpty()) {
			if (!meta.hasLore() || !meta.getLore().equals(Settings.shopCreationItemLore)) {
				return;
			}
		}

		if (clickingWithCreationItem.containsKey(player.getUniqueId())) {
			// something called another PlayerInteractEvent inside the last PlayerInteractEvent..:
			Log.debug("Detected click with shop creation item while already handling another click with shop creation item for player '" + player.getName() + "'.");
			event.setCancelled(true); // making sure that the shop creation item cannot be used
			return; // don't handle the event twice, focus on the event we already have
		}

		boolean selectingChest = false;

		if (event.getAction() == Action.RIGHT_CLICK_AIR) {
			if (player.isSneaking()) {
				// cycle shop objects
				plugin.getShopObjectTypeRegistry().selectNext(player);
			} else {
				// cycle shopkeeper types
				plugin.getShopTypeRegistry().selectNext(player);
			}
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block block = event.getClickedBlock();
			assert block != null;
			Block selectedChest = plugin.getSelectedChest(player);

			if (Utils.isChest(block.getType()) && !block.equals(selectedChest)) {
				// handle chest selection in later event phase:
				selectingChest = true;
			} else if (selectedChest != null) {
				assert Utils.isChest(selectedChest.getType());
				// placing player shop:

				// check for too far:
				if (!selectedChest.getWorld().getUID().equals(block.getWorld().getUID()) || (int) selectedChest.getLocation().distanceSquared(block.getLocation()) > (Settings.maxChestDistance * Settings.maxChestDistance)) {
					Utils.sendMessage(player, Settings.msgChestTooFar);
				} else {
					// get shop type
					ShopType<?> shopType = plugin.getShopTypeRegistry().getSelection(player);
					// get shop object type
					ShopObjectType objType = plugin.getShopObjectTypeRegistry().getSelection(player);

					// TODO move object type specific stuff into the object type instead
					if (shopType != null && objType != null && !(objType == DefaultShopObjectTypes.SIGN && !validSignFace(event.getBlockFace()))) {
						// create player shopkeeper
						Block spawnBlock = event.getClickedBlock().getRelative(event.getBlockFace());
						if (spawnBlock.getType() == Material.AIR) {
							ShopCreationData creationData = new ShopCreationData(player, shopType, selectedChest, spawnBlock.getLocation(), objType);
							Shopkeeper shopkeeper = plugin.createNewPlayerShopkeeper(creationData);
							if (shopkeeper != null) {
								// perform special setup
								if (objType == DefaultShopObjectTypes.SIGN) {
									// set sign
									spawnBlock.setType(Material.WALL_SIGN);
									Sign signState = (Sign) spawnBlock.getState();
									((Attachable) signState.getData()).setFacingDirection(event.getBlockFace());
									signState.setLine(0, Settings.signShopFirstLine);
									signState.setLine(2, player.getName());
									signState.update();
								}

								// remove creation item manually:
								event.setCancelled(true);
								Bukkit.getScheduler().runTask(plugin, new Runnable() {
									public void run() { // TODO can the player (very) quickly change the item in hand?
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
			} else {
				// clicked a location without a chest selected
				Utils.sendMessage(player, Settings.msgMustSelectChest);
			}
		}

		// preparing for event handling in pater phase:
		clickingWithCreationItem.put(player.getUniqueId(), new ClickData(inHand, selectingChest));

		// removing creation item from player's hand, so that the click events only gets cancelled by other plugins
		// if they are trying to prevent chest access (which we want to detect) and not because of the usage of the item in hand
		// we are resetting it in a later phase of the event handling
		player.setItemInHand(null);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	void onPlayerInteractLate(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ClickData clickData = clickingWithCreationItem.remove(player.getUniqueId());
		if (clickData == null) return; // player wasn't clicking with creation item

		// reset item in hand:
		player.setItemInHand(clickData.itemInHand);

		if (clickData.selectingChest) {
			Block block = event.getClickedBlock();
			assert block != null && Utils.isChest(block.getType());

			// handle chest selection:
			if (event.useInteractedBlock() == Result.DENY) {
				// check if the chest was recently placed:
				if (Settings.requireChestRecentlyPlaced && !plugin.wasRecentlyPlaced(player, block)) {
					// chest was not recently placed:
					Utils.sendMessage(player, Settings.msgChestNotPlaced);
				} else {
					// select chest:
					plugin.selectChest(player, block);
					Utils.sendMessage(player, Settings.msgSelectedChest);
				}
			} else {
				// player has no access to that chest
				Log.debug("Right-click on chest prevented, player " + player.getName() + " at " + block.getLocation().toString());
			}
			event.setCancelled(true);
		} else {
			// prevent regular usage (do this last because otherwise the canceling can interfere with logic above)
			if (Settings.preventShopCreationItemRegularUsage && !player.hasPermission(ShopkeepersPlugin.BYPASS_PERMISSION)) {
				Log.debug("Preventing normal shop creation item usage");
				event.setCancelled(true);
			}
		}
	}

	private boolean validSignFace(BlockFace face) {
		return face == BlockFace.NORTH || face == BlockFace.SOUTH || face == BlockFace.EAST || face == BlockFace.WEST;
	}
}