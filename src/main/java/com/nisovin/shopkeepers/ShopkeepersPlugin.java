package com.nisovin.shopkeepers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.shopkeepers.events.*;
import com.nisovin.shopkeepers.pluginhandlers.*;
import com.nisovin.shopkeepers.shopobjects.*;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityShop;
import com.nisovin.shopkeepers.shoptypes.*;
import com.nisovin.shopkeepers.ui.UITypeRegistry;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;
import com.nisovin.shopkeepers.abstractTypes.SelectableTypeRegistry;
import com.nisovin.shopkeepers.compat.NMSManager;

public class ShopkeepersPlugin extends JavaPlugin implements ShopkeepersAPI {

	private static ShopkeepersPlugin plugin;

	public static ShopkeepersPlugin getInstance() {
		return plugin;
	}

	// shop types manager:
	private final SelectableTypeRegistry<ShopType> shopTypesManager = new SelectableTypeRegistry<ShopType>() {

		@Override
		protected String getTypeName() {
			return "shop type";
		}

		@Override
		public boolean canBeSelected(Player player, ShopType type) {
			// TODO This currently skips the admin shop type. Maybe included the admin shop types here for players
			// which are admins, because there /could/ be different types of admin shops in the future (?)
			return super.canBeSelected(player, type) && type.isPlayerShopType();
		}
	};

	// shop object types manager:
	private final SelectableTypeRegistry<ShopObjectType> shopObjectTypesManager = new SelectableTypeRegistry<ShopObjectType>() {

		@Override
		protected String getTypeName() {
			return "shop object type";
		}
	};

	// ui managers:
	private final UITypeRegistry uiRegistry = new UITypeRegistry();

	// shops:
	private final Map<String, List<Shopkeeper>> allShopkeepersByChunk = new HashMap<String, List<Shopkeeper>>();
	private final Map<String, Shopkeeper> activeShopkeepers = new HashMap<String, Shopkeeper>();

	private final Map<String, Shopkeeper> naming = Collections.synchronizedMap(new HashMap<String, Shopkeeper>());
	private final Map<String, List<String>> recentlyPlacedChests = new HashMap<String, List<String>>();
	private final Map<String, Block> selectedChest = new HashMap<String, Block>();

	// saving:
	private boolean dirty = false;
	private int chunkLoadSaveTask = -1;

	// listeners:
	private CreatureForceSpawnListener creatureForceSpawnListener = null;

	@Override
	public void onEnable() {
		plugin = this;

		// clear registers:
		this.shopTypesManager.clearAll();
		this.shopObjectTypesManager.clearAll();
		this.uiRegistry.clearAll();

		// register static stuff:
		this.shopTypesManager.registerAll(DefaultShopTypes.getAll());
		this.shopObjectTypesManager.registerAll(DefaultShopObjectTypes.getAll());
		this.uiRegistry.registerAll(DefaultUIs.getAll());

		// try to load suitable NMS code
		NMSManager.load(this);
		if (NMSManager.getProvider() == null) {
			plugin.getLogger().severe("Incompatible server version: Shopkeepers cannot be enabled.");
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
		if (Settings.loadConfiguration(config)) {
			// if values were missing -> add those to the file and save it
			saveConfig();
		}
		Log.setDebug(config.getBoolean("debug", false));

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

		// process additional permissions
		String[] perms = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : perms) {
			if (Bukkit.getPluginManager().getPermission("shopkeeper.maxshops." + perm) == null) {
				Bukkit.getPluginManager().addPermission(new Permission("shopkeeper.maxshops." + perm, PermissionDefault.FALSE));
			}
		}

		// inform ui registry:
		this.uiRegistry.onEnable(this);

		// register events

		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new PlayerJoinListener(), this);
		pm.registerEvents(new ShopNamingListener(this), this);
		pm.registerEvents(new ChestListener(this), this);
		pm.registerEvents(new CreateListener(this), this);
		pm.registerEvents(new VillagerInteractionListener(this), this);
		pm.registerEvents(new LivingEntityShopListener(this), this);
		if (Settings.enableSignShops) {
			pm.registerEvents(new BlockShopListener(this), this);
		}

		if (Settings.blockVillagerSpawns) {
			pm.registerEvents(new BlockVillagerSpawnListener(), this);
		}

		if (Settings.protectChests) {
			pm.registerEvents(new ChestProtectListener(this), this);
		}
		if (Settings.deleteShopkeeperOnBreakChest) {
			pm.registerEvents(new RemoveShopOnChestBreakListener(this), this);
		}

		// register force-creature-spawn event:
		if (Settings.bypassSpawnBlocking) {
			this.creatureForceSpawnListener = new CreatureForceSpawnListener();
			Bukkit.getPluginManager().registerEvents(creatureForceSpawnListener, this);
		}

		// register command handler:
		CommandManager commandManager = new CommandManager(this);
		this.getCommand("shopkeeper").setExecutor(commandManager);

		// load shopkeeper saved data
		this.load();

		// spawn villagers in loaded chunks
		for (World world : Bukkit.getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks()) {
				this.loadShopkeepersInChunk(chunk);
			}
		}

		// start teleporter
		Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
			public void run() {
				List<Shopkeeper> readd = new ArrayList<Shopkeeper>();
				Iterator<Map.Entry<String, Shopkeeper>> iter = activeShopkeepers.entrySet().iterator();
				while (iter.hasNext()) {
					Shopkeeper shopkeeper = iter.next().getValue();
					boolean update = shopkeeper.teleport();
					if (update) {
						// if the shopkeeper had to be respawned it's shop id changed:
						// this removes the entry which was stored with the old shop id and later adds back the shopkeeper with it's new id
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
		}, 200, 200); // 10 seconds

		// start verifier
		if (Settings.enableSpawnVerifier) {
			Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
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
										Log.debug("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
									}
								}
							}
						}
					}
					if (count > 0) {
						Log.debug("Spawn verifier: " + count + " shopkeepers respawned");
					}
				}
			}, 600, 1200); // 30,60 seconds
		}

		// start saver
		if (!Settings.saveInstantly) {
			Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
				public void run() {
					if (dirty) {
						saveReal();
						dirty = false;
					}
				}
			}, 6000, 6000); // 5 minutes
		}

		// let's update the shopkeepers for all online players:
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.updateShopkeepersForPlayer(player);
		}

	}

	@Override
	public void onDisable() {
		if (this.dirty) {
			this.saveReal();
			this.dirty = false;
		}

		// close all open windows:
		this.uiRegistry.closeAll();

		for (Shopkeeper shopkeeper : this.activeShopkeepers.values()) {
			shopkeeper.despawn();
		}
		this.activeShopkeepers.clear();
		this.allShopkeepersByChunk.clear();

		this.shopTypesManager.clearAllSelections();
		this.shopObjectTypesManager.clearAllSelections();

		this.selectedChest.clear();

		// clear all types of registers:
		this.shopTypesManager.clearAll();
		this.shopObjectTypesManager.clearAll();
		this.uiRegistry.clearAll();

		HandlerList.unregisterAll(this);
		Bukkit.getScheduler().cancelTasks(this);

		plugin = null;
	}

	/**
	 * Reloads the plugin.
	 */
	public void reload() {
		this.onDisable();
		this.onEnable();
	}

	void onPlayerQuit(Player player) {
		String playerName = player.getName();
		this.shopTypesManager.clearSelection(player);
		this.shopObjectTypesManager.clearSelection(player);
		this.uiRegistry.onQuit(player);

		this.selectedChest.remove(playerName);
		this.recentlyPlacedChests.remove(playerName);
		this.naming.remove(playerName);
	}

	// bypassing creature blocking plugins ('region protection' plugins):
	public void forceCreatureSpawn(Location location, EntityType entityType) {
		if (creatureForceSpawnListener != null && Settings.bypassSpawnBlocking) {
			creatureForceSpawnListener.forceCreatureSpawn(location, entityType);
		}
	}

	// UI

	public UITypeRegistry getUIRegistry() {
		return this.uiRegistry;
	}

	// SHOP TYPES

	public SelectableTypeRegistry<ShopType> getShopTypeRegistry() {
		return this.shopTypesManager;
	}

	// SHOP OBJECT TYPES

	public SelectableTypeRegistry<ShopObjectType> getShopObjectTypeRegistry() {
		return this.shopObjectTypesManager;
	}

	// RECENTLY PLACED CHESTS

	void onChestPlacement(Player player, Block chest) {
		assert player != null && chest != null && chest.getType() == Material.CHEST;
		String playerName = player.getName();
		List<String> recentlyPlaced = this.recentlyPlacedChests.get(playerName);
		if (recentlyPlaced == null) {
			recentlyPlaced = new LinkedList<String>();
			this.recentlyPlacedChests.put(playerName, recentlyPlaced);
		}
		recentlyPlaced.add(chest.getWorld().getName() + "," + chest.getX() + "," + chest.getY() + "," + chest.getZ());
		if (recentlyPlaced.size() > 5) {
			recentlyPlaced.remove(0);
		}
	}

	public boolean wasRecentlyPlaced(Player player, Block chest) {
		assert player != null && chest != null && chest.getType() == Material.CHEST;
		String playerName = player.getName();
		List<String> recentlyPlaced = this.recentlyPlacedChests.get(playerName);
		return recentlyPlaced != null && recentlyPlaced.contains(chest.getWorld().getName() + "," + chest.getX() + "," + chest.getY() + "," + chest.getZ());
	}

	// SELECTED CHEST

	void selectChest(Player player, Block chest) {
		assert player != null;
		String playerName = player.getName();
		if (chest == null) this.selectedChest.remove(playerName);
		else {
			assert chest.getType() == Material.CHEST;
			this.selectedChest.put(playerName, chest);
		}
	}

	public Block getSelectedChest(Player player) {
		assert player != null;
		return this.selectedChest.get(player.getName());
	}

	// SHOPKEEPER NAMING

	void onNaming(Player player, Shopkeeper shopkeeper) {
		assert player != null && shopkeeper != null;
		this.naming.put(player.getName(), shopkeeper);
	}

	Shopkeeper getCurrentlyNamedShopkeeper(Player player) {
		assert player != null;
		return this.naming.get(player.getName());
	}

	boolean isNaming(Player player) {
		assert player != null;
		return this.getCurrentlyNamedShopkeeper(player) != null;
	}

	Shopkeeper endNaming(Player player) {
		assert player != null;
		return this.naming.remove(player.getName());
	}

	// SHOPKEEPER MEMORY STORAGE

	// this needs to be called right after a new shopkeeper was created..
	void registerShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		// assert !this.isRegistered(shopkeeper);

		// add default trading handler, if none is provided:
		if (shopkeeper.getUIHandler(DefaultUIs.TRADING_WINDOW) == null) {
			shopkeeper.registerUIHandler(new TradingHandler(DefaultUIs.TRADING_WINDOW, shopkeeper));
		}

		// add to chunk list
		List<Shopkeeper> list = this.allShopkeepersByChunk.get(shopkeeper.getChunkId());
		if (list == null) {
			list = new ArrayList<Shopkeeper>();
			this.allShopkeepersByChunk.put(shopkeeper.getChunkId(), list);
		}
		list.add(shopkeeper);

		if (!shopkeeper.needsSpawning()) this.activeShopkeepers.put(shopkeeper.getId(), shopkeeper);
		if (!shopkeeper.isActive() && this.isChunkLoaded(shopkeeper.getChunkId())) {
			boolean spawned = shopkeeper.spawn();
			if (spawned) {
				activeShopkeepers.put(shopkeeper.getId(), shopkeeper);
			} else {
				Log.debug("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
			}
		}
	}

	@Override
	public Shopkeeper getShopkeeperByEntityId(int entityId) {
		return this.activeShopkeepers.get("entity" + entityId);
	}

	@Override
	public Shopkeeper getShopkeeperByBlock(Block block) {
		if (block == null) return null;
		return this.activeShopkeepers.get("block" + block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ());
	}

	@Override
	public boolean isShopkeeper(Entity entity) {
		if (entity == null) return false;
		return this.getShopkeeperByEntityId(entity.getEntityId()) != null;
	}

	/*
	 * Shopkeeper getActiveShopkeeper(String shopId) {
	 * return this.activeShopkeepers.get(shopId);
	 * }
	 */

	Collection<List<Shopkeeper>> getAllShopkeepersByChunks() {
		return this.allShopkeepersByChunk.values();
	}

	Collection<Shopkeeper> getActiveShopkeepers() {
		return this.activeShopkeepers.values();
	}

	@Override
	public List<Shopkeeper> getShopkeepersInChunk(String world, int x, int z) {
		return this.allShopkeepersByChunk.get(world + "," + x + "," + z);
	}

	boolean isChestProtected(Player player, Block block) {
		for (Shopkeeper shopkeeper : this.activeShopkeepers.values()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper pshop = (PlayerShopkeeper) shopkeeper;
				if ((player == null || !pshop.isOwner(player)) && pshop.usesChest(block)) {
					return true;
				}
			}
		}
		return false;
	}

	List<PlayerShopkeeper> getShopkeeperOwnersOfChest(Block block) {
		List<PlayerShopkeeper> owners = new ArrayList<PlayerShopkeeper>();
		for (Shopkeeper shopkeeper : this.activeShopkeepers.values()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper pshop = (PlayerShopkeeper) shopkeeper;
				if (pshop.usesChest(block)) {
					owners.add(pshop);
				}
			}
		}
		return owners;
	}

	// LOADING/UNLOADING/REMOVAL

	private void activateShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		if (!shopkeeper.isActive() && shopkeeper.needsSpawning()) {
			Shopkeeper oldShopkeeper = this.activeShopkeepers.get(shopkeeper.getId());
			if (Log.isDebug() && oldShopkeeper != null && oldShopkeeper.getShopObject() instanceof LivingEntityShop) {
				LivingEntityShop oldLivingShop = (LivingEntityShop) oldShopkeeper.getShopObject();
				LivingEntity oldEntity = oldLivingShop.getEntity();
				Log.debug("Old, active shopkeeper was found (unloading probably has been skipped earlier): "
						+ (oldEntity == null ? "null" : (oldEntity.getUniqueId() + " | " + (oldEntity.isDead() ? "dead | " : "alive | ")
								+ (oldEntity.isValid() ? "valid" : "invalid"))));
			}
			boolean spawned = shopkeeper.spawn();
			if (spawned) {
				this.activeShopkeepers.put(shopkeeper.getId(), shopkeeper);
			} else {
				Log.warning("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
			}
		}
	}

	private void deactivateShopkeeper(Shopkeeper shopkeeper, boolean closeWindows) {
		String shopId = shopkeeper.getId();
		if (closeWindows) shopkeeper.closeAllOpenWindows();
		this.activeShopkeepers.remove(shopId);
		shopkeeper.despawn();
	}

	public void deleteShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		this.deactivateShopkeeper(shopkeeper, true);
		shopkeeper.onDeletion();
		this.allShopkeepersByChunk.get(shopkeeper.getChunkId()).remove(shopkeeper);
	}

	void loadShopkeepersInChunk(Chunk chunk) {
		assert chunk != null;
		List<Shopkeeper> shopkeepers = this.allShopkeepersByChunk.get(chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ());
		if (shopkeepers != null) {
			Log.debug("Loading " + shopkeepers.size() + " shopkeepers in chunk " + chunk.getX() + "," + chunk.getZ());
			for (Shopkeeper shopkeeper : shopkeepers) {
				this.activateShopkeeper(shopkeeper);
			}
			// save
			this.dirty = true;
			if (Settings.saveInstantly) {
				if (chunkLoadSaveTask < 0) {
					chunkLoadSaveTask = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						public void run() {
							if (dirty) {
								saveReal();
								dirty = false;
							}
							chunkLoadSaveTask = -1;
						}
					}, 600).getTaskId();
				}
			}
		}
	}

	void unloadShopkeepersInChunk(Chunk chunk) {
		assert chunk != null;
		List<Shopkeeper> shopkeepers = this.getShopkeepersInChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
		if (shopkeepers != null) {
			Log.debug("Unloading " + shopkeepers.size() + " shopkeepers in chunk " + chunk.getX() + "," + chunk.getZ());
			for (Shopkeeper shopkeeper : shopkeepers) {
				// skip sign shops: those are meant to not be removed and stay 'active' all the time currently
				if (!shopkeeper.needsSpawning()) continue;
				this.deactivateShopkeeper(shopkeeper, false);
			}
		}
	}

	void loadShopkeepersInWorld(World world) {
		assert world != null;
		for (Chunk chunk : world.getLoadedChunks()) {
			this.loadShopkeepersInChunk(chunk);
		}
	}

	void unloadShopkeepersInWorld(World world) {
		assert world != null;
		String worldName = world.getName();
		Iterator<Shopkeeper> iter = this.activeShopkeepers.values().iterator();
		int count = 0;
		while (iter.hasNext()) {
			Shopkeeper shopkeeper = iter.next();
			if (shopkeeper.getWorldName().equals(worldName)) {
				shopkeeper.despawn();
				iter.remove();
				count++;
			}
		}
		Log.debug("Unloaded " + count + " shopkeepers in world " + worldName);
	}

	private boolean isChunkLoaded(String chunkStr) {
		String[] chunkData = chunkStr.split(",");
		World world = Bukkit.getServer().getWorld(chunkData[0]);
		if (world != null) {
			int x = Integer.parseInt(chunkData[1]);
			int z = Integer.parseInt(chunkData[2]);
			return world.isChunkLoaded(x, z);
		}
		return false;
	}

	// SHOPKEEPER CREATION:

	@Override
	public Shopkeeper createNewAdminShopkeeper(ShopCreationData creationData) {
		if (creationData == null || creationData.location == null || creationData.objectType == null) return null;
		if (creationData.shopType == null) creationData.shopType = DefaultShopTypes.ADMIN;
		else if (creationData.shopType.isPlayerShopType()) return null; // we are expecting an admin shop type here..
		// create the shopkeeper (and spawn it)
		Shopkeeper shopkeeper = creationData.shopType.createShopkeeper(creationData);
		if (shopkeeper != null) {
			this.save();
			Utils.sendMessage(creationData.creator, creationData.shopType.getCreatedMessage());

			// run event
			Bukkit.getPluginManager().callEvent(new ShopkeeperCreatedEvent(creationData.creator, shopkeeper));
		} else {
			// TODO send informative message here?
		}
		return shopkeeper;
	}

	@Override
	public Shopkeeper createNewPlayerShopkeeper(ShopCreationData creationData) {
		if (creationData == null || creationData.shopType == null || creationData.objectType == null
				|| creationData.creator == null || creationData.location == null) {
			return null;
		}

		// check worldguard
		if (Settings.enableWorldGuardRestrictions) {
			if (!WorldGuardHandler.canBuild(creationData.creator, creationData.location)) {
				Utils.sendMessage(creationData.creator, Settings.msgShopCreateFail);
				return null;
			}
		}

		// check towny
		if (Settings.enableTownyRestrictions) {
			if (!TownyHandler.isCommercialArea(creationData.location)) {
				Utils.sendMessage(creationData.creator, Settings.msgShopCreateFail);
				return null;
			}
		}

		int maxShops = Settings.maxShopsPerPlayer;
		String[] maxShopsPermOptions = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : maxShopsPermOptions) {
			if (creationData.creator.hasPermission("shopkeeper.maxshops." + perm)) {
				maxShops = Integer.parseInt(perm);
			}
		}

		// call event
		CreatePlayerShopkeeperEvent event = new CreatePlayerShopkeeperEvent(creationData, maxShops);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return null;
		} else {
			creationData.location = event.getSpawnLocation();
			creationData.shopType = event.getType();
			maxShops = event.getMaxShopsForPlayer();
		}

		// count owned shops
		if (maxShops > 0) {
			int count = 0;
			for (List<Shopkeeper> list : this.allShopkeepersByChunk.values()) {
				for (Shopkeeper shopkeeper : list) {
					if (shopkeeper instanceof PlayerShopkeeper && ((PlayerShopkeeper) shopkeeper).isOwner(creationData.creator)) {
						count++;
					}
				}
			}
			if (count >= maxShops) {
				Utils.sendMessage(creationData.creator, Settings.msgTooManyShops);
				return null;
			}
		}

		// create the shopkeeper
		Shopkeeper shopkeeper = creationData.shopType.createShopkeeper(creationData);

		// spawn and save the shopkeeper
		if (shopkeeper != null) {
			this.save();
			Utils.sendMessage(creationData.creator, creationData.shopType.getCreatedMessage());
			// run event
			Bukkit.getPluginManager().callEvent(new ShopkeeperCreatedEvent(creationData.creator, shopkeeper));
		} else {
			// TODO print some 'creation fail' message here?
		}

		return shopkeeper;
	}

	// SHOPS LOADING AND SAVING

	private File getSaveFile() {
		return new File(this.getDataFolder(), "save.yml");
	}

	private void load() {
		File file = this.getSaveFile();
		if (!file.exists()) return;

		YamlConfiguration config = new YamlConfiguration();
		Scanner scanner = null;
		FileInputStream stream = null;
		try {
			if (Settings.fileEncoding != null && !Settings.fileEncoding.isEmpty()) {
				stream = new FileInputStream(file);
				scanner = new Scanner(stream, Settings.fileEncoding);
				scanner.useDelimiter("\\A");
				if (!scanner.hasNext()) return; // file is completely empty -> no shopkeeper data is available
				String data = scanner.next();
				config.loadFromString(data);
			} else {
				config.load(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		} finally {
			if (scanner != null) scanner.close();
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		Set<String> keys = config.getKeys(false);
		for (String key : keys) {
			ConfigurationSection section = config.getConfigurationSection(key);
			ShopType shopType = this.shopTypesManager.get(section.getString("type"));
			// unknown shop type
			if (shopType == null) {
				// git an owner entry? -> default to normal player shop type
				if (section.contains("owner")) {
					shopType = DefaultShopTypes.PLAYER_NORMAL;
				} else {
					continue; // no valid shop type given..
				}
			}
			Shopkeeper shopkeeper = shopType.loadShopkeeper(section);
			if (shopkeeper == null) {
				Log.debug("Failed to load shopkeeper: " + key); // TODO more informative debug message here?
				continue;
			}

			// check if shop is too old
			if (Settings.playerShopkeeperInactiveDays > 0 && shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
				UUID ownerUUID = playerShop.getOwnerUUID();
				// TODO: this potentially could freeze, but shouldn't be a big issue here as we are inside the load method which only gets called once per plugin load
				// TODO disabled this for now due to issues; also: as shopkeepers are now registered on creation we have to undo the registration again here..
				/*
				 * OfflinePlayer offlinePlayer = ownerUUID != null ? Bukkit.getOfflinePlayer(ownerUUID) : Bukkit.getOfflinePlayer(playerShop.getOwnerName());
				 * if (!offlinePlayer.hasPlayedBefore()) continue; // we definitely got the wrong OfflinePlayer
				 * long lastPlayed = offlinePlayer.getLastPlayed();
				 * if ((lastPlayed > 0) && ((System.currentTimeMillis() - lastPlayed) / 86400000 > Settings.playerShopkeeperInactiveDays)) {
				 * // shop is too old, don't load it
				 * plugin.getLogger().info("Shopkeeper owned by " + playerShop.getOwnerAsString() + " at " + shopkeeper.getPositionString() + " has been removed for owner inactivity");
				 * continue;
				 * }
				 */
			}

			// the shopkeeper already gets registered during creation
			// add to shopkeepers by chunk
			/*
			 * List<Shopkeeper> list = allShopkeepersByChunk.get(shopkeeper.getChunkId());
			 * if (list == null) {
			 * list = new ArrayList<Shopkeeper>();
			 * allShopkeepersByChunk.put(shopkeeper.getChunkId(), list);
			 * }
			 * list.add(shopkeeper);
			 * 
			 * // add to active shopkeepers if spawning not needed
			 * if (!shopkeeper.needsSpawning()) {
			 * activeShopkeepers.put(shopkeeper.getId(), shopkeeper);
			 * }
			 */
		}
	}

	public void save() {
		if (Settings.saveInstantly) {
			this.saveReal();
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

		File file = this.getSaveFile();
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
			Log.debug("Saved shopkeeper data");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// UUID <-> PLAYERNAME HANDLING

	// checks for missing owner uuids and updates owner names for the shopkeepers of the given player:
	void updateShopkeepersForPlayer(Player player) {
		boolean dirty = false;
		UUID playerUUID = player.getUniqueId();
		String playerName = player.getName();
		for (List<Shopkeeper> shopkeepers : this.allShopkeepersByChunk.values()) {
			for (Shopkeeper shopkeeper : shopkeepers) {
				if (shopkeeper instanceof PlayerShopkeeper) {
					PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
					UUID ownerUUID = playerShop.getOwnerUUID();
					String ownerName = playerShop.getOwnerName();

					if (ownerUUID != null) {
						if (playerUUID.equals(ownerUUID)) {
							if (!ownerName.equalsIgnoreCase(playerName)) {
								// update the stored name, because the player must have changed it:
								playerShop.setOwner(player);
								dirty = true;
							}
						}
					} else {
						// we have no uuid for the owner of this shop yet, let's identify the owner by name:
						if (playerName.equalsIgnoreCase(ownerName)) {
							// let's store this player's uuid:
							playerShop.setOwner(player);
							dirty = true;
						}
					}
				}
			}
		}

		if (dirty) {
			this.save();
		}
	}
}