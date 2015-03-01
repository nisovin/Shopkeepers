package com.nisovin.shopkeepers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.shopobjects.living.LivingEntityType;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;

public abstract class Shopkeeper {

	private UUID uniqueId;
	protected ShopObject shopObject;
	protected String worldName;
	protected int x;
	protected int y;
	protected int z;
	protected String name;

	private boolean valid = true;

	protected final Map<String, UIHandler> uiHandlers = new HashMap<String, UIHandler>();
	private boolean uiActive = true; // can be used to deactivate UIs for this shopkeeper

	protected Shopkeeper(ConfigurationSection config) {
		Validate.notNull(config);
		this.load(config);
	}

	/**
	 * Creates a new shopkeeper and spawns it in the world. This should be used when a player is
	 * creating a new shopkeeper.
	 * 
	 * @param location
	 *            the location to spawn at
	 * @param objectType
	 *            the ShopObjectType of this shopkeeper
	 */
	protected Shopkeeper(ShopCreationData creationData) {
		Validate.notNull(creationData.location);
		Validate.notNull(creationData.objectType);

		this.uniqueId = UUID.randomUUID();

		Location location = creationData.location;
		this.worldName = location.getWorld().getName();
		this.x = location.getBlockX();
		this.y = location.getBlockY();
		this.z = location.getBlockZ();
		this.shopObject = creationData.objectType.createObject(this, creationData);
	}

	/**
	 * Loads a shopkeeper's saved data from a config section of a config file.
	 * 
	 * @param config
	 *            the config section
	 */
	protected void load(ConfigurationSection config) {
		String uniqueIdString = config.getString("uniqueId", "");
		try {
			this.uniqueId = UUID.fromString(uniqueIdString);
		} catch (IllegalArgumentException e) {
			if (!uniqueIdString.isEmpty()) {
				Log.warning("Invalid shop uuid '" + uniqueIdString + "'. Creating a new one.");
			}
			this.uniqueId = UUID.randomUUID();
		}

		this.name = config.getString("name");
		this.worldName = config.getString("world");
		this.x = config.getInt("x");
		this.y = config.getInt("y");
		this.z = config.getInt("z");
		ShopObjectType objectType = ShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().get(config.getString("object"));
		if (objectType == null) {
			Log.warning("Invalid object type '" + config.getString("object") + "' for shopkeeper '" + uniqueId + "'. Did you edit the save file? Switching to type 'villager'.");
			objectType = LivingEntityType.VILLAGER.getObjectType(); // default to villager
		}
		this.shopObject = objectType.createObject(this, new ShopCreationData()); // dummy ShopCreationData
		this.shopObject.load(config);
	}

	/**
	 * Saves the shopkeeper's data to the specified configuration section.
	 * 
	 * @param config
	 *            the config section
	 */
	protected void save(ConfigurationSection config) {
		config.set("uniqueId", uniqueId.toString());
		config.set("name", name);
		config.set("world", worldName);
		config.set("x", x);
		config.set("y", y);
		config.set("z", z);
		config.set("type", this.getType().getIdentifier());
		shopObject.save(config);
	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	/**
	 * Gets the type of this shopkeeper (ex: admin, normal player, book player, buying player, trading player, etc.).
	 * 
	 * @return the shopkeeper type
	 */
	public abstract ShopType<?> getType();

	public String getName() {
		return Utils.colorize(name);
	}

	public void setName(String name) {
		this.name = shopObject.trimToNameLength(name);
		shopObject.setName(this.name);
	}

	public ShopObject getShopObject() {
		return shopObject;
	}

	public boolean needsSpawning() {
		return shopObject.needsSpawning();
	}

	/**
	 * Spawns the shopkeeper into the world at its spawn location and overwrites it's AI.
	 */
	public boolean spawn() {
		return shopObject.spawn();
	}

	/**
	 * Whether or not this shopkeeper needs to be spawned and despawned with chunk load and unloads.
	 * 
	 * @return
	 */
	public boolean activateByChunk() {
		return shopObject.getObjectType().activateByChunk();
	}

	/**
	 * Checks if the shopkeeper is active (is alive in the world).
	 * 
	 * @return whether the shopkeeper is active
	 */
	public boolean isActive() {
		return shopObject.isActive();
	}

	/**
	 * Teleports this shopkeeper to its spawn location.
	 * 
	 * @return whether to update this shopkeeper in the collection
	 */
	public boolean teleport() {
		return shopObject.check();
	}

	/**
	 * Removes this shopkeeper from the world.
	 */
	public void despawn() {
		shopObject.despawn();
	}

	/**
	 * Persistently removes this shopkeeper.
	 */
	public void delete() {
		ShopkeepersPlugin.getInstance().deleteShopkeeper(this);
	}

	protected void onDeletion() {
		shopObject.delete();
		valid = false;
	}

	/**
	 * The shopkeepers gets invalid, when he was deleted.
	 * 
	 * @return true, if not deleted
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * Gets the ChunkData identifying the chunk this shopkeeper spawns in.
	 * 
	 * @return the chunk information
	 */
	public ChunkData getChunkData() {
		return new ChunkData(worldName, (x >> 4), (z >> 4));
	}

	public String getPositionString() {
		return worldName + "," + x + "," + y + "," + z;
	}

	public Location getActualLocation() {
		return shopObject.getActualLocation();
	}

	/**
	 * Gets the name of the world this shopkeeper lives in.
	 * 
	 * @return the world name
	 */
	public String getWorldName() {
		return worldName;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	/**
	 * This only works if the world is loaded.
	 * 
	 * @return null, if the world this shopkeeper is in isn't loaded
	 */
	public Location getLocation() {
		World world = Bukkit.getWorld(worldName);
		if (world == null) return null;
		return new Location(world, x, y, z);
	}

	/**
	 * Sets the stored location of this Shopkeeper.
	 * This will not actually move the shopkeeper entity until the next time teleport() is called.
	 * 
	 * @param location
	 *            The new stored location of this shopkeeper.
	 */
	public void setLocation(Location location) {
		ChunkData oldChunk = this.getChunkData();
		x = location.getBlockX();
		y = location.getBlockY();
		z = location.getBlockZ();
		worldName = location.getWorld().getName();

		// update shopkeeper in chunk map:
		ShopkeepersPlugin.getInstance().onShopkeeperMove(this, oldChunk);
	}

	/**
	 * Gets the shopkeeper's object ID. This is can change when the shopkeeper object (ex. shopkeeper entity) respawns.
	 * 
	 * @return the object id, or null if the shopkeeper object is currently not active
	 */
	public String getObjectId() {
		return shopObject.getId();
	}

	/**
	 * Gets the shopkeeper's trade recipes. This will be a list of ItemStack[3],
	 * where the first two elemets of the ItemStack[] array are the cost, and the third
	 * element is the trade result (the item sold by the shopkeeper).
	 * 
	 * @return the trade recipes of this shopkeeper
	 */
	public abstract List<ItemStack[]> getRecipes();

	// SHOPKEEPER UIs:

	public boolean isUIActive() {
		return uiActive;
	}

	public void deactivateUI() {
		uiActive = false;
	}

	public void activateUI() {
		uiActive = true;
	}

	/**
	 * Deactivates all currently open UIs (purchasing, editing, hiring, etc.) and closes them 1 tick later.
	 */
	public void closeAllOpenWindows() {
		ShopkeepersPlugin.getInstance().getUIManager().closeAllDelayed(this);
	}

	/**
	 * Gets the handler this specific shopkeeper is using for the specified interface type.
	 * 
	 * @param uiIdentifier
	 *            Specifies the user interface type.
	 * @return The handler, or null if this shopkeeper is not supporting the specified interface type
	 */
	public UIHandler getUIHandler(String uiIdentifier) {
		return uiHandlers.get(uiIdentifier);
	}

	/**
	 * 
	 * @param uiManager
	 *            Specifies the type of user interface.
	 * @return
	 */
	public UIHandler getUIHandler(UIType uiManager) {
		return uiManager != null ? uiHandlers.get(uiManager.getIdentifier()) : null;
	}

	/**
	 * Registers an ui handler for a specific type of user interface for this specific shopkeeper.
	 * 
	 * @param uiHandler
	 *            The handler
	 */
	public void registerUIHandler(UIHandler uiHandler) {
		Validate.notNull(uiHandler);
		uiHandlers.put(uiHandler.getUIType().getIdentifier(), uiHandler);
	}

	/**
	 * Attempts to open the interface specified by the given identifier for the specified player.
	 * Fails if this shopkeeper doesn't support the specified interface type, if the player
	 * cannot open this interface type for this shopkeeper (for example because of missing permissions),
	 * or if something else goes wrong.
	 * 
	 * @param uiIdentifier
	 *            Specifies the user interface type.
	 * @param player
	 *            the player requesting the specified interface
	 * @return true the player's request was successful and the interface was opened, false otherwise
	 */
	public boolean openWindow(String uiIdentifier, Player player) {
		return ShopkeepersPlugin.getInstance().getUIManager().requestUI(uiIdentifier, this, player);
	}

	// shortcuts for the default window types:

	/**
	 * Attempts to open the editor interface of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player requesting the editor interface
	 * @return whether or not the player's request was successful and the player is now editing
	 */
	public boolean openEditorWindow(Player player) {
		return this.openWindow(DefaultUIs.EDITOR_WINDOW.getIdentifier(), player);
	}

	/**
	 * Attempts to open the trading interface of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player requesting the trading interface
	 * @return whether or not the player's request was successful and the player is now trading
	 */
	public boolean openTradingWindow(Player player) {
		return this.openWindow(DefaultUIs.TRADING_WINDOW.getIdentifier(), player);
	}

	/**
	 * Attempts to open the hiring interface of this shopkeeper for the specified player.
	 * Fails if this shopkeeper type doesn't support hiring (ex. admin shops).
	 * 
	 * @param player
	 *            the player requesting the hiring interface
	 * @return whether or not the player's request was successful and the player is now hiring
	 */
	public boolean openHireWindow(Player player) {
		return this.openWindow(DefaultUIs.HIRING_WINDOW.getIdentifier(), player);
	}

	// NAMING:

	public void startNaming(Player player) {
		ShopkeepersPlugin.getInstance().onNaming(player, this);
	}

	// HANDLE INTERACTION:

	/**
	 * Called when a player interacts with this shopkeeper.
	 * 
	 * @param player
	 *            the interacting player
	 */
	protected void onPlayerInteraction(Player player) {
		assert player != null;
		if (player.isSneaking()) {
			// open editor window:
			this.openEditorWindow(player);
		} else {
			// open trading window:
			// check for special conditions, which else would remove the player's spawn egg when attempting to open the
			// trade window via nms/reflection,
			// because of minecraft's spawnChildren code
			if (player.getItemInHand().getType() == Material.MONSTER_EGG) {
				Log.debug("Cannot open trading window: Player is holding a spawn egg");
				Utils.sendMessage(player, Settings.msgCantOpenShopWithSpawnEgg);
				return;
			}
			this.openTradingWindow(player);
		}
	}
}