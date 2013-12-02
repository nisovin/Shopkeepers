package com.nisovin.shopkeepers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.shopkeepers.events.*;
import com.nisovin.shopkeepers.pluginhandlers.*;
import com.nisovin.shopkeepers.shopobjects.*;
import com.nisovin.shopkeepers.shoptypes.*;
import com.nisovin.shopkeepers.volatilecode.*;

public class ShopkeepersPlugin extends JavaPlugin {

	static ShopkeepersPlugin plugin;
	static VolatileCodeHandle volatileCodeHandle = null;

	private boolean debug = false;
	
	Map<String, List<Shopkeeper>> allShopkeepersByChunk = new HashMap<String, List<Shopkeeper>>();
	Map<String, Shopkeeper> activeShopkeepers = new HashMap<String, Shopkeeper>();
	Map<String, String> editing = new HashMap<String, String>();
	Map<String, String> naming = Collections.synchronizedMap(new HashMap<String, String>());
	Map<String, String> purchasing = new HashMap<String, String>();
	Map<String, String> hiring = new HashMap<String, String>();
	Map<String, List<String>> recentlyPlacedChests = new HashMap<String, List<String>>();
	Map<String, ShopkeeperType> selectedShopType = new HashMap<String, ShopkeeperType>();
	Map<String, ShopObjectType> selectedShopObjectType = new HashMap<String, ShopObjectType>();
	Map<String, Block> selectedChest = new HashMap<String, Block>();
	
	private boolean dirty = false;
	private int chunkLoadSaveTask = -1;
		
	BlockFace[] chestProtectFaces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
	BlockFace[] hopperProtectFaces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
	
	@Override
	public void onEnable() {
		plugin = this;
				
		// load volatile code handler
		try {
			Class.forName("net.minecraft.server.v1_6_R2.MinecraftServer");
			volatileCodeHandle = new VolatileCode_1_6_R2();
		} catch (ClassNotFoundException e_1_6_r2) {
			try {
				Class.forName("net.minecraft.server.v1_6_R1.MinecraftServer");
				volatileCodeHandle = new VolatileCode_1_6_R1();
			} catch (ClassNotFoundException e_1_6_r1) {
				try {
					volatileCodeHandle = new VolatileCode_Unknown();
					getLogger().warning("Potentially incompatible server version: Shopkeepers is running in 'compatibility mode'.");
				} catch (Exception e_u) {
				}
			}
		}
		if (volatileCodeHandle == null) {
			getLogger().severe("Incompatible server version: Shopkeepers cannot be enabled.");
			this.setEnabled(false);
			return;
		}
		
		// get config
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			saveDefaultConfig();
		}
		reloadConfig();
		Configuration config = getConfig();
		Settings.loadConfiguration(config);
		debug = config.getBoolean("debug", debug);

		// get lang config
		String lang = config.getString("language", "en");
		File langFile = new File(getDataFolder(), "language-" + lang + ".yml");
		if (!langFile.exists() && this.getResource("language-" + lang + ".yml") != null) {
			saveResource("language-" + lang + ".yml", false);
		}
		if (langFile.exists()) {
			try {
				YamlConfiguration langConfig = new YamlConfiguration();
				langConfig.load(langFile);
				Settings.loadLanguageConfiguration(langConfig);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// load shopkeeper saved data
		load();
		
		// spawn villagers in loaded chunks
		for (World world : Bukkit.getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks()) {
				loadShopkeepersInChunk(chunk);
			}
		}
		
		// process additional perms
		String[] perms = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : perms) {
			if (Bukkit.getPluginManager().getPermission("shopkeeper.maxshops." + perm) == null) {
				Bukkit.getPluginManager().addPermission(new Permission("shopkeeper.maxshops." + perm, PermissionDefault.FALSE));
			}
		}
		
		// register events
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new ShopListener(this), this);
		pm.registerEvents(new CreateListener(this), this);
		if (Settings.enableVillagerShops) {
			pm.registerEvents(new VillagerListener(this), this);
		}
		if (Settings.enableSignShops) {
			pm.registerEvents(new BlockListener(this), this);
		}
		if (Settings.enableWitchShops) {
			pm.registerEvents(new WitchListener(this), this);
		}
		if (Settings.enableCreeperShops) {
			pm.registerEvents(new CreeperListener(this), this);
		}
		if (Settings.blockVillagerSpawns) {
			pm.registerEvents(new BlockSpawnListener(), this);
		}
		if (Settings.protectChests) {
			pm.registerEvents(new ChestProtectListener(this), this);
		}
		if (Settings.deleteShopkeeperOnBreakChest) {
			pm.registerEvents(new ChestBreakListener(this), this);
		}
		
		// start teleporter
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				List<Shopkeeper> readd = new ArrayList<Shopkeeper>();
				Iterator<Map.Entry<String, Shopkeeper>> iter = activeShopkeepers.entrySet().iterator();
				while (iter.hasNext()) {
					Shopkeeper shopkeeper = iter.next().getValue();
					boolean update = shopkeeper.teleport();
					if (update) {
						readd.add(shopkeeper);
						iter.remove();
					}
				}
				for (Shopkeeper shopkeeper : readd) {
					if (shopkeeper.isActive()) {
						activeShopkeepers.put(shopkeeper.getId(), shopkeeper);
					}
				}
			}
		}, 200, 200);
		
		// start verifier
		if (Settings.enableSpawnVerifier) {
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				public void run() {
					int count = 0;
					for (String chunkStr : allShopkeepersByChunk.keySet()) {
						if (isChunkLoaded(chunkStr)) {
							List<Shopkeeper> shopkeepers = allShopkeepersByChunk.get(chunkStr);
							for (Shopkeeper shopkeeper : shopkeepers) {
								if (!shopkeeper.isActive()) {
									boolean spawned = shopkeeper.spawn();
									if (spawned) {
										activeShopkeepers.put(shopkeeper.getId(), shopkeeper);
										count++;
									} else {
										debug("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
									}
								}
							}
						}
					}
					if (count > 0) {
						debug("Spawn verifier: " + count + " shopkeepers respawned");
					}
				}
			}, 600, 1200);
		}
		
		// start saver
		if (!Settings.saveInstantly) {
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				public void run() {
					if (dirty) {
						saveReal();
						dirty = false;
					}
				}
			}, 6000, 6000);
		}
		
	}
	
	@Override
	public void onDisable() {
		if (dirty) {
			saveReal();
			dirty = false;
		}
		
		for (String playerName : editing.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
				player.closeInventory();
			}
		}
		editing.clear();
		
		for (String playerName : purchasing.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
				player.closeInventory();
			}
		}
		purchasing.clear();
		
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
			shopkeeper.remove();
		}
		activeShopkeepers.clear();
		allShopkeepersByChunk.clear();
		
		selectedShopType.clear();
		selectedShopObjectType.clear();
		selectedChest.clear();
		
		HandlerList.unregisterAll((Plugin)this);		
		Bukkit.getScheduler().cancelTasks(this);
		
		plugin = null;
	}
	
	/**
	 * Reloads the plugin.
	 */
	public void reload() {
		onDisable();
		onEnable();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("shopkeeper.reload")) {
			// reload command
			reload();
			sender.sendMessage(ChatColor.GREEN + "Shopkeepers plugin reloaded!");
			return true;
		} else if (args.length == 1 && args[0].equalsIgnoreCase("debug") && sender.isOp()) {
			// toggle debug command
			debug = !debug;
			sender.sendMessage(ChatColor.GREEN + "Debug mode " + (debug?"enabled":"disabled"));
			return true;
			
		} else if (args.length == 1 && args[0].equals("check") && sender.isOp()) {
			for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
				if (shopkeeper.isActive()) {
					Location loc = shopkeeper.getActualLocation();
					sender.sendMessage("Shopkeeper at " + shopkeeper.getPositionString() + ": active (" + (loc != null ? loc.toString() : "maybe not?!?") + ")");
				} else {
					sender.sendMessage("Shopkeeper at " + shopkeeper.getPositionString() + ": INACTIVE!");
				}
			}
			return true;
			
		} else if (sender instanceof Player) {
			Player player = (Player)sender;
			Block block = player.getTargetBlock(null, 10);
			
			// transfer ownership
			if (args.length == 2 && args[0].equalsIgnoreCase("transfer") && player.hasPermission("shopkeeper.transfer")) {
				Player newOwner = Bukkit.getPlayer(args[1]);
				if (newOwner == null) {
					sender.sendMessage("No player found");
					return true;
				}
				if (block.getType() != Material.CHEST) {
					sender.sendMessage("Must target chest");
					return true;
				}
				List<PlayerShopkeeper> shopkeepers = getShopkeeperOwnersOfChest(block);
				if (shopkeepers.size() == 0) {
					sender.sendMessage("No shopkeepers use that chest");
					return true;
				}
				if (!player.isOp() && !player.hasPermission("shopkeeper.bypass")) {
					for (PlayerShopkeeper shopkeeper : shopkeepers) {
						if (!shopkeeper.getOwner().equalsIgnoreCase(player.getName())) {
							sender.sendMessage("Not your shopkeeper");
							return true;
						}
					}
				}
				for (PlayerShopkeeper shopkeeper : shopkeepers) {
					shopkeeper.setOwner(newOwner.getName());
				}
				save();
				sender.sendMessage("New owner set to " + newOwner.getName());
				return true;
			}
			
			// set for hire
			if (args.length == 1 && args[0].equalsIgnoreCase("setforhire") && player.hasPermission("shopkeeper.setforhire")) {
				if (block.getType() != Material.CHEST) {
					sender.sendMessage("Must target chest");
					return true;
				}
				List<PlayerShopkeeper> shopkeepers = getShopkeeperOwnersOfChest(block);
				if (shopkeepers.size() == 0) {
					sender.sendMessage("No shopkeepers use that chest");
					return true;
				}
				if (!player.isOp() && !player.hasPermission("shopkeeper.bypass")) {
					for (PlayerShopkeeper shopkeeper : shopkeepers) {
						if (!shopkeeper.getOwner().equalsIgnoreCase(player.getName())) {
							sender.sendMessage("Not your shopkeeper");
							return true;
						}
					}
				}
				ItemStack hireCost = player.getItemInHand();
				if (hireCost == null || hireCost.getType() == Material.AIR || hireCost.getAmount() == 0) {
					sender.sendMessage("Must hold hire cost in hand");
					return true;
				}
				for (PlayerShopkeeper shopkeeper : shopkeepers) {
					shopkeeper.setForHire(true, hireCost.clone());
				}
				save();
				sender.sendMessage("Shopkeeper set for hire");
				return true;
			}
						
			// get the spawn location for the shopkeeper
			if (block != null && block.getType() != Material.AIR) {
				if (Settings.createPlayerShopWithCommand && block.getType() == Material.CHEST) {
					// check if already a chest
					if (isChestProtected(null, block)) {
						return true;
					}
					// check for recently placed
					if (Settings.requireChestRecentlyPlaced) {
						List<String> list = plugin.recentlyPlacedChests.get(player.getName());
						if (list == null || !list.contains(block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ())) {
							sendMessage(player, Settings.msgChestNotPlaced);
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
					// create the player shopkeeper
					ShopkeeperType shopType = ShopkeeperType.next(player, null);
					ShopObjectType shopObjType = ShopObjectType.next(player, null);
					if (args != null && args.length > 0) {
						if (args.length >= 1) {
							if ((args[0].toLowerCase().startsWith("norm") || args[0].toLowerCase().startsWith("sell"))) {
								shopType = ShopkeeperType.PLAYER_NORMAL;
							} else if (args[0].toLowerCase().startsWith("book")) {
								shopType = ShopkeeperType.PLAYER_BOOK;
							} else if (args[0].toLowerCase().startsWith("buy")) {
								shopType = ShopkeeperType.PLAYER_BUY;
							} else if (args[0].toLowerCase().startsWith("trad")) {
								shopType = ShopkeeperType.PLAYER_TRADE;
							} else if (args[0].toLowerCase().equals("villager")) {
								shopObjType = ShopObjectType.VILLAGER;
							} else if (args[0].toLowerCase().equals("sign")) {
								shopObjType = ShopObjectType.SIGN;
							}
						}
						if (args.length >= 2) {
							if (args[1].equalsIgnoreCase("villager")) {
								shopObjType = ShopObjectType.VILLAGER;
							} else if (args[1].equalsIgnoreCase("sign")) {
								shopObjType = ShopObjectType.SIGN;
							}
						}
						if (shopType != null && !shopType.hasPermission(player)) {
							shopType = null;
						}
						if (shopObjType != null && !shopObjType.hasPermission(player)) {
							shopObjType = null;
						}
					}
					if (shopType != null) {
						Shopkeeper shopkeeper = createNewPlayerShopkeeper(player, block, block.getLocation().add(0, 1.5, 0), shopType, shopObjType.createObject());
						if (shopkeeper != null) {
							sendCreatedMessage(player, shopType);
						}
					}
				} else if (player.hasPermission("shopkeeper.admin")) {
					// create the admin shopkeeper
					ShopObjectType shopObjType = ShopObjectType.VILLAGER;
					Location loc = block.getLocation().add(0, 1.5, 0);
					if (args.length > 0) {
						if (args[0].equals("sign")) {
							shopObjType = ShopObjectType.SIGN;
							loc = block.getLocation();
						} else if (args[0].equals("witch")) {
							shopObjType = ShopObjectType.WITCH;
						} else if (args[0].equals("creeper")) {
							shopObjType = ShopObjectType.CREEPER;
						}
					}
					Shopkeeper shopkeeper = createNewAdminShopkeeper(loc, shopObjType.createObject());
					if (shopkeeper != null) {
						sendMessage(player, Settings.msgAdminShopCreated);
						
						// run event
						Bukkit.getPluginManager().callEvent(new ShopkeeperCreatedEvent(player, shopkeeper));
					}
				}
			} else {
				sendMessage(player, Settings.msgShopCreateFail);
			}
			
			return true;
		} else {
			sender.sendMessage("You must be a player to create a shopkeeper.");
			sender.sendMessage("Use 'shopkeeper reload' to reload the plugin.");
			return true;
		}
	}
	
	/**
	 * Creates a new admin shopkeeper and spawns it into the world.
	 * @param location the block location the shopkeeper should spawn
	 * @param profession the shopkeeper's profession, a number from 0 to 5
	 * @return the shopkeeper created
	 */
	public Shopkeeper createNewAdminShopkeeper(Location location, ShopObject shopObject) {
		// create the shopkeeper (and spawn it)
		Shopkeeper shopkeeper = new AdminShopkeeper(location, shopObject);
		shopkeeper.spawn();
		activeShopkeepers.put(shopkeeper.getId(), shopkeeper);
		addShopkeeper(shopkeeper);
		
		return shopkeeper;
	}

	/**
	 * Creates a new player-based shopkeeper and spawns it into the world.
	 * @param player the player who created the shopkeeper
	 * @param chest the backing chest for the shop
	 * @param location the block location the shopkeeper should spawn
	 * @param profession the shopkeeper's profession, a number from 0 to 5
	 * @param type the player shop type (0=normal, 1=book, 2=buy)
	 * @return the shopkeeper created
	 */
	public Shopkeeper createNewPlayerShopkeeper(Player player, Block chest, Location location, ShopkeeperType shopType, ShopObject shopObject) {
		if (shopType == null || shopObject == null) {
			return null;
		}
		
		// check worldguard
		if (Settings.enableWorldGuardRestrictions) {
			if (!WorldGuardHandler.canBuild(player, location)) {
				plugin.sendMessage(player, Settings.msgShopCreateFail);
				return null;
			}
		}
		
		// check towny
		if (Settings.enableTownyRestrictions) {
			if (!TownyHandler.isCommercialArea(location)) {
				plugin.sendMessage(player, Settings.msgShopCreateFail);
				return null;
			}
		}
		
		int maxShops = Settings.maxShopsPerPlayer;
		String[] maxShopsPermOptions = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : maxShopsPermOptions) {
			if (player.hasPermission("shopkeeper.maxshops." + perm)) {
				maxShops = Integer.parseInt(perm);
			}
		}
		
		// call event
		CreatePlayerShopkeeperEvent event = new CreatePlayerShopkeeperEvent(player, chest, location, shopType, maxShops);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return null;
		} else {
			location = event.getSpawnLocation();
			shopType = event.getType();
			maxShops = event.getMaxShopsForPlayer();
		}
		
		// count owned shops
		if (maxShops > 0) {
			int count = 0;
			for (List<Shopkeeper> list : allShopkeepersByChunk.values()) {
				for (Shopkeeper shopkeeper : list) {
					if (shopkeeper instanceof PlayerShopkeeper && ((PlayerShopkeeper)shopkeeper).getOwner().equalsIgnoreCase(player.getName())) {
						count++;
					}
				}
			}
			if (count >= maxShops) {
				sendMessage(player, Settings.msgTooManyShops);
				return null;
			}
		}
		
		// create the shopkeeper
		Shopkeeper shopkeeper = null;
		if (shopType == ShopkeeperType.PLAYER_NORMAL) {
			shopkeeper = new NormalPlayerShopkeeper(player, chest, location, shopObject);
		} else if (shopType == ShopkeeperType.PLAYER_BOOK) {
			shopkeeper = new WrittenBookPlayerShopkeeper(player, chest, location, shopObject);
		} else if (shopType == ShopkeeperType.PLAYER_BUY) {
			shopkeeper = new BuyingPlayerShopkeeper(player, chest, location, shopObject);
		} else if (shopType == ShopkeeperType.PLAYER_TRADE) {
			shopkeeper = new TradingPlayerShopkeeper(player, chest, location, shopObject);
		}

		// spawn and save the shopkeeper
		if (shopkeeper != null) {
			shopkeeper.spawn();
			activeShopkeepers.put(shopkeeper.getId(), shopkeeper);
			addShopkeeper(shopkeeper);
		}
		
		// run event
		Bukkit.getPluginManager().callEvent(new ShopkeeperCreatedEvent(player, shopkeeper));
		
		return shopkeeper;
	}
	
	/**
	 * Gets the shopkeeper by the villager's entity id.
	 * @param entityId the entity id of the villager
	 * @return the Shopkeeper, or null if the enitity with the given id is not a shopkeeper
	 */
	public Shopkeeper getShopkeeperByEntityId(int entityId) {
		return activeShopkeepers.get(entityId);
	}
	
	/**
	 * Gets all shopkeepers from a given chunk. Returns null if there are no shopkeepers in that chunk.
	 * @param world the world
	 * @param x chunk x-coordinate
	 * @param z chunk z-coordinate
	 * @return a list of shopkeepers, or null if there are none
	 */
	public List<Shopkeeper> getShopkeepersInChunk(String world, int x, int z) {
		return allShopkeepersByChunk.get(world + "," + x + "," + z);
	}
	
	/**
	 * Checks if a given entity is a Shopkeeper.
	 * @param entity the entity to check
	 * @return whether the entity is a Shopkeeper
	 */
	public boolean isShopkeeper(Entity entity) {
		return activeShopkeepers.containsKey("entity" + entity.getEntityId());
	}
	
	public boolean isShopkeeperEditorWindow(Inventory inventory) {
		return inventory.getTitle().equals(Settings.editorTitle);
	}
	
	public boolean isShopkeeperHireWindow(Inventory inventory) {
		return inventory.getTitle().equals(Settings.forHireTitle);
	}

	void addShopkeeper(Shopkeeper shopkeeper) {
		// add to chunk list
		List<Shopkeeper> list = allShopkeepersByChunk.get(shopkeeper.getChunk());
		if (list == null) {
			list = new ArrayList<Shopkeeper>();
			allShopkeepersByChunk.put(shopkeeper.getChunk(), list);
		}
		list.add(shopkeeper);
		// save all data
		save();
	}

	boolean sendCreatedMessage(Player player, ShopkeeperType shopType) {
		if (shopType == ShopkeeperType.PLAYER_NORMAL) {
			plugin.sendMessage(player, Settings.msgPlayerShopCreated);
			return true;
		} else if (shopType == ShopkeeperType.PLAYER_BOOK) {
			plugin.sendMessage(player, Settings.msgBookShopCreated);
			return true;
		} else if (shopType == ShopkeeperType.PLAYER_BUY) {
			plugin.sendMessage(player, Settings.msgBuyShopCreated);
			return true;
		} else if (shopType == ShopkeeperType.PLAYER_TRADE) {
			plugin.sendMessage(player, Settings.msgTradeShopCreated);
			return true;
		}
		return false;
	}
	
	void handleShopkeeperInteraction(Player player, Shopkeeper shopkeeper) {
		if (shopkeeper != null && player.isSneaking()) {
			// modifying a shopkeeper
			ShopkeepersPlugin.debug("  Opening editor window...");
			boolean isEditing = shopkeeper.onEdit(player);
			if (isEditing) {
				ShopkeepersPlugin.debug("  Editor window opened");
				editing.put(player.getName(), shopkeeper.getId());
			} else {
				ShopkeepersPlugin.debug("  Editor window NOT opened");
			}
		} else if (shopkeeper != null) {
			if (shopkeeper instanceof PlayerShopkeeper && ((PlayerShopkeeper)shopkeeper).isForHire() && player.hasPermission("shopkeeper.hire")) {
				// show hire interface
				openHireWindow((PlayerShopkeeper)shopkeeper, player);
				hiring.put(player.getName(), shopkeeper.getId());
			} else {
				// trading with shopkeeper
				ShopkeepersPlugin.debug("  Opening trade window...");
				OpenTradeEvent evt = new OpenTradeEvent(player, shopkeeper);
				Bukkit.getPluginManager().callEvent(evt);
				if (evt.isCancelled()) {
					ShopkeepersPlugin.debug("  Trade cancelled by another plugin");
					return;
				}
				// open trade window
				openTradeWindow(shopkeeper, player);
				purchasing.put(player.getName(), shopkeeper.getId());
				ShopkeepersPlugin.debug("  Trade window opened");
			}
		}
	}
	
	void closeTradingForShopkeeper(final String id) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				Iterator<String> editors = editing.keySet().iterator();
				while (editors.hasNext()) {
					String name = editors.next();
					if (editing.get(name).equals(id)) {
						editors.remove();
						Player player = Bukkit.getPlayerExact(name);
						if (player != null) {
							player.closeInventory();
						}
					}
				}
				Iterator<String> purchasers = purchasing.keySet().iterator();
				while (purchasers.hasNext()) {
					String name = purchasers.next();
					if (purchasing.get(name).equals(id)) {
						purchasers.remove();
						Player player = Bukkit.getPlayerExact(name);
						if (player != null) {
							player.closeInventory();
						}
					}
				}
				Iterator<String> hirers = hiring.keySet().iterator();
				while (hirers.hasNext()) {
					String name = hirers.next();
					if (hiring.get(name).equals(id)) {
						hirers.remove();
						Player player = Bukkit.getPlayerExact(name);
						if (player != null) {
							player.closeInventory();
						}
					}
				}
			}
		}, 1);
	}
	
	void closeInventory(final HumanEntity player) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(ShopkeepersPlugin.plugin, new Runnable() {
			public void run() {
				player.closeInventory();
			}
		}, 1);
	}
	
	boolean openTradeWindow(Shopkeeper shopkeeper, Player player) {
		return volatileCodeHandle.openTradeWindow(shopkeeper, player);
	}
	
	void openHireWindow(PlayerShopkeeper shopkeeper, Player player) {
		Inventory inv = Bukkit.createInventory(player, 9, ChatColor.translateAlternateColorCodes('&', Settings.forHireTitle));
		
		ItemStack item = new ItemStack(Settings.hireItem, 0);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Settings.msgButtonHire));
		item.setItemMeta(meta);
		inv.setItem(2, item);
		inv.setItem(6, item);
		
		ItemStack hireCost = shopkeeper.getHireCost();
		if (hireCost == null) return;
		inv.setItem(4, hireCost);
		
		player.openInventory(inv);
	}
	
	boolean isChestProtected(Player player, Block block) {
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper pshop = (PlayerShopkeeper)shopkeeper;
				if ((player == null || !pshop.getOwner().equalsIgnoreCase(player.getName())) && pshop.usesChest(block)) {
					return true;
				}
			}
		}
		return false;
	}
	
	List<PlayerShopkeeper> getShopkeeperOwnersOfChest(Block block) {
		List<PlayerShopkeeper> owners = new ArrayList<PlayerShopkeeper>();
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper pshop = (PlayerShopkeeper)shopkeeper;
				if (pshop.usesChest(block)) {
					owners.add(pshop);
				}
			}
		}
		return owners;
	}
	
	void sendMessage(Player player, String message) {
		message = ChatColor.translateAlternateColorCodes('&', message);
		String[] msgs = message.split("\n");
		for (String msg : msgs) {
			player.sendMessage(msg);
		}
	}
	
	void loadShopkeepersInChunk(Chunk chunk) {
		List<Shopkeeper> shopkeepers = allShopkeepersByChunk.get(chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ());
		if (shopkeepers != null) {
			debug("Loading " + shopkeepers.size() + " shopkeepers in chunk " + chunk.getX() + "," + chunk.getZ());
			for (Shopkeeper shopkeeper : shopkeepers) {
				if (!shopkeeper.isActive() && shopkeeper.needsSpawned()) {
					boolean spawned = shopkeeper.spawn();
					if (spawned) {
						activeShopkeepers.put(shopkeeper.getId(), shopkeeper);
					} else {
						getLogger().warning("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
					}
				}
			}
			// save
			dirty = true;
			if (Settings.saveInstantly) {
				if (chunkLoadSaveTask < 0) {
					chunkLoadSaveTask = Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							if (dirty) {
								saveReal();
								dirty = false;
							}
							chunkLoadSaveTask = -1;
						}
					}, 600);
				}
			}
		}
	}
	
	private boolean isChunkLoaded(String chunkStr) {
		String[] chunkData = chunkStr.split(",");
		World w = getServer().getWorld(chunkData[0]);
		if (w != null) {
			int x = Integer.parseInt(chunkData[1]);
			int z = Integer.parseInt(chunkData[2]);
			return w.isChunkLoaded(x, z);
		}
		return false;
	}
	
	private void load() {
		File file = new File(getDataFolder(), "save.yml");
		if (!file.exists()) return;
		
		YamlConfiguration config = new YamlConfiguration();
		try {
			if (Settings.fileEncoding != null && !Settings.fileEncoding.isEmpty()) {
				FileInputStream stream = new FileInputStream(file);
				String data = new Scanner(stream, Settings.fileEncoding).useDelimiter("\\A").next();
				config.loadFromString(data);
				stream.close();
			} else {
				config.load(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		Set<String> keys = config.getKeys(false);
		for (String key : keys) {
			ConfigurationSection section = config.getConfigurationSection(key);
			Shopkeeper shopkeeper = null;
			String type = section.getString("type", "");
			if (type.equals("book")) {
				shopkeeper = new WrittenBookPlayerShopkeeper(section);
			} else if (type.equals("buy")) {
				shopkeeper = new BuyingPlayerShopkeeper(section);
			} else if (type.equals("trade")) {
				shopkeeper = new TradingPlayerShopkeeper(section);
			} else if (type.equals("player") || section.contains("owner")) {
				shopkeeper = new NormalPlayerShopkeeper(section);
			} else {
				shopkeeper = new AdminShopkeeper(section);
			}
			if (shopkeeper != null) {
				// check if shop is too old
				if (Settings.playerShopkeeperInactiveDays > 0 && shopkeeper instanceof PlayerShopkeeper) {
					String owner = ((PlayerShopkeeper)shopkeeper).getOwner();
					OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);
					long lastPlayed = offlinePlayer.getLastPlayed();
					if ((lastPlayed > 0) && ((System.currentTimeMillis() - lastPlayed) / 86400000 > Settings.playerShopkeeperInactiveDays)) {
						// shop is too old, don't load it
						getLogger().info("Shopkeeper owned by " + owner + " at " + shopkeeper.getPositionString() + " has been removed for owner inactivity");
						continue;
					}
				}
				
				// add to shopkeepers by chunk
				List<Shopkeeper> list = allShopkeepersByChunk.get(shopkeeper.getChunk());
				if (list == null) {
					list = new ArrayList<Shopkeeper>();
					allShopkeepersByChunk.put(shopkeeper.getChunk(), list);
				}
				list.add(shopkeeper);
				
				// add to active shopkeepers if spawning not needed
				if (!shopkeeper.needsSpawned()) {
					activeShopkeepers.put(shopkeeper.getId(), shopkeeper);
				}
			}
		}
	}
	
	void save() {
		if (Settings.saveInstantly) {
			saveReal();
		} else {
			dirty = true;
		}
	}
	
	private void saveReal() {
		YamlConfiguration config = new YamlConfiguration();
		int counter = 0;
		for (List<Shopkeeper> shopkeepers : allShopkeepersByChunk.values()) {
			for (Shopkeeper shopkeeper : shopkeepers) {
				ConfigurationSection section = config.createSection(counter + "");
				shopkeeper.save(section);
				counter++;
			}
		}
		
		File file = new File(getDataFolder(), "save.yml");
		if (file.exists()) {
			file.delete();
		}
		try {
			if (Settings.fileEncoding != null && !Settings.fileEncoding.isEmpty()) {
				PrintWriter writer = new PrintWriter(file, Settings.fileEncoding);
				writer.write(config.saveToString());
				writer.close();
			} else {
				config.save(file);
			}
			debug("Saved shopkeeper data");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static VolatileCodeHandle getVolatileCode() {
		return volatileCodeHandle;
	}
	
	public static ShopkeepersPlugin getInstance() {
		return plugin;
	}
	
	public static void debug(String message) {
		if (plugin.debug) {
			plugin.getLogger().info(message);
		}
	}
	
	public static void warning(String message) {
		plugin.getLogger().warning(message);
	}

}
