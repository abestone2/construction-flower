package com.abocalypse.constructionflower.plan;

import java.util.Map;

import com.abocalypse.constructionflower.lib.EnumOrientation;

public class PlanSpec extends SubPlanSpec {
	
	public PlanSpec(Map<String,Object> propertiesFromFile, EnumOrientation orientation) {
		super(properties(propertiesFromFile, orientation));
	}
	
	private static Map<String, Object> properties(Map<String,Object> propertiesFromFile, EnumOrientation orientation) {
		propertiesFromFile.put("orientation", orientation.toString());
		return propertiesFromFile;
	}
	
}
