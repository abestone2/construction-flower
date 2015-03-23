package com.abocalypse.constructionflower.plan;

import java.util.Map;

public class PlanSpec extends SubPlanSpec {
	
	public PlanSpec(Map<String,Object> propertiesFromFile, PlanPartSpec.Orientation orientation) {
		super(properties(propertiesFromFile, orientation));
	}
	
	private static Map<String, Object> properties(Map<String,Object> propertiesFromFile, PlanPartSpec.Orientation orientation) {
		propertiesFromFile.put("orientation", orientation.toString());
		return propertiesFromFile;
	}
	
}
