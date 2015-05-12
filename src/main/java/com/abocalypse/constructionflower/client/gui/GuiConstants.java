package com.abocalypse.constructionflower.client.gui;

import java.util.EnumMap;

import com.abocalypse.constructionflower.plan.PlanPartSpec;
import com.abocalypse.constructionflower.plan.WorldPlanRegistry;

public class GuiConstants {
	
	public static final int HORIZONTAL_GUTTER = 5;
	public static final int VERTICAL_GUTTER = 8;
	public static final int BUTTON_WIDTH = 150;
	public static final int BUTTON_HEIGHT = 20;
	public static final int SLOT_HEIGHT = 20;
	public static final int TEXT_FIELD_HEIGHT = 20;
	public static final int HEADER_HEIGHT = 20;
	public static final int SCROLL_BAR_WIDTH = 8;
	
	public static final EnumMap<PlanPartSpec.Orientation, String> ORIENTATION_TEXT;
	static {
		ORIENTATION_TEXT = new EnumMap<PlanPartSpec.Orientation, String>(PlanPartSpec.Orientation.class);
		ORIENTATION_TEXT.put(PlanPartSpec.Orientation.TOPNORTH, "Top of Plan is North");
		ORIENTATION_TEXT.put(PlanPartSpec.Orientation.TOPEAST, "Top of Plan is East");
		ORIENTATION_TEXT.put(PlanPartSpec.Orientation.TOPSOUTH, "Top of Plan is South");
		ORIENTATION_TEXT.put(PlanPartSpec.Orientation.TOPWEST, "Top of Plan is West");
	}
	
	public static final EnumMap<WorldPlanRegistry.AnchorMode, String> ANCHOR_MODE_TEXT;
	static {
		ANCHOR_MODE_TEXT= new EnumMap<WorldPlanRegistry.AnchorMode, String>(WorldPlanRegistry.AnchorMode.class);
		ANCHOR_MODE_TEXT.put(WorldPlanRegistry.AnchorMode.RELATIVE_TO_ORIGIN, "Relative to Origin");
		ANCHOR_MODE_TEXT.put(WorldPlanRegistry.AnchorMode.RELATIVE_TO_SPAWN, "Relative to Spawn Point");
		ANCHOR_MODE_TEXT.put(WorldPlanRegistry.AnchorMode.RELATIVE_TO_COORDS, "Relative to:");
		ANCHOR_MODE_TEXT.put(WorldPlanRegistry.AnchorMode.RELATIVE_TO_POSITION, "Relative to My Position");
		ANCHOR_MODE_TEXT.put(WorldPlanRegistry.AnchorMode.RELATIVE_TO_PLAN, "Relative to Anchor of:");
	}


}
