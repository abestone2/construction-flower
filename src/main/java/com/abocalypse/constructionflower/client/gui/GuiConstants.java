package com.abocalypse.constructionflower.client.gui;

import java.util.EnumMap;

import com.abocalypse.constructionflower.lib.EnumAnchorMode;
import com.abocalypse.constructionflower.lib.EnumOrientation;

public class GuiConstants {
	
	public static final int HORIZONTAL_GUTTER = 5;
	public static final int VERTICAL_GUTTER = 6;
	public static final int BUTTON_WIDTH = 150;
	public static final int BUTTON_HEIGHT = 20;
	public static final int SLOT_HEIGHT = 20;
	public static final int TEXT_FIELD_HEIGHT = 20;
	public static final int SCROLL_BAR_WIDTH = 8;

	public static final int HEADER_HEIGHT = 12;
	public static final int HEADER_VERTICAL_GUTTER = 2;
	public static final int SPACE_FOR_HEADER_ROW = HEADER_HEIGHT + HEADER_VERTICAL_GUTTER;
	public static final float[] HEADER_COLOR = new float[]{0.53125F, 0.53125F, 0.8125F, 1.0F};
	
	public static final EnumMap<EnumOrientation, String> ORIENTATION_TEXT;
	static {
		ORIENTATION_TEXT = new EnumMap<EnumOrientation, String>(EnumOrientation.class);
		ORIENTATION_TEXT.put(EnumOrientation.TOPNORTH, "Top of Plan is North");
		ORIENTATION_TEXT.put(EnumOrientation.TOPEAST, "Top of Plan is East");
		ORIENTATION_TEXT.put(EnumOrientation.TOPSOUTH, "Top of Plan is South");
		ORIENTATION_TEXT.put(EnumOrientation.TOPWEST, "Top of Plan is West");
	}
	
	public static final EnumMap<EnumAnchorMode, String> ANCHOR_MODE_TEXT;
	static {
		ANCHOR_MODE_TEXT= new EnumMap<EnumAnchorMode, String>(EnumAnchorMode.class);
		ANCHOR_MODE_TEXT.put(EnumAnchorMode.RELATIVE_TO_ORIGIN, "Relative to Origin");
		ANCHOR_MODE_TEXT.put(EnumAnchorMode.RELATIVE_TO_SPAWN, "Relative to Spawn Point");
		ANCHOR_MODE_TEXT.put(EnumAnchorMode.RELATIVE_TO_COORDS, "Relative to:");
		ANCHOR_MODE_TEXT.put(EnumAnchorMode.RELATIVE_TO_POSITION, "Relative to My Position");
		ANCHOR_MODE_TEXT.put(EnumAnchorMode.RELATIVE_TO_PLAN, "Relative to Anchor of:");
	}


}
