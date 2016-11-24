package com.nisovin.shopkeepers.shoptypes.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.compat.NMSManager;

/**
 * Stores information about up to two items being traded for another item.
 */
public class TradingOffer {

	private ItemStack resultItem; // not null/empty
	private ItemStack item1; // not null/empty
	private ItemStack item2; // can be null

	public TradingOffer(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		Validate.isTrue(!Utils.isEmpty(resultItem), "Result item cannot be empty!");
		Validate.isTrue(!Utils.isEmpty(item1), "Item1 cannot be empty!");
		this.resultItem = resultItem.clone();
		this.item1 = item1.clone();
		this.item2 = Utils.isEmpty(item2) ? null : item2.clone();
	}

	public ItemStack getResultItem() {
		return resultItem.clone();
	}

	public ItemStack getItem1() {
		return item1.clone();
	}

	public ItemStack getItem2() {
		return item2 == null ? null : item2.clone();
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	public static void saveToConfig(ConfigurationSection config, String node, Collection<TradingOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		int id = 0;
		for (TradingOffer offer : offers) {
			// TODO temporary, due to a bukkit bug custom head item can currently not be saved
			if (Settings.skipCustomHeadSaving
					&& (Utils.isCustomHeadItem(offer.getItem1())
							|| Utils.isCustomHeadItem(offer.getItem2())
							|| Utils.isCustomHeadItem(offer.getResultItem()))) {
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
				if (Utils.isEmpty(resultItem) || Utils.isEmpty(item1)) continue; // invalid offer
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
				if (resultItem != null) {
					// legacy: the amount was stored separately from the item
					resultItem.setAmount(offerSection.getInt("amount", 1));
					if (offerSection.contains("attributes")) {
						String attributes = offerSection.getString("attributes");
						if (attributes != null && !attributes.isEmpty()) {
							resultItem = NMSManager.getProvider().loadItemAttributesFromString(resultItem, attributes);
						}
					}
				}
				if (Utils.isEmpty(resultItem)) continue; // invalid offer
				ItemStack item1 = offerSection.getItemStack("item1");
				if (Utils.isEmpty(item1)) continue; // invalid offer
				ItemStack item2 = offerSection.getItemStack("item2");
				// legacy: no attributes were stored for item1 and item2
				offers.add(new TradingOffer(resultItem, item1, item2));
			}
		}
		return offers;
	}
}
