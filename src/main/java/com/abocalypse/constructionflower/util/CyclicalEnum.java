package com.abocalypse.constructionflower.util;

public class CyclicalEnum<T> {
	
	private int ordinal;
	private int size;
	private T[] values;
	
	public CyclicalEnum(Class<T> cls) {
		values = cls.getEnumConstants();
		size = values.length;
		ordinal = 0;
	}
	
	public T value() {
		return values[ordinal];
	}
	
	public void advance() {
		ordinal++;
		if ( ordinal == size ) {
			ordinal = 0;
		}
	}
	
	public T next() {
		this.advance();
		return this.value();
	}

}
