package com.nisovin.shopkeepers.shoptypes.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Stores information about a type of book being sold for a certain price.
 * TODO Currently unused.
 */
public class BookOffer {

	private String bookTitle;
	private int price;

	public BookOffer(String bookTitle, int price) {
		assert bookTitle != null;
		assert price >= 0;
		this.bookTitle = bookTitle;
		this.price = price;
	}

	public String getBookTitle() {
		return bookTitle;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		assert price >= 0;
		this.price = price;
	}

	// //////////
	// STATIC UTILITIES
	// //////////

	public static void saveToConfig(ConfigurationSection config, String node, Collection<BookOffer> offers) {
		ConfigurationSection offersSection = config.createSection(node);
		for (BookOffer offer : offers) {
			offersSection.set(offer.getBookTitle(), offer.getPrice());
		}
	}

	public static List<BookOffer> loadFromConfig(ConfigurationSection config, String node) {
		List<BookOffer> offers = new ArrayList<BookOffer>();
		ConfigurationSection offersSection = config.getConfigurationSection(node);
		if (offersSection != null) {
			for (String bookTitle : offersSection.getKeys(false)) {
				int price = offersSection.getInt(bookTitle);
				offers.add(new BookOffer(bookTitle, price));
			}
		}
		return offers;
	}
}
