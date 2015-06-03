package com.abocalypse.constructionflower;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;

import com.abocalypse.constructionflower.blocks.ModBlocks;
import com.abocalypse.constructionflower.command.LoadPlanCommand;
import com.abocalypse.constructionflower.command.ManagePlansCommand;
import com.abocalypse.constructionflower.event.EventListener;
import com.abocalypse.constructionflower.lib.ConfigHandler;
import com.abocalypse.constructionflower.lib.Constants;
import com.abocalypse.constructionflower.network.LoadPlanMessage;
import com.abocalypse.constructionflower.network.LoadedPlansMessage;
import com.abocalypse.constructionflower.network.SpawnedOntoBlocksMessage;
import com.abocalypse.constructionflower.world.ConstructionFlowerWorldGen;

@Mod(modid = Constants.MODID, name = Constants.MODNAME, version = Constants.VERSION)
public class ConstructionFlower {

	@Mod.Instance
	public static ConstructionFlower instance;
	
	public SimpleNetworkWrapper network;
	
    // Says where the client and server 'proxy' code is loaded.
    @SidedProxy(clientSide="com.abocalypse.constructionflower.client.ClientProxy",
	    serverSide="com.abocalypse.constructionflower.CommonProxy")
    public static CommonProxy proxy;
	
	public static int constructionFlowerRenderId;
	
	public static Logger logger = LogManager.getLogger(Constants.MODID);
			
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	
    	ConfigHandler.init(event.getSuggestedConfigurationFile(), new File(event.getModConfigurationDirectory(), Constants.MODPATH));
    	ModBlocks.preInit();
  		MinecraftForge.EVENT_BUS.register(new EventListener());
  		proxy.preInit();

    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
    	
    	GameRegistry.registerWorldGenerator(new ConstructionFlowerWorldGen(), 20);
    	
     	network = new SimpleNetworkWrapper(Constants.MODID + "_Channel");
     	network.registerMessage(LoadPlanMessage.Handler.class, LoadPlanMessage.class, 2, Side.SERVER);
    	network.registerMessage(LoadedPlansMessage.ServerHandler.class, LoadedPlansMessage.class, 5, Side.SERVER);
    	proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }

    

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
    	event.registerServerCommand(new LoadPlanCommand());
    	event.registerServerCommand(new ManagePlansCommand());
    }


}
