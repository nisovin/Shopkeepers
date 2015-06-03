package com.nisovin.shopkeepers;

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

	private final ShopkeepersPlugin plugin;

	CreateListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		if (event instanceof TestPlayerInteractEvent) return;
		Action action = event.getAction();
		if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

		// get player, ignore creative mode:
		final Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE) return;

		// make sure item in hand is the creation item:
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

		// check for player shop spawn:

		String playerName = player.getName();
		if (action == Action.RIGHT_CLICK_AIR) {
			if (player.isSneaking()) {
				// cycle shop objects:
				plugin.getShopObjectTypeRegistry().selectNext(player);
			} else {
				// cycle shopkeeper types:
				plugin.getShopTypeRegistry().selectNext(player);
			}
		} else if (action == Action.RIGHT_CLICK_BLOCK) {
			Block block = event.getClickedBlock();
			Block selectedChest = plugin.getSelectedChest(player);
			// validate old selected chest:
			if (selectedChest != null && !Utils.isChest(selectedChest.getType())) {
				plugin.selectChest(player, null);
				selectedChest = null;
			}

			if (Utils.isChest(block.getType()) && !block.equals(selectedChest)) {
				// chest selection:

				// check if the chest was recently placed:
				if (Settings.requireChestRecentlyPlaced && !plugin.wasRecentlyPlaced(player, block)) {
					// chest was not recently placed:
					Utils.sendMessage(player, Settings.msgChestNotPlaced);
				} else {
					boolean chestAccessDenied = (event.useInteractedBlock() == Result.DENY);
					if (chestAccessDenied) {
						// making sure that the chest access is really denied, and that the event
						// is not cancelled because of denying usage with the item in hand:
						player.setItemInHand(null);
						TestPlayerInteractEvent fakeInteractEvent = new TestPlayerInteractEvent(player, event.getAction(), null, block, event.getBlockFace());
						Bukkit.getPluginManager().callEvent(fakeInteractEvent);
						chestAccessDenied = (fakeInteractEvent.useInteractedBlock() == Result.DENY);

						// resetting item in hand:
						player.setItemInHand(inHand);
					}

					if (chestAccessDenied) {
						Log.debug("Right-click on chest prevented, player " + player.getName() + " at " + block.getLocation().toString());
					} else {
						// select chest:
						plugin.selectChest(player, block);
						Utils.sendMessage(player, Settings.msgSelectedChest);
					}
				}

				event.setCancelled(true);
			} else if (selectedChest != null) {
				assert Utils.isChest(selectedChest.getType()); // we have checked that above
				// placing player shop:

				// check for too far:
				if (!selectedChest.getWorld().getUID().equals(block.getWorld().getUID()) || (int) selectedChest.getLocation().distanceSquared(block.getLocation()) > (Settings.maxChestDistance * Settings.maxChestDistance)) {
					Utils.sendMessage(player, Settings.msgChestTooFar);
					// TODO maybe deny normal usage
				} else {
					// get shop type:
					ShopType<?> shopType = plugin.getShopTypeRegistry().getSelection(player);
					// get shop object type:
					ShopObjectType objType = plugin.getShopObjectTypeRegistry().getSelection(player);

					BlockFace blockFace = event.getBlockFace();
					// TODO move object type specific stuff into the object type instead
					if (shopType != null && objType != null && !(objType == DefaultShopObjectTypes.SIGN && !Utils.isWallSignFace(blockFace))) {
						// create player shopkeeper:
						Block spawnBlock = event.getClickedBlock().getRelative(blockFace);
						if (spawnBlock.getType() == Material.AIR) {
							ShopCreationData creationData = new ShopCreationData(player, shopType, selectedChest, spawnBlock.getLocation(), objType);
							Shopkeeper shopkeeper = plugin.createNewPlayerShopkeeper(creationData);
							if (shopkeeper != null) {
								// creation was successful:
								// reset selected chest:
								plugin.selectChest(player, null);

								// perform special setup:
								if (objType == DefaultShopObjectTypes.SIGN) {
									// set sign:
									// TODO maybe also allow non-wall signs?
									spawnBlock.setType(Material.WALL_SIGN);
									Sign signState = (Sign) spawnBlock.getState();
									((Attachable) signState.getData()).setFacingDirection(blockFace);
									signState.setLine(0, Settings.signShopFirstLine);
									signState.setLine(2, playerName);
									signState.update();
								}

								// manually remove creation item:
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
						} else {
							// TODO maybe deny normal usage and inform player that something didn't work
						}
					} else {
						// TODO maybe deny normal usage and inform player that something didn't work
					}
				}
			} else {
				// clicked a location without a chest selected:
				Utils.sendMessage(player, Settings.msgMustSelectChest);
				// TODO maybe deny normal usage
			}
		}

		// TODO maybe always prevent normal usage, also for cases in which shop creation fails because of some reason
		// and instead optionally allow normal usage when crouching? Or, if normal usage is not denied, allow shop creation only when crouching?

		// prevent regular usage (do this last because otherwise the canceling can interfere with logic above):
		// TODO are there items which would require canceling the event for left clicks or physical interaction as well?
		if (Settings.preventShopCreationItemRegularUsage && !Utils.hasPermission(player, ShopkeepersAPI.BYPASS_PERMISSION)) {
			Log.debug("Preventing normal shop creation item usage");
			event.setCancelled(true);
		}
	}
}