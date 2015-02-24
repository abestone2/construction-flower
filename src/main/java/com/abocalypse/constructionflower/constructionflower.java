package com.abocalypse.constructionflower;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

import com.abocalypse.constructionflower.blocks.ModBlocks;
import com.abocalypse.constructionflower.lib.ConfigHandler;
import com.abocalypse.constructionflower.lib.Constants;
import com.abocalypse.constructionflower.worldgen.ConstructionFlowerWorldGen;

@Mod(modid = Constants.MODID, name = Constants.MODNAME, version = Constants.VERSION)
public class constructionflower {
	
    // Says where the client and server 'proxy' code is loaded.
    @SidedProxy(clientSide="com.abocalypse.constructionflower.client.ClientProxy",
	    serverSide="com.abocalypse.constructionflower.CommonProxy")
    public static CommonProxy proxy;
	
	public static int constructionFlowerRenderId;
	
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	
    	ConfigHandler.init(event.getSuggestedConfigurationFile());
    	ModBlocks.preInit();
    	proxy.registerRenderers();

    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
    	
    	GameRegistry.registerWorldGenerator(new ConstructionFlowerWorldGen(), 20);

    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }


}
