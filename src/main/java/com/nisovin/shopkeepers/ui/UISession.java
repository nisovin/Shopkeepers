package com.nisovin.shopkeepers.ui;

import com.nisovin.shopkeepers.Shopkeeper;

class UISession {
	// store shopkeeper directly and not by id, because the id might change or currently be invalid (for inactive shopkeepers).. important especially for remotely opened windows
	private final Shopkeeper shopkeeper;
	private final UIHandler uiHandler;

	UISession(Shopkeeper shopkeeper, UIHandler handler) {
		this.shopkeeper = shopkeeper;
		this.uiHandler = handler;
	}

	public Shopkeeper getShopkeeper() {
		return shopkeeper;
	}

	public UIHandler getUIHandler() {
		return uiHandler;
	}

	public UIType getUIManager() {
		return uiHandler.getUIManager();
	}
}