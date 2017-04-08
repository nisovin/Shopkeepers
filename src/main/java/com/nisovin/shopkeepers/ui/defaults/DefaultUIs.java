package com.nisovin.shopkeepers.ui.defaults;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.ShopkeepersAPI;
import com.nisovin.shopkeepers.ui.UIType;

public class DefaultUIs {

	public static List<UIType> getAllUITypes() {
		List<UIType> defaults = new ArrayList<UIType>();
		defaults.add(EDITOR_WINDOW);
		defaults.add(TRADING_WINDOW);
		defaults.add(HIRING_WINDOW);
		return defaults;
	}

	// DEFAULT UIs:

	public static final UIType EDITOR_WINDOW = new UIType("editor-window", null);

	public static final UIType TRADING_WINDOW = new UIType("trading-window", null);

	public static final UIType HIRING_WINDOW = new UIType("hiring-window", ShopkeepersAPI.HIRE_PERMISSION);

}
