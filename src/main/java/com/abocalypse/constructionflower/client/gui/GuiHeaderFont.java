package com.abocalypse.constructionflower.client.gui;

import java.awt.Font;

import com.abocalypse.constructionflower.truetyper.TrueTypeFont;

public class GuiHeaderFont {
	
	private static TrueTypeFont headerFont;
	
	public static void init() {
			Font awtFont = new Font("Ariel", Font.PLAIN, 18);
			headerFont = new TrueTypeFont(awtFont, true);
	}
	
	public static TrueTypeFont getFont() {
		return headerFont;
	}
}
