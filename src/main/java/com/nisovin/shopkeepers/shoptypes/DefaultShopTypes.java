package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.List;

import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeepersPlugin;

public class DefaultShopTypes {

	private final ShopType<?> adminShopType = new AdminShopType();
	private final ShopType<?> normalPlayerShopType = new NormalPlayerShopType();
	private final ShopType<?> tradingPlayerShopType = new TradingPlayerShopType();
	private final ShopType<?> buyingPlayerShopType = new BuyingPlayerShopType();
	private final ShopType<?> bookPlayerShopType = new BookPlayerShopType();

	public DefaultShopTypes() {
	}

	public List<ShopType<?>> getAllShopTypes() {
		List<ShopType<?>> shopTypes = new ArrayList<ShopType<?>>();
		shopTypes.add(adminShopType);
		shopTypes.add(normalPlayerShopType);
		shopTypes.add(tradingPlayerShopType);
		shopTypes.add(buyingPlayerShopType);
		shopTypes.add(bookPlayerShopType);
		return shopTypes;
	}

	public ShopType<?> getAdminShopType() {
		return adminShopType;
	}

	public ShopType<?> getNormalPlayerShopType() {
		return normalPlayerShopType;
	}

	public ShopType<?> getTradingPlayerShopType() {
		return tradingPlayerShopType;
	}

	public ShopType<?> getBuyingPlayerShopType() {
		return buyingPlayerShopType;
	}

	public ShopType<?> getBookPlayerShopType() {
		return bookPlayerShopType;
	}

	// STATICS (for convenience):

	public static ShopType<?> ADMIN() {
		return ShopkeepersPlugin.getInstance().getDefaultShopTypes().getAdminShopType();
	}

	public static ShopType<?> PLAYER_NORMAL() {
		return ShopkeepersPlugin.getInstance().getDefaultShopTypes().getNormalPlayerShopType();
	}

	public static ShopType<?> PLAYER_TRADING() {
		return ShopkeepersPlugin.getInstance().getDefaultShopTypes().getTradingPlayerShopType();
	}

	public static ShopType<?> PLAYER_BUYING() {
		return ShopkeepersPlugin.getInstance().getDefaultShopTypes().getBuyingPlayerShopType();
	}

	public static ShopType<?> PLAYER_BOOK() {
		return ShopkeepersPlugin.getInstance().getDefaultShopTypes().getBookPlayerShopType();
	}
}
