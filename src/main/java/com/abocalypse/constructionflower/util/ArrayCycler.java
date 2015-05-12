package com.abocalypse.constructionflower.util;

import java.util.ArrayList;
import java.util.Set;

public class ArrayCycler<T> implements ICycler<T> {
	
	private int index;
	private final ArrayList<T> valueArray;
	
	public ArrayCycler(ArrayList<T> valueArray) {
		this.valueArray = valueArray;
		this.index = 0;
	}

	@Override
	public T value() {
		return this.valueArray.get(this.index);
	}

	@Override
	public void advance() {
		this.index++;
		if ( index == this.valueArray.size()) {
			this.index = 0;
		}
	}

	@Override
	public void advanceTo(T t) {
		int startIndex = this.index;
		this.advance();
		while( this.index != startIndex ) {
			if ( this.value() == t ) {
				break;
			}
			this.advance();
		}
	}

	@Override
	public T next() {
		this.advance();
		return this.value();
	}

	@Override
	public void advanceToNot(Set<T> skip) {
		int startIndex = this.index;
		this.advance();
		while( this.index != startIndex ) {
			if ( !skip.contains(this.value()) ) {
				break;
			}
			this.advance();
		}
	}

}
