package com.nisovin.shopkeepers.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.abstractTypes.TypeRegistry;
import com.nisovin.shopkeepers.ui.UIManager.UISession;

public class UITypeRegistry extends TypeRegistry<UIManager> {

	public UITypeRegistry() {
	}

	public void onEnable(ShopkeepersPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(new UIListener(this), plugin);
	}

	@Override
	protected String getTypeName() {
		return "UIManager";
	}

	public boolean requestUI(String uiIdentifier, Shopkeeper shopkeeper, Player player) {
		UIManager uiManager = this.get(uiIdentifier);
		if (uiManager == null) {
			Log.debug("Unknown interface type: " + uiIdentifier);
			return false;
		}
		return uiManager.requestOpen(shopkeeper, player);
	}

	UISession getSession(Player player) {
		if (player != null) {
			for (UIManager manager : this.registeredTypes.values()) {
				UISession session = manager.getSession(player);
				if (session != null) return session;
			}
		}
		return null;
	}

	public UIManager getOpenInterface(Player player) {
		UISession session = this.getSession(player);
		return session != null ? session.uiType : null;
	}

	public void onQuit(Player player) {
		if (player == null) return;
		for (UIManager manager : this.registeredTypes.values()) {
			manager.onClose(player);
		}
	}

	// TODO make sure that this is delayed where needed
	public void closeAll(Shopkeeper shopkeeper) {
		if (shopkeeper == null) return;
		for (UIManager manager : this.registeredTypes.values()) {
			manager.closeAll(shopkeeper);
		}
	}

	public void closeAllDelayed(final Shopkeeper shopkeeper) {
		if (shopkeeper == null) return;

		// delayed because this is/was originally called from inside the PlayerCloseInventoryEvent
		Bukkit.getScheduler().runTaskLater(ShopkeepersPlugin.getInstance(), new Runnable() {
			public void run() {
				closeAll(shopkeeper);
			}
		}, 1);
	}

	public void closeAll() {
		for (UIManager manager : this.registeredTypes.values()) {
			manager.closeAll();
		}
	}
}