package com.abocalypse.constructionflower.client;

import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChunkCoordinates;
import net.minecraftforge.common.MinecraftForge;

import com.abocalypse.constructionflower.CommonProxy;
import com.abocalypse.constructionflower.ConstructionFlower;
import com.abocalypse.constructionflower.blocks.ModBlocks;
import com.abocalypse.constructionflower.client.gui.GuiHeaderFont;
import com.abocalypse.constructionflower.client.gui.GuiLoadPlan;
import com.abocalypse.constructionflower.client.gui.GuiManagePlans;
import com.abocalypse.constructionflower.client.renderer.RenderConstructionFlower;
import com.abocalypse.constructionflower.event.ClientEventListener;
import com.abocalypse.constructionflower.plan.BlockXZCoords;
import com.abocalypse.constructionflower.plan.WorldPlanRegistry;

import cpw.mods.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {

	@Override
    public void preInit() {
		ConstructionFlower.constructionFlowerRenderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(ConstructionFlower.constructionFlowerRenderId, new RenderConstructionFlower());
        // register listeners for events that include client classes
        // currently just the one for GuiOpenEvent
    	MinecraftForge.EVENT_BUS.register(new ClientEventListener());
    	// this actually could be done on the server without a crash,
    	// but to no purpose
    	GuiHeaderFont.init();
    }
	
	// These need to be done here. The various message classes need
	// to be available on the server, so they can be registered there,
	// so including any client code in them (even if it was never
	// executed) would cause a dedicated server to crash on startup.
	//
	// http://www.minecraftforge.net/forum/index.php/topic,26086.msg133053.html#msg133053
	public void clientOnMessageForLoadedPlansMessage(Map<String, WorldPlanRegistry.PlanInfo> loadedPlans) {
		Minecraft mc = Minecraft.getMinecraft();
		mc.displayGuiScreen(new GuiManagePlans(null, loadedPlans));
	}
	public void onMessageForOpenGuiLoadPlanMessage(List<String> planSpecFiles, Map<String, BlockXZCoords> existingPlans) {
		Minecraft mc = Minecraft.getMinecraft();
		mc.displayGuiScreen(new GuiLoadPlan(null, false, planSpecFiles, existingPlans));
	}
	public void onMessageForSpawnedOntoBlocksMessage(List<ChunkCoordinates> spawnedOnto) {
		for ( ChunkCoordinates block : spawnedOnto) {
				Minecraft.getMinecraft().theWorld.setBlock(block.posX, block.posY, block.posZ, ModBlocks.constructionFlower);
		}
	}


}
