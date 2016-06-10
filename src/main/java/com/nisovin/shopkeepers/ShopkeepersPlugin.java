package com.nisovin.shopkeepers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
import org.bukkit.scheduler.BukkitRunnable;

import com.nisovin.shopkeepers.abstractTypes.SelectableTypeRegistry;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.events.CreatePlayerShopkeeperEvent;
import com.nisovin.shopkeepers.events.ShopkeeperCreatedEvent;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.pluginhandlers.TownyHandler;
import com.nisovin.shopkeepers.pluginhandlers.WorldGuardHandler;
import com.nisovin.shopkeepers.shopobjects.CitizensShop;
import com.nisovin.shopkeepers.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopobjects.SignShop;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityShop;
import com.nisovin.shopkeepers.shoptypes.DefaultShopTypes;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;
import com.nisovin.shopkeepers.ui.UIManager;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;

public class ShopkeepersPlugin extends JavaPlugin implements ShopkeepersAPI {

	private static ShopkeepersPlugin plugin;

	public static ShopkeepersPlugin getInstance() {
		return plugin;
	}

	// shop types manager:
	private final SelectableTypeRegistry<ShopType<?>> shopTypesManager = new SelectableTypeRegistry<ShopType<?>>() {

		@Override
		protected String getTypeName() {
			return "shop type";
		}

		@Override
		public boolean canBeSelected(Player player, ShopType<?> type) {
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

	// default shop and shop object types:
	private DefaultShopTypes defaultShopTypes;
	private DefaultShopObjectTypes defaultShopObjectTypes;

	// ui manager:
	private final UIManager uiManager = new UIManager();

	// all shopkeepers:
	private final Map<UUID, Shopkeeper> shopkeepersById = new LinkedHashMap<UUID, Shopkeeper>();
	private final Collection<Shopkeeper> allShopkeepersView = Collections.unmodifiableCollection(shopkeepersById.values());
	private int nextShopSessionId = 1;
	private final Map<Integer, Shopkeeper> shopkeepersBySessionId = new LinkedHashMap<Integer, Shopkeeper>();
	private final Map<ChunkData, List<Shopkeeper>> shopkeepersByChunk = new HashMap<ChunkData, List<Shopkeeper>>();
	private final Map<String, Shopkeeper> activeShopkeepers = new HashMap<String, Shopkeeper>(); // TODO remove this (?)
	private final Collection<Shopkeeper> activeShopkeepersView = Collections.unmodifiableCollection(activeShopkeepers.values());

	private final Map<String, ConfirmEntry> confirming = new HashMap<String, ConfirmEntry>();
	private final Map<String, Shopkeeper> naming = Collections.synchronizedMap(new HashMap<String, Shopkeeper>());
	private final Map<String, List<String>> recentlyPlacedChests = new HashMap<String, List<String>>();
	private final Map<String, Block> selectedChest = new HashMap<String, Block>();

	// saving:
	// flag to (temporary) turn off saving
	private boolean skipSaving = false;
	private boolean dirty = false;
	private int chunkLoadSaveTask = -1;
	// keeps track about certain stats and information during a save, gets reused
	private final SaveInfo saveInfo = new SaveInfo();
	// the task which performs file io during a save
	private int saveIOTask = -1;
	// determines if there was another saveReal()-request while another saveIOTask was still in progress
	private boolean saveRealAgain = false;

	// listeners:
	private CreatureForceSpawnListener creatureForceSpawnListener = null;
	private SignShopListener signShopListener = null;

	@Override
	public void onEnable() {
		plugin = this;
		skipSaving = false;

		// try to load suitable NMS code:
		NMSManager.load(this);
		if (NMSManager.getProvider() == null) {
			Log.severe("Incompatible server version: Shopkeepers cannot be enabled.");
			this.setEnabled(false);
			return;
		}

		// load config:
		File file = new File(this.getDataFolder(), "config.yml");
		if (!file.exists()) {
			this.saveDefaultConfig();
		}
		this.reloadConfig();
		Configuration config = this.getConfig();
		if (Settings.loadConfiguration(config)) {
			// if values were missing -> add those to the file and save it
			this.saveConfig();
		}
		Log.setDebug(config.getBoolean("debug", false));

		// load lang config:
		String lang = config.getString("language", "en");
		File langFile = new File(this.getDataFolder(), "language-" + lang + ".yml");
		if (!langFile.exists() && this.getResource("language-" + lang + ".yml") != null) {
			this.saveResource("language-" + lang + ".yml", false);
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

		// initialize default shop and shop object types (after config has been loaded):
		defaultShopTypes = new DefaultShopTypes();
		defaultShopObjectTypes = new DefaultShopObjectTypes();

		// register default stuff:
		shopTypesManager.registerAll(defaultShopTypes.getAllShopTypes());
		shopObjectTypesManager.registerAll(defaultShopObjectTypes.getAllObjectTypes());
		uiManager.registerAll(DefaultUIs.getAllUITypes());

		// inform ui manager (registers ui event handlers):
		uiManager.onEnable(this);

		// register events
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new PluginListener(), this);
		pm.registerEvents(new WorldListener(this), this);
		pm.registerEvents(new PlayerJoinQuitListener(this), this);
		pm.registerEvents(new ShopNamingListener(this), this);
		pm.registerEvents(new ChestListener(this), this);
		pm.registerEvents(new CreateListener(this), this);
		pm.registerEvents(new VillagerInteractionListener(this), this);
		pm.registerEvents(new LivingEntityShopListener(this), this);

		if (Settings.enableSignShops) {
			this.signShopListener = new SignShopListener(this);
			pm.registerEvents(signShopListener, this);
		}

		// enable citizens handler:
		CitizensHandler.enable();

		if (Settings.blockVillagerSpawns) {
			pm.registerEvents(new BlockVillagerSpawnListener(), this);
		}

		if (Settings.protectChests) {
			pm.registerEvents(new ChestProtectListener(this), this);
		}
		if (Settings.deleteShopkeeperOnBreakChest) {
			pm.registerEvents(new RemoveShopOnChestBreakListener(this), this);
		}

		// register force-creature-spawn event handler:
		if (Settings.bypassSpawnBlocking) {
			creatureForceSpawnListener = new CreatureForceSpawnListener();
			Bukkit.getPluginManager().registerEvents(creatureForceSpawnListener, this);
		}

		// register command handler:
		CommandManager commandManager = new CommandManager(this);
		this.getCommand("shopkeeper").setExecutor(commandManager);

		// load shopkeeper saved data:
		if (!this.load()) {
			// detected issue during loading, disable plugin without saving, to prevent loss of shopkeeper data:
			Log.severe("Detected an issue during loading of the shopkeeper data! Disabling plugin!");
			skipSaving = true;
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// activate (spawn) shopkeepers in loaded chunks:
		for (World world : Bukkit.getWorlds()) {
			this.loadShopkeepersInWorld(world);
		}

		Bukkit.getScheduler().runTaskLater(this, new Runnable() {

			@Override
			public void run() {
				// remove invalid citizens shopkeepers:
				CitizensHandler.removeInvalidCitizensShopkeepers();
				// remove inactive player shopkeepers:
				removeInactivePlayerShops();
			}
		}, 5L);

		// start teleporter task:
		Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
			public void run() {
				List<Shopkeeper> readd = new ArrayList<Shopkeeper>();
				Iterator<Map.Entry<String, Shopkeeper>> iter = activeShopkeepers.entrySet().iterator();
				while (iter.hasNext()) {
					Shopkeeper shopkeeper = iter.next().getValue();
					boolean update = shopkeeper.check();
					if (update) {
						// if the shopkeeper had to be respawned it's shop id changed:
						// this removes the entry which was stored with the old shop id and later adds back the
						// shopkeeper with it's new id
						readd.add(shopkeeper);
						iter.remove();
					}
				}
				if (!readd.isEmpty()) {
					for (Shopkeeper shopkeeper : readd) {
						if (shopkeeper.isActive()) {
							_activateShopkeeper(shopkeeper);
						}
					}

					// shopkeepers might have been respawned, request save:
					save();
				}
			}
		}, 200, 200); // 10 seconds

		// start verifier task:
		if (Settings.enableSpawnVerifier) {
			Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
				public void run() {
					int count = 0;
					for (Entry<ChunkData, List<Shopkeeper>> chunkEntry : shopkeepersByChunk.entrySet()) {
						ChunkData chunk = chunkEntry.getKey();
						if (chunk.isChunkLoaded()) {
							List<Shopkeeper> shopkeepers = chunkEntry.getValue();
							for (Shopkeeper shopkeeper : shopkeepers) {
								if (shopkeeper.needsSpawning() && !shopkeeper.isActive()) {
									// deactivate by old object id:
									_deactivateShopkeeper(shopkeeper);

									boolean spawned = shopkeeper.spawn();
									if (spawned) {
										// activate with new object id:
										_activateShopkeeper(shopkeeper);
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
						save();
					}
				}
			}, 600, 1200); // 30,60 seconds
		}

		// start save task:
		if (!Settings.saveInstantly) {
			Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
				public void run() {
					if (dirty) {
						saveReal();
					}
				}
			}, 6000, 6000); // 5 minutes
		}

		// let's update the shopkeepers for all already online players:
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (Utils.isNPC(player)) continue;
			this.updateShopkeepersForPlayer(player.getUniqueId(), player.getName());
		}
	}

	@Override
	public void onDisable() {
		// close all open windows:
		uiManager.closeAll();

		// despawn shopkeepers:
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
			shopkeeper.despawn();
		}

		// disable citizens handler:
		CitizensHandler.disable();

		// save:
		if (dirty) {
			this.saveReal(false); // not async here
		}

		activeShopkeepers.clear();
		shopkeepersByChunk.clear();
		shopkeepersById.clear();
		shopkeepersBySessionId.clear();
		nextShopSessionId = 1;

		shopTypesManager.clearAllSelections();
		shopObjectTypesManager.clearAllSelections();

		confirming.clear();
		naming.clear();
		selectedChest.clear();

		// clear all types of registers:
		shopTypesManager.clearAll();
		shopObjectTypesManager.clearAll();
		uiManager.clearAll();

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
		shopTypesManager.clearSelection(player);
		shopObjectTypesManager.clearSelection(player);
		uiManager.onInventoryClose(player);

		selectedChest.remove(playerName);
		recentlyPlacedChests.remove(playerName);
		naming.remove(playerName);
		this.endConfirmation(player);
	}

	// bypassing creature blocking plugins ('region protection' plugins):
	public void forceCreatureSpawn(Location location, EntityType entityType) {
		if (creatureForceSpawnListener != null && Settings.bypassSpawnBlocking) {
			creatureForceSpawnListener.forceCreatureSpawn(location, entityType);
		}
	}

	public void cancelNextBlockPhysics(Location location) {
		if (signShopListener != null) {
			signShopListener.cancelNextBlockPhysics(location);
		}
	}

	// UI

	public UIManager getUIManager() {
		return uiManager;
	}

	// SHOP TYPES

	public SelectableTypeRegistry<ShopType<?>> getShopTypeRegistry() {
		return shopTypesManager;
	}

	public DefaultShopTypes getDefaultShopTypes() {
		return defaultShopTypes;
	}

	// SHOP OBJECT TYPES

	public SelectableTypeRegistry<ShopObjectType> getShopObjectTypeRegistry() {
		return shopObjectTypesManager;
	}

	public DefaultShopObjectTypes getDefaultShopObjectTypes() {
		return defaultShopObjectTypes;
	}

	/**
	 * Gets the default shop object type.
	 * 
	 * <p>
	 * Usually this will be the villager entity shop object type.<br>
	 * However, there are no guarantees that this might not get changed or be configurable in the future.
	 * </p>
	 * 
	 * @return the default shop object type
	 */
	public ShopObjectType getDefaultShopObjectType() {
		// default: villager entity shop object type:
		return this.getDefaultShopObjectTypes().getLivingEntityObjectTypes().getObjectType(EntityType.VILLAGER);
	}

	// RECENTLY PLACED CHESTS

	void onChestPlacement(Player player, Block chest) {
		assert player != null && chest != null && Utils.isChest(chest.getType());
		String playerName = player.getName();
		List<String> recentlyPlaced = recentlyPlacedChests.get(playerName);
		if (recentlyPlaced == null) {
			recentlyPlaced = new LinkedList<String>();
			recentlyPlacedChests.put(playerName, recentlyPlaced);
		}
		recentlyPlaced.add(chest.getWorld().getName() + "," + chest.getX() + "," + chest.getY() + "," + chest.getZ());
		if (recentlyPlaced.size() > 5) {
			recentlyPlaced.remove(0);
		}
	}

	public boolean wasRecentlyPlaced(Player player, Block chest) {
		assert player != null && chest != null && Utils.isChest(chest.getType());
		String playerName = player.getName();
		List<String> recentlyPlaced = recentlyPlacedChests.get(playerName);
		return recentlyPlaced != null && recentlyPlaced.contains(chest.getWorld().getName() + "," + chest.getX() + "," + chest.getY() + "," + chest.getZ());
	}

	// SELECTED CHEST

	void selectChest(Player player, Block chest) {
		assert player != null;
		String playerName = player.getName();
		if (chest == null) selectedChest.remove(playerName);
		else {
			assert Utils.isChest(chest.getType());
			selectedChest.put(playerName, chest);
		}
	}

	public Block getSelectedChest(Player player) {
		assert player != null;
		return selectedChest.get(player.getName());
	}

	// COMMAND CONFIRMING

	void waitForConfirm(final Player player, Runnable action, int delay) {
		assert player != null && delay > 0;
		int taskId = new BukkitRunnable() {

			@Override
			public void run() {
				endConfirmation(player);
				Utils.sendMessage(player, Settings.msgConfirmationExpired);
			}
		}.runTaskLater(this, delay).getTaskId();
		ConfirmEntry oldEntry = confirming.put(player.getName(), new ConfirmEntry(action, taskId));
		if (oldEntry != null) {
			// end old confirmation task:
			Bukkit.getScheduler().cancelTask(oldEntry.getTaskId());
		}
	}

	Runnable endConfirmation(Player player) {
		ConfirmEntry entry = confirming.remove(player.getName());
		if (entry != null) {
			// end confirmation task:
			Bukkit.getScheduler().cancelTask(entry.getTaskId());

			// return action:
			return entry.getAction();
		}
		return null;
	}

	void onConfirm(Player player) {
		assert player != null;
		Runnable action = this.endConfirmation(player);
		if (action != null) {
			// execute confirmed task:
			action.run();
		} else {
			Utils.sendMessage(player, Settings.msgNothingToConfirm);
		}
	}

	// SHOPKEEPER NAMING

	void onNaming(Player player, Shopkeeper shopkeeper) {
		assert player != null && shopkeeper != null;
		naming.put(player.getName(), shopkeeper);
	}

	Shopkeeper getCurrentlyNamedShopkeeper(Player player) {
		assert player != null;
		return naming.get(player.getName());
	}

	boolean isNaming(Player player) {
		assert player != null;
		return this.getCurrentlyNamedShopkeeper(player) != null;
	}

	Shopkeeper endNaming(Player player) {
		assert player != null;
		return naming.remove(player.getName());
	}

	// SHOPKEEPER MEMORY STORAGE

	private void addShopkeeperToChunk(Shopkeeper shopkeeper, ChunkData chunkData) {
		List<Shopkeeper> list = shopkeepersByChunk.get(chunkData);
		if (list == null) {
			list = new ArrayList<Shopkeeper>();
			shopkeepersByChunk.put(chunkData, list);
		}
		list.add(shopkeeper);
	}

	private void removeShopkeeperFromChunk(Shopkeeper shopkeeper, ChunkData chunkData) {
		List<Shopkeeper> byChunk = shopkeepersByChunk.get(chunkData);
		if (byChunk == null) return;
		if (byChunk.remove(shopkeeper) && byChunk.isEmpty()) {
			shopkeepersByChunk.remove(chunkData);
		}
	}

	// this needs to be called right after a new shopkeeper was created..
	void registerShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		// assert !this.isRegistered(shopkeeper);

		// add default trading handler, if none is provided:
		if (shopkeeper.getUIHandler(DefaultUIs.TRADING_WINDOW) == null) {
			shopkeeper.registerUIHandler(new TradingHandler(DefaultUIs.TRADING_WINDOW, shopkeeper));
		}

		// store by unique id:
		shopkeepersById.put(shopkeeper.getUniqueId(), shopkeeper);

		// assign session id:
		int shopSessionId = nextShopSessionId;
		nextShopSessionId++;
		shopkeepersBySessionId.put(shopSessionId, shopkeeper);

		// inform shopkeeper:
		shopkeeper.onRegistration(shopSessionId);

		// add shopkeeper to chunk:
		ChunkData chunkData = shopkeeper.getChunkData();
		this.addShopkeeperToChunk(shopkeeper, chunkData);

		// activate shopkeeper:
		if (!shopkeeper.needsSpawning()) {
			this._activateShopkeeper(shopkeeper);
		} else if (!shopkeeper.isActive() && chunkData.isChunkLoaded()) {
			boolean spawned = shopkeeper.spawn();
			if (spawned) {
				this._activateShopkeeper(shopkeeper);
			} else {
				Log.debug("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
			}
		}
	}

	@Override
	public Shopkeeper getShopkeeperByEntity(Entity entity) {
		if (entity == null) return null;
		Shopkeeper shopkeeper = activeShopkeepers.get(LivingEntityShop.getId(entity));
		if (shopkeeper != null) return shopkeeper;
		// check if this is a citizens npc shopkeeper:
		Integer npcId = CitizensHandler.getNPCId(entity);
		if (npcId == null) return null;
		return activeShopkeepers.get(CitizensShop.getId(npcId));
	}

	@Override
	public Shopkeeper getShopkeeper(UUID shopkeeperUUID) {
		return shopkeepersById.get(shopkeeperUUID);
	}

	@Override
	public Shopkeeper getShopkeeper(int shopkeeperSessionId) {
		return shopkeepersBySessionId.get(shopkeeperSessionId);
	}

	@Override
	public Shopkeeper getShopkeeperByName(String shopName) {
		if (shopName == null) return null;
		shopName = ChatColor.stripColor(shopName);
		for (Shopkeeper shopkeeper : this.getAllShopkeepers()) {
			if (shopkeeper.getName() != null && ChatColor.stripColor(shopkeeper.getName()).equalsIgnoreCase(shopName)) {
				return shopkeeper;
			}
		}
		return null;
	}

	@Override
	public Shopkeeper getShopkeeperByBlock(Block block) {
		if (block == null) return null;
		return activeShopkeepers.get(SignShop.getId(block));
	}

	public Shopkeeper getActiveShopkeeper(String objectId) {
		return activeShopkeepers.get(objectId);
	}

	@Override
	public boolean isShopkeeper(Entity entity) {
		return this.getShopkeeperByEntity(entity) != null;
	}

	@Override
	public Collection<Shopkeeper> getAllShopkeepers() {
		return allShopkeepersView;
	}

	@Override
	public Collection<List<Shopkeeper>> getAllShopkeepersByChunks() {
		return Collections.unmodifiableCollection(shopkeepersByChunk.values());
	}

	@Override
	public Collection<Shopkeeper> getActiveShopkeepers() {
		return activeShopkeepersView;
	}

	@Override
	public List<Shopkeeper> getShopkeepersInChunk(String worldName, int x, int z) {
		return this.getShopkeepersInChunk(new ChunkData(worldName, x, z));
	}

	@Override
	public List<Shopkeeper> getShopkeepersInChunk(ChunkData chunkData) {
		List<Shopkeeper> byChunk = shopkeepersByChunk.get(chunkData);
		if (byChunk == null) return null;
		return Collections.unmodifiableList(byChunk);
	}

	boolean isChestProtected(Player player, Block block) {
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
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
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
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

	// performs some validation before actually activating a shopkeeper:
	// returns false if some validation failed
	private boolean _activateShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		String objectId = shopkeeper.getObjectId();
		if (objectId == null) {
			// currently only null is considered invalid,
			// prints 'null' to log then:
			Log.warning("Detected shopkeeper with invalid object id: " + objectId);
			return false;
		} else if (activeShopkeepers.containsKey(objectId)) {
			Log.warning("Detected shopkeepers with duplicate object id: " + objectId);
			return false;
		} else {
			// activate shopkeeper:
			activeShopkeepers.put(objectId, shopkeeper);
			return true;
		}
	}

	private boolean _deactivateShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		String objectId = shopkeeper.getObjectId();
		if (activeShopkeepers.get(objectId) == shopkeeper) {
			activeShopkeepers.remove(objectId);
			return true;
		}
		return false;
	}

	private void activateShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		if (shopkeeper.needsSpawning() && !shopkeeper.isActive()) {
			// deactivate shopkeeper by old shop object id, in case there is one:
			if (this._deactivateShopkeeper(shopkeeper)) {
				if (Log.isDebug() && shopkeeper.getShopObject() instanceof LivingEntityShop) {
					LivingEntityShop livingShop = (LivingEntityShop) shopkeeper.getShopObject();
					LivingEntity oldEntity = livingShop.getEntity();
					Log.debug("Old, active shopkeeper was found (unloading probably has been skipped earlier): "
							+ (oldEntity == null ? "null" : (oldEntity.getUniqueId() + " | " + (oldEntity.isDead() ? "dead | " : "alive | ")
									+ (oldEntity.isValid() ? "valid" : "invalid"))));
				}
			}

			// spawn and activate:
			boolean spawned = shopkeeper.spawn();
			if (spawned) {
				// activate with new object id:
				this._activateShopkeeper(shopkeeper);
			} else {
				Log.warning("Failed to spawn shopkeeper at " + shopkeeper.getPositionString());
			}
		}
	}

	private void deactivateShopkeeper(Shopkeeper shopkeeper, boolean closeWindows) {
		assert shopkeeper != null;
		if (closeWindows) {
			// delayed closing of all open windows:
			shopkeeper.closeAllOpenWindows();
		}
		this._deactivateShopkeeper(shopkeeper);
		shopkeeper.despawn();
	}

	public void deleteShopkeeper(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		// deactivate shopkeeper:
		this.deactivateShopkeeper(shopkeeper, true);

		// inform shopkeeper:
		shopkeeper.onDeletion();

		// remove shopkeeper by id and session id:
		shopkeepersById.remove(shopkeeper.getUniqueId());
		shopkeepersBySessionId.remove(shopkeeper.getSessionId());

		// remove shopkeeper from chunk:
		ChunkData chunkData = shopkeeper.getChunkData();
		this.removeShopkeeperFromChunk(shopkeeper, chunkData);
	}

	public void onShopkeeperMove(Shopkeeper shopkeeper, ChunkData oldChunk) {
		assert oldChunk != null;
		ChunkData newChunk = shopkeeper.getChunkData();
		if (!oldChunk.equals(newChunk)) {
			// remove from old chunk:
			this.removeShopkeeperFromChunk(shopkeeper, oldChunk);

			// add to new chunk:
			this.addShopkeeperToChunk(shopkeeper, newChunk);
		}
	}

	/**
	 * 
	 * @param chunk
	 * @return the number of shops in the affected chunk
	 */
	int loadShopkeepersInChunk(Chunk chunk) {
		assert chunk != null;
		int affectedShops = 0;
		List<Shopkeeper> shopkeepers = shopkeepersByChunk.get(new ChunkData(chunk));
		if (shopkeepers != null) {
			affectedShops = shopkeepers.size();
			Log.debug("Loading " + affectedShops + " shopkeepers in chunk " + chunk.getX() + "," + chunk.getZ());
			for (Shopkeeper shopkeeper : shopkeepers) {
				// inform shopkeeper about chunk load:
				shopkeeper.onChunkLoad();

				// activate:
				this.activateShopkeeper(shopkeeper);
			}

			// save:
			dirty = true;
			if (Settings.saveInstantly) {
				if (chunkLoadSaveTask == -1) {
					chunkLoadSaveTask = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						public void run() {
							if (dirty) {
								saveReal();
							}
							chunkLoadSaveTask = -1;
						}
					}, 600).getTaskId();
				}
			}
		}
		return affectedShops;
	}

	/**
	 * Unloads all shopkeepers in the given chunk.
	 * 
	 * @param chunk
	 *            the chunk
	 * @return the number of affected shops
	 */
	int unloadShopkeepersInChunk(Chunk chunk) {
		assert chunk != null;
		int affectedShops = 0;
		List<Shopkeeper> shopkeepers = this.getShopkeepersInChunk(new ChunkData(chunk));
		if (shopkeepers != null) {
			affectedShops = shopkeepers.size();
			Log.debug("Unloading " + affectedShops + " shopkeepers in chunk " + chunk.getX() + "," + chunk.getZ());
			for (Shopkeeper shopkeeper : shopkeepers) {
				// inform shopkeeper about chunk unload:
				shopkeeper.onChunkUnload();

				// skip shopkeepers which are kept active all the time (ex. sign, citizens shops):
				if (!shopkeeper.needsSpawning()) continue;

				// deactivate:
				this.deactivateShopkeeper(shopkeeper, false);
			}
		}
		return affectedShops;
	}

	void loadShopkeepersInWorld(World world) {
		assert world != null;
		int affectedShops = 0;
		for (Chunk chunk : world.getLoadedChunks()) {
			affectedShops += this.loadShopkeepersInChunk(chunk);
		}
		Log.debug("Loaded " + affectedShops + " shopkeepers in world " + world.getName());
	}

	void unloadShopkeepersInWorld(World world) {
		assert world != null;
		int affectedShops = 0;
		for (Chunk chunk : world.getLoadedChunks()) {
			affectedShops += this.unloadShopkeepersInChunk(chunk);
		}
		Log.debug("Unloaded " + affectedShops + " shopkeepers in world " + world.getName());
	}

	// SHOPKEEPER CREATION:

	@Override
	public boolean hasCreatePermission(Player player) {
		if (player == null) return false;
		return shopTypesManager.getSelection(player) != null && shopObjectTypesManager.getSelection(player) != null;
	}

	@Override
	public Shopkeeper createNewAdminShopkeeper(ShopCreationData creationData) {
		try {
			if (creationData == null || creationData.spawnLocation == null || creationData.objectType == null) {
				throw new ShopkeeperCreateException("null");
			}
			if (creationData.shopType == null) {
				creationData.shopType = DefaultShopTypes.ADMIN();
			} else if (creationData.shopType.isPlayerShopType()) {
				// we are expecting an admin shop type here..
				throw new ShopkeeperCreateException("Expecting admin shop type, got player shop type!");
			}

			// create the shopkeeper (and spawn it):
			Shopkeeper shopkeeper = creationData.shopType.createShopkeeper(creationData);
			if (shopkeeper == null) {
				throw new ShopkeeperCreateException("ShopType returned null shopkeeper!");
			}
			assert shopkeeper != null;

			// save:
			this.save();

			// send creation message to creator:
			Utils.sendMessage(creationData.creator, creationData.shopType.getCreatedMessage());

			// run shopkeeper-created-event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperCreatedEvent(creationData.creator, shopkeeper));

			return shopkeeper;
		} catch (ShopkeeperCreateException e) {
			Log.warning("Couldn't create admin shopkeeper: " + e.getMessage());
			return null;
		}
	}

	@Override
	public Shopkeeper createNewPlayerShopkeeper(ShopCreationData creationData) {
		try {
			if (creationData == null || creationData.shopType == null || creationData.objectType == null
					|| creationData.creator == null || creationData.chest == null || creationData.spawnLocation == null) {
				throw new ShopkeeperCreateException("null");
			}

			// check if this chest is already used by some other shopkeeper:
			if (this.isChestProtected(null, creationData.chest)) {
				Utils.sendMessage(creationData.creator, Settings.msgShopCreateFail);
				return null;
			}

			// check worldguard:
			if (Settings.enableWorldGuardRestrictions) {
				if (!WorldGuardHandler.isShopAllowed(creationData.creator, creationData.spawnLocation)) {
					Utils.sendMessage(creationData.creator, Settings.msgShopCreateFail);
					return null;
				}
			}

			// check towny:
			if (Settings.enableTownyRestrictions) {
				if (!TownyHandler.isCommercialArea(creationData.spawnLocation)) {
					Utils.sendMessage(creationData.creator, Settings.msgShopCreateFail);
					return null;
				}
			}

			int maxShops = this.getMaxShops(creationData.creator);

			// call event:
			CreatePlayerShopkeeperEvent event = new CreatePlayerShopkeeperEvent(creationData, maxShops);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				Log.debug("CreatePlayerShopkeeperEvent was cancelled!");
				return null;
			} else {
				creationData.spawnLocation = event.getSpawnLocation();
				creationData.shopType = event.getType();
				maxShops = event.getMaxShopsForPlayer();
			}

			// count owned shops:
			if (maxShops > 0) {
				int count = this.countShopsOfPlayer(creationData.creator);
				if (count >= maxShops) {
					Utils.sendMessage(creationData.creator, Settings.msgTooManyShops);
					return null;
				}
			}

			// create and spawn the shopkeeper:
			Shopkeeper shopkeeper = creationData.shopType.createShopkeeper(creationData);
			if (shopkeeper == null) {
				throw new ShopkeeperCreateException("ShopType returned null shopkeeper!");
			}
			assert shopkeeper != null;

			// save:
			this.save();

			// send creation message to creator:
			Utils.sendMessage(creationData.creator, creationData.shopType.getCreatedMessage());

			// run shopkeeper-created-event
			Bukkit.getPluginManager().callEvent(new ShopkeeperCreatedEvent(creationData.creator, shopkeeper));

			return shopkeeper;
		} catch (ShopkeeperCreateException e) {
			Log.warning("Couldn't create player shopkeeper: " + e.getMessage());
			return null;
		}
	}

	public int countShopsOfPlayer(Player player) {
		int count = 0;
		for (Shopkeeper shopkeeper : shopkeepersById.values()) {
			if (shopkeeper instanceof PlayerShopkeeper && ((PlayerShopkeeper) shopkeeper).isOwner(player)) {
				count++;
			}
		}
		return count;
	}

	public int getMaxShops(Player player) {
		int maxShops = Settings.maxShopsPerPlayer;
		String[] maxShopsPermOptions = Settings.maxShopsPermOptions.replace(" ", "").split(",");
		for (String perm : maxShopsPermOptions) {
			if (Utils.hasPermission(player, "shopkeeper.maxshops." + perm)) {
				maxShops = Integer.parseInt(perm);
			}
		}
		return maxShops;
	}

	// INACTIVE SHOPS

	private void removeInactivePlayerShops() {
		if (Settings.playerShopkeeperInactiveDays <= 0) return;

		final Set<UUID> playerUUIDs = new HashSet<UUID>();
		for (Shopkeeper shopkeeper : shopkeepersById.values()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
				playerUUIDs.add(playerShop.getOwnerUUID());
			}
		}
		if (playerUUIDs.isEmpty()) {
			// no player shops found:
			return;
		}

		// fetch OfflinePlayers async:
		final int playerShopkeeperInactiveDays = Settings.playerShopkeeperInactiveDays;
		Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {

			@Override
			public void run() {
				final List<OfflinePlayer> inactivePlayers = new ArrayList<OfflinePlayer>(playerUUIDs.size());
				long now = System.currentTimeMillis();
				for (UUID uuid : playerUUIDs) {
					OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
					if (!offlinePlayer.hasPlayedBefore()) continue;

					long lastPlayed = offlinePlayer.getLastPlayed();
					if ((lastPlayed > 0) && ((now - lastPlayed) / 86400000 > playerShopkeeperInactiveDays)) {
						inactivePlayers.add(offlinePlayer);
					}
				}

				if (inactivePlayers.isEmpty()) {
					// no inactive players found:
					return;
				}

				// continue in main thread:
				Bukkit.getScheduler().runTask(ShopkeepersPlugin.this, new Runnable() {

					@Override
					public void run() {
						List<PlayerShopkeeper> forRemoval = new ArrayList<PlayerShopkeeper>();
						for (OfflinePlayer inactivePlayer : inactivePlayers) {
							// remove all shops of this inactive player:
							UUID playerUUID = inactivePlayer.getUniqueId();

							for (Shopkeeper shopkeeper : shopkeepersById.values()) {
								if (shopkeeper instanceof PlayerShopkeeper) {
									PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
									UUID ownerUUID = playerShop.getOwnerUUID();
									if (ownerUUID.equals(playerUUID)) {
										forRemoval.add(playerShop);
									}
								}
							}
						}

						// remove those shopkeepers:
						if (!forRemoval.isEmpty()) {
							for (PlayerShopkeeper shopkeeper : forRemoval) {
								shopkeeper.delete();
								Log.info("Shopkeeper owned by " + shopkeeper.getOwnerAsString() + " at "
										+ shopkeeper.getPositionString() + " has been removed for owner inactivity.");
							}

							// save:
							save();
						}
					}
				});
			}
		});
	}

	// HANDLING PLAYER NAME CHANGES:

	// updates owner names for the shopkeepers of the specified player:
	void updateShopkeepersForPlayer(UUID playerUUID, String playerName) {
		boolean dirty = false;
		for (Shopkeeper shopkeeper : shopkeepersById.values()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
				UUID ownerUUID = playerShop.getOwnerUUID();
				String ownerName = playerShop.getOwnerName();

				if (ownerUUID.equals(playerUUID)) {
					if (!ownerName.equals(playerName)) {
						// update the stored name, because the player must have changed it:
						playerShop.setOwner(playerUUID, playerName);
						dirty = true;
					} else {
						// The shop was already updated to uuid based identification and the player's name hasn't
						// changed.
						// If we assume that this is consistent among all shops of this player
						// we can stop checking the other shops here:
						return;
					}
				}
			}
		}

		if (dirty) {
			this.save();
		}
	}

	// SHOPS LOADING AND SAVING

	private static class SaveInfo {
		long startTime;
		long packingDuration;
		long ioStartTime;
		long ioDuration;
		long fullDuration;

		public void printDebugInfo() {
			Log.debug("Saved shopkeeper data (" + fullDuration + "ms (Data packing: " + packingDuration + "ms, Async IO: " + ioDuration + "ms))");
		}
	}

	private File getSaveFile() {
		return new File(this.getDataFolder(), "save.yml");
	}

	// returns false if there was some issue during loading
	private boolean load() {
		File file = this.getSaveFile();
		if (!file.exists()) {
			// file does not exist yet -> no shopkeeper data available
			return true;
		}

		YamlConfiguration config = new YamlConfiguration();
		Scanner scanner = null;
		FileInputStream stream = null;
		try {
			if (Settings.fileEncoding != null && !Settings.fileEncoding.isEmpty()) {
				stream = new FileInputStream(file);
				scanner = new Scanner(stream, Settings.fileEncoding);
				scanner.useDelimiter("\\A");
				if (!scanner.hasNext()) {
					return true; // file is completely empty -> no shopkeeper data is available
				}
				String data = scanner.next();
				config.loadFromString(data);
			} else {
				config.load(file);
			}
		} catch (Exception e) {
			// issue detected:
			e.printStackTrace();
			return false; // disable without save
		} finally {
			if (scanner != null) {
				scanner.close();
			}
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
			ShopType<?> shopType = shopTypesManager.get(section.getString("type"));
			// unknown shop type
			if (shopType == null) {
				// got an owner entry? -> default to normal player shop type
				if (section.contains("owner")) {
					Log.warning("No valid shop type specified for shopkeeper '" + key + "': defaulting to "
							+ DefaultShopTypes.PLAYER_NORMAL().getIdentifier());
					shopType = DefaultShopTypes.PLAYER_NORMAL();
				} else {
					// no valid shop type given..
					Log.warning("Failed to load shopkeeper '" + key + "': unknown type");
					return false; // disable without save
				}
			}

			// load shopkeeper:
			try {
				Shopkeeper shopkeeper = shopType.loadShopkeeper(section);
				if (shopkeeper == null) {
					throw new ShopkeeperCreateException("ShopType returned null shopkeeper!");
				}
			} catch (ShopkeeperCreateException e) {
				Log.warning("Failed to load shopkeeper '" + key + "': " + e.getMessage());
				return false;
			}
		}
		return true;
	}

	@Override
	public void save() {
		if (Settings.saveInstantly) {
			this.saveReal();
		} else {
			dirty = true;
		}
	}

	@Override
	public void saveReal() {
		this.saveReal(true);
	}

	// should only get called sync on disable:
	private void saveReal(boolean async) {
		if (skipSaving) {
			Log.debug("Skipped saving due to flag.");
			return;
		}

		// is another async save task already running?
		if (async && saveIOTask != -1) {
			// set flag which triggers a new save once that current task is done:
			saveRealAgain = true;
			return;
		}

		// store shopkeeper data into memory configuration:
		saveInfo.startTime = System.currentTimeMillis();
		final YamlConfiguration config = new YamlConfiguration();
		int counter = 1;
		for (Shopkeeper shopkeeper : shopkeepersById.values()) {
			String sectionKey = String.valueOf(counter++);
			ConfigurationSection section = config.createSection(sectionKey);
			try {
				shopkeeper.save(section);
			} catch (Exception e) {
				// error while saving shopkeeper data:
				// skip this shopkeeper and print warning + stacktrace:
				config.set(sectionKey, null);
				Log.warning("Couldn't save shopkeeper '" + shopkeeper.getUniqueId() + "' at " + shopkeeper.getPositionString() + "!");
				e.printStackTrace();
			}
		}
		// time to store shopkeeper data in memory configuration:
		saveInfo.packingDuration = System.currentTimeMillis() - saveInfo.startTime;

		dirty = false;

		if (!async) {
			// sync file io:
			this.saveDataToFile(config, null);
			// print debug info:
			saveInfo.printDebugInfo();
		} else {
			// async file io:
			saveIOTask = Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {

				@Override
				public void run() {
					saveDataToFile(config, new Runnable() {

						@Override
						public void run() {
							// continue sync:
							Bukkit.getScheduler().runTask(ShopkeepersPlugin.this, new Runnable() {

								@Override
								public void run() {
									saveIOTask = -1;

									// print debug info:
									saveInfo.printDebugInfo();

									// did we get another request to saveReal() in the meantime?
									if (saveRealAgain) {
										// trigger another full save with latest data:
										saveRealAgain = false;
										saveReal();
									}
								}
							});
						}
					});
				}
			}).getTaskId();
		}
	}

	// max total delay: 500ms
	private static final int SAVING_MAX_ATTEMPTS = 20;
	private static final long SAVING_ATTEMPTS_DELAY_MILLIS = 25;

	// can be run async and sync:
	private void saveDataToFile(FileConfiguration config, Runnable callback) {
		assert config != null;

		saveInfo.ioStartTime = System.currentTimeMillis();

		File saveFile = this.getSaveFile();
		File tempSaveFile = new File(saveFile.getParentFile(), saveFile.getName() + ".temp");

		// first trying to save to a temporary save file
		// if all goes well, the save file gets replaced with the temporary file

		int savingAttempt = 0;
		boolean printStacktrace = true;
		String error;
		Exception exception;

		while (++savingAttempt <= SAVING_MAX_ATTEMPTS) {
			boolean problem = false;
			error = null;
			exception = null;

			// remove old temporary file, if there is one:
			if (!problem) {
				if (tempSaveFile.exists()) {
					if (!tempSaveFile.canWrite()) {
						error = "Cannot write to temporary save file! (" + tempSaveFile.getName() + ")";
						problem = true;
					} else {
						// remove old temporary save file:
						if (!tempSaveFile.delete()) {
							error = "Couldn't delete existing temporary save file! (" + tempSaveFile.getName() + ")";
							problem = true;
						}
					}
				}
			}

			// make sure that the parent directories exist:
			if (!problem) {
				File parentDir = tempSaveFile.getParentFile();
				if (parentDir != null && !parentDir.exists()) {
					if (!parentDir.mkdirs()) {
						error = "Couldn't create parent directories for temporary save file! (" + parentDir.getAbsolutePath() + ")";
						problem = true;
					}
				}
			}

			// create new temporary save file:
			if (!problem) {
				try {
					tempSaveFile.createNewFile();
				} catch (IOException e) {
					error = "Couldn't create temporary save file! (" + tempSaveFile.getName() + ")";
					exception = e;
					problem = true;
				}
			}

			// write shopkeeper data to temporary save file:
			if (!problem) {
				PrintWriter writer = null;
				try {
					if (Settings.fileEncoding != null && !Settings.fileEncoding.isEmpty()) {
						writer = new PrintWriter(tempSaveFile, Settings.fileEncoding);
						writer.write(config.saveToString());
					} else {
						config.save(tempSaveFile);
					}
				} catch (Exception e) {
					error = "Couldn't save data to temporary save file!(" + tempSaveFile.getName() + ")";
					exception = e;
					problem = true;
				} finally {
					if (writer != null) {
						writer.close();
					}
				}
			}

			// delete old save file:
			if (!problem) {
				if (saveFile.exists()) {
					if (!saveFile.canWrite()) {
						error = "Cannot write to save file! (" + saveFile.getName() + ")";
						problem = true;
					} else {
						// remove old save file:
						if (!saveFile.delete()) {
							error = "Couldn't delete existing old save file! (" + saveFile.getName() + ")";
							problem = true;
						}
					}
				}
			}

			// rename temporary save file:
			if (!problem) {
				if (!tempSaveFile.renameTo(saveFile)) {
					error = "Couldn't rename temporary save file! (" + tempSaveFile.getName() + " to " + saveFile.getName() + ")";
					problem = true;
				}
			}

			// handle problem situation:
			if (problem) {
				// don't spam with errors and stacktraces, only print them once for the first try:
				if (exception != null && printStacktrace) {
					printStacktrace = false;
					exception.printStackTrace();
				}
				Log.severe("Saving attempt " + savingAttempt + " failed: " + (error != null ? error : "Unknown error"));

				if (savingAttempt < SAVING_MAX_ATTEMPTS) {
					// try again after a small delay:
					try {
						Thread.sleep(SAVING_ATTEMPTS_DELAY_MILLIS);
					} catch (InterruptedException e) {
					}
				} else {
					// saving failed even after a bunch of retries:
					Log.severe("Saving failed! Save data might be lost! :(");
					break;
				}
			} else {
				// saving was successful:
				break;
			}
		}

		long now = System.currentTimeMillis();
		saveInfo.ioDuration = now - saveInfo.ioStartTime; // time for pure io
		saveInfo.fullDuration = now - saveInfo.startTime; // time from saveReal() call to finished save

		if (callback != null) {
			callback.run();
		}
	}

	private static class ConfirmEntry {

		private final Runnable action;
		private final int taskId;

		public ConfirmEntry(Runnable action, int taskId) {
			this.taskId = taskId;
			this.action = action;
		}

		public int getTaskId() {
			return taskId;
		}

		public Runnable getAction() {
			return action;
		}
	}
}
