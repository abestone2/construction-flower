package com.abocalypse.constructionflower.plan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SubPlanSpec extends PlanPartSpec {

		private static class PlanPartInfo {
		public final int deltaX;
		public final int deltaZ;
		public final PlanPartSpec spec;
		
		public PlanPartInfo(int deltaX, int deltaZ, PlanPartSpec spec) {
			this.deltaX = deltaX;
			this.deltaZ = deltaZ;
			this.spec = spec;
		}
	}
	
	private ArrayList<PlanPartInfo> parts;
	
	@SuppressWarnings("unchecked")
	public SubPlanSpec(Map<String,Object> properties) {
		super(properties);
		parts = new ArrayList<PlanPartInfo>();
		for ( Object part : (JSONArray)(properties.get("parts")) ) {
			EnumConstructionFlowerPlanPartType partType = EnumConstructionFlowerPlanPartType.valueOf((String)(((JSONObject)part).getOrDefault("type", "plan")));
			int deltaX = new Integer(((Long)(((JSONObject)part).getOrDefault("delta x", 0L))).intValue());
			int deltaZ = new Integer(((Long)(((JSONObject)part).getOrDefault("delta z", 0L))).intValue());
			JSONObject partProperties = (JSONObject)((JSONObject)part).get("plan");

			PlanPartSpec spec = null;
			switch (partType) {
			case IMAGE :
				spec = new ImagePlanPartSpec(partProperties);
				break;
			case PLAN :
				spec = new SubPlanSpec(partProperties);
				break;
			default :
				throw new IllegalArgumentException("Unknown part type.");
			}
			
			parts.add(new PlanPartInfo(deltaX, deltaZ, spec));
		}
	}

	@Override
	protected void doLoad(int worldX, int worldZ,
			ChunkPlanBlocks chunkPlanBlocks) throws IOException {

		for ( PlanPartInfo part : parts ) {
			part.spec.load(x(part.deltaX, part.deltaZ), z(part.deltaX, part.deltaZ), chunkPlanBlocks);
		}
		
	}


}
