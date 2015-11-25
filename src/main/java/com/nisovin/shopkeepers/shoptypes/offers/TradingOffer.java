package com.nisovin.shopkeepers.shoptypes.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.compat.NMSManager;

/**
 * Stores information about up to two items being traded for another item.
 */
public class TradingOffer {

	private ItemStack resultItem;
	private ItemStack item1;
	private ItemStack item2;

	public TradingOffer(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		assert resultItem != null;
		assert item1 != null;
		this.resultItem = resultItem;
		this.item1 = item1;
		this.item2 = item2;
	}

	public ItemStack getResultItem() {
		return resultItem;
	}

	public ItemStack getItem1() {
		return item1;
	}

	public ItemStack getItem2() {
		return item2;
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	public static void saveToConfig(ConfigurationSection config, String node, Collection<TradingOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		int id = 0;
		for (TradingOffer offer : offers) {
			// TODO temporary, due to a bukkit bug custom head item can currently not be saved
			if (Utils.isCustomHeadItem(offer.getItem1())
					|| Utils.isCustomHeadItem(offer.getItem2())
					|| Utils.isCustomHeadItem(offer.getResultItem())) {
				Log.warning("Skipping saving of trade involving a head item with custom texture, which cannot be saved currently due to a bukkit bug.");
				continue;
			}
			ConfigurationSection offerSection = offersSection.createSection(String.valueOf(id));
			Utils.saveItem(offerSection, "resultItem", offer.getResultItem());
			Utils.saveItem(offerSection, "item1", offer.getItem1());
			Utils.saveItem(offerSection, "item2", offer.getItem2());
			id++;
		}
	}

	public static List<TradingOffer> loadFromConfig(ConfigurationSection config, String node) {
		List<TradingOffer> offers = new ArrayList<TradingOffer>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String key : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(key);
				ItemStack resultItem = Utils.loadItem(offerSection, "resultItem");
				ItemStack item1 = Utils.loadItem(offerSection, "item1");
				ItemStack item2 = Utils.loadItem(offerSection, "item2");
				offers.add(new TradingOffer(resultItem, item1, item2));
			}
		}
		return offers;
	}

	// legacy:

	/*public static void saveToConfigOld(ConfigurationSection config, String node, Collection<TradingOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		int id = 0;
		for (TradingOffer offer : offers) {
			ItemStack resultItem = offer.getResultItem();
			ConfigurationSection offerSection = offersSection.createSection(id + "");
			offerSection.set("item", resultItem);
			String attributes = NMSManager.getProvider().saveItemAttributesToString(resultItem);
			if (attributes != null && !attributes.isEmpty()) {
				offerSection.set("attributes", attributes);
			}
			// legacy: amount was stored separately from the item
			offerSection.set("amount", resultItem.getAmount());
			offerSection.set("item1", offer.getItem1());
			offerSection.set("item2", offer.getItem2());
			// legacy: no attributes were stored for item1 and item2
			id++;
		}
	}*/

	public static List<TradingOffer> loadFromConfigOld(ConfigurationSection config, String node) {
		List<TradingOffer> offers = new ArrayList<TradingOffer>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String key : offersSection.getKeys(false)) {
				ConfigurationSection offerSection = offersSection.getConfigurationSection(key);
				ItemStack resultItem = offerSection.getItemStack("item");
				// legacy: the amount was stored separately from the item
				resultItem.setAmount(offerSection.getInt("amount", 1));
				if (offerSection.contains("attributes")) {
					String attributes = offerSection.getString("attributes");
					if (attributes != null && !attributes.isEmpty()) {
						resultItem = NMSManager.getProvider().loadItemAttributesFromString(resultItem, attributes);
					}
				}
				ItemStack item1 = offerSection.getItemStack("item1");
				ItemStack item2 = offerSection.getItemStack("item2");
				// legacy: no attributes were stored for item1 and item2
				offers.add(new TradingOffer(resultItem, item1, item2));
			}
		}
		return offers;
	}
}
