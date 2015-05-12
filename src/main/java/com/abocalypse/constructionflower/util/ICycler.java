package com.abocalypse.constructionflower.util;

import java.util.Set;

public interface ICycler<T> {
		
	public T value();
	
	public void advance();
	
	public void advanceTo(T t);
	
	public T next();
	
	public void advanceToNot(Set<T> skip);
	
}
