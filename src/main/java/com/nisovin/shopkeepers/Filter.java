package com.nisovin.shopkeepers;

public interface Filter<T> {

	public boolean accept(T object);
}
