package com.nisovin.shopkeepers.shoptypes.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Stores information about a type of book being sold for a certain price.
 * TODO Currently unused.
 */
public class BookOffer {

	private String bookTitle;
	private int price;

	public BookOffer(String bookTitle, int price) {
		// TODO what about empty book titles, and price of 0?
		Validate.notNull(bookTitle, "Book title cannot be null!");
		this.bookTitle = bookTitle;
		this.setPrice(price);
	}

	public String getBookTitle() {
		return bookTitle;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		// TODO what about price of 0? maybe filter that?
		Validate.isTrue(price >= 0, "Price cannot be negative!");
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
