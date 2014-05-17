package com.nisovin.shopkeepers.ui.defaults;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.ui.UIManager;

public class DefaultUIs {

	private final static List<UIManager> VALUES = new ArrayList<UIManager>();
	private final static List<UIManager> unmodifiable = Collections.unmodifiableList(VALUES);

	private static UIManager add(UIManager uiManager) {
		assert uiManager != null;
		VALUES.add(uiManager);
		return uiManager;
	}

	public static List<UIManager> getValues() {
		return unmodifiable;
	}

	public static boolean isDefaultWindowManager(String identifier) {
		if (identifier == null) return false;
		for (UIManager manager : VALUES) {
			if (manager.getIdentifier().equals(identifier)) {
				return true;
			}
		}
		return false;
	}

	// DEFAULT UIs:

	public static final UIManager EDITOR_WINDOW = add(new UIManager("editor window", null));

	public static final UIManager TRADING_WINDOW = add(new UIManager("trading window", null));

	public static final UIManager HIRING_WINDOW = add(new UIManager("hiring window", "shopkeeper.hire"));
}