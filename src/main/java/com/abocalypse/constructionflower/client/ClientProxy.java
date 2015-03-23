package com.abocalypse.constructionflower.client;

import com.abocalypse.constructionflower.CommonProxy;
import com.abocalypse.constructionflower.ConstructionFlower;
import com.abocalypse.constructionflower.client.renderer.RenderConstructionFlower;

import cpw.mods.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {

	@Override
    public void registerRenderers() {
		ConstructionFlower.constructionFlowerRenderId = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(ConstructionFlower.constructionFlowerRenderId, new RenderConstructionFlower());
    }

}
