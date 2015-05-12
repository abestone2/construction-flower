package com.abocalypse.constructionflower.util;

import java.util.EnumSet;
import java.util.Set;

public class EnumCycler<T extends Enum<T>> implements ICycler<T> {
	
	private int ordinal;
	private int size;
	private T[] values;
	private EnumSet<T> valuesSet;
	
	public EnumCycler(Class<T> cls) {
		this.values = cls.getEnumConstants();
		this.valuesSet = EnumSet.allOf(cls);
		size = values.length;
		ordinal = 0;
	}
	
	@Override
	public T value() {
		return values[ordinal];
	}
	
	@Override
	public void advance() {
		ordinal++;
		if ( ordinal == size ) {
			ordinal = 0;
		}
	}
	
	@Override
	public void advanceTo(T t) {
		ordinal = t.ordinal();
		
	}
	
	@Override
	public T next() {
		this.advance();
		return this.value();
	}
	
	@Override
	public void advanceToNot(Set<T> skip) {
		this.advance();
		if ( skip == null ) {
			return;
		} else if ( skip.equals(this.valuesSet) ) {
			// TODO not sure what should happen here
			return;
		}
		while ( skip.contains(this.value()) ) {
			this.advance();
		}
	}

}
