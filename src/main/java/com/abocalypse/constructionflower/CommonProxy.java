package com.abocalypse.constructionflower;

import java.util.List;
import java.util.Map;

import net.minecraft.util.ChunkCoordinates;

import com.abocalypse.constructionflower.plan.BlockXZCoords;
import com.abocalypse.constructionflower.plan.WorldPlanRegistry;

// This is just to tell a dedicated server to do
// nothing in various places where the client needs to
// do something.
public class CommonProxy {

	public void preInit() {}
	public void init() {}
	public void clientOnMessageForLoadedPlansMessage(Map<String, WorldPlanRegistry.PlanInfo> loadedPlans) {}
	public void onMessageForOpenGuiLoadPlanMessage(List<String> planSpecFiles, Map<String, BlockXZCoords> existingPlans) {}
	public void onMessageForSpawnedOntoBlocksMessage(List<ChunkCoordinates> spawnedOnto) {}
	
}
