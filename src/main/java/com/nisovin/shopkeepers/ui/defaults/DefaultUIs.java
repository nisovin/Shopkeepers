package com.nisovin.shopkeepers.ui.defaults;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.ui.UIManager;

public class DefaultUIs {

	public static List<UIManager> getAll() {
		List<UIManager> defaults = new ArrayList<UIManager>();
		defaults.add(EDITOR_WINDOW);
		defaults.add(TRADING_WINDOW);
		defaults.add(HIRING_WINDOW);
		return defaults;
	}

	// DEFAULT UIs:

	public static final UIManager EDITOR_WINDOW = new UIManager("editor window", null);

	public static final UIManager TRADING_WINDOW = new UIManager("trading window", null);

	public static final UIManager HIRING_WINDOW = new UIManager("hiring window", "shopkeeper.hire");
}