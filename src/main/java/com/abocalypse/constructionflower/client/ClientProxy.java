package com.abocalypse.constructionflower.client;

import net.minecraftforge.common.MinecraftForge;

import com.abocalypse.constructionflower.CommonProxy;
import com.abocalypse.constructionflower.ConstructionFlower;
import com.abocalypse.constructionflower.client.gui.GuiHeaderFont;
import com.abocalypse.constructionflower.client.renderer.RenderConstructionFlower;
import com.abocalypse.constructionflower.event.ClientEventListener;
import com.abocalypse.constructionflower.network.LoadedPlansMessage;
import com.abocalypse.constructionflower.network.OpenGuiLoadPlanMessage;
import com.abocalypse.constructionflower.network.SpawnedOntoBlocksMessage;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;

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
	
	@Override
	public void init() {
		// because these classes include GuiScreen, they can't be
		// registered on the server
    	ConstructionFlower.instance.network.registerMessage(LoadedPlansMessage.ClientHandler.class, LoadedPlansMessage.class, 6, Side.CLIENT);
		ConstructionFlower.instance.network.registerMessage(OpenGuiLoadPlanMessage.Handler.class, OpenGuiLoadPlanMessage.class, 4, Side.CLIENT);
    	ConstructionFlower.instance.network.registerMessage(SpawnedOntoBlocksMessage.Handler.class, SpawnedOntoBlocksMessage.class, 7, Side.CLIENT);
	}

}
