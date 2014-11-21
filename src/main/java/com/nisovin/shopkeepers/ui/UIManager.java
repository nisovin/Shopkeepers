package com.nisovin.shopkeepers.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.abstractTypes.AbstractType;

/**
 * The central component which handles one specific type of a shopkeeper interface window.
 */
public class UIManager extends AbstractType {

	static class UISession {
		// store shopkeeper directly and not by id, because the id might change or currently be invalid (for inactive shopkeepers).. important especially for remotely opened windows
		final Shopkeeper shopkeeper;
		final UIHandler uiHandler;

		UISession(Shopkeeper shopkeeper, UIHandler handler) {
			this.shopkeeper = shopkeeper;
			this.uiHandler = handler;
		}

		public UIManager getUIManager() {
			return uiHandler.getUIManager();
		}
	}

	protected final Map<String, UISession> players = new HashMap<String, UISession>();

	public UIManager(String windowIdentifier, String permission) {
		super(windowIdentifier, permission);
	}

	protected UISession getSession(Player player) {
		if (player == null) return null;
		return players.get(player.getName());
	}

	public Shopkeeper getOpenShopkeeper(Player player) {
		UISession session = this.getSession(player);
		return session != null ? session.shopkeeper : null;
	}

	public boolean hasOpen(Player player) {
		return this.getOpenShopkeeper(player) != null;
	}

	public boolean requestOpen(Shopkeeper shopkeeper, Player player) {
		if (player == null || shopkeeper == null) {
			Log.debug("Cannot open " + identifier + ": invalid argument(s): null");
			return false;
		}

		UIHandler uiHandler = shopkeeper.getUIHandler(identifier);
		if (uiHandler == null) {
			Log.debug("Cannot open " + identifier + ": this shopkeeper is not handling/supporting this type of interface window.");
			return false;
		}

		String playerName = player.getName();
		if (!uiHandler.canOpen(player)) {
			Log.debug("Cannot open " + identifier + " for '" + playerName + "'.");
			return false;
		}

		Log.debug("Opening " + identifier + "...");
		boolean isOpen = uiHandler.openWindow(player);
		if (isOpen) {
			Log.debug(identifier + " opened");
			UISession oldSession = players.put(playerName, new UISession(shopkeeper, uiHandler));
			if (oldSession != null) {
				// old window already should automatically have been closed by the new window.. no need currently, to do that here
			}
			return true;
		} else {
			Log.debug(identifier + " NOT opened");
			return false;
		}
	}

	protected void onClose(Player player) {
		assert player != null;
		players.remove(player.getName());
	}

	protected void closeAll(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		Iterator<Entry<String, UISession>> iter = players.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, UISession> entry = iter.next();
			UISession session = entry.getValue();
			if (session.shopkeeper.equals(shopkeeper)) {
				iter.remove();
				Player player = Bukkit.getPlayerExact(entry.getKey());
				if (player != null) {
					player.closeInventory();
				}
			}
		}
	}

	protected void closeAll() {
		for (String playerName : players.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
				player.closeInventory();
			}
		}
		players.clear();
	}
}