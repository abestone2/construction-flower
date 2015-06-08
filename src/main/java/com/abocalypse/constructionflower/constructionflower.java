package com.abocalypse.constructionflower;

import java.io.File;

import net.minecraftforge.common.MinecraftForge;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
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
import com.abocalypse.constructionflower.world.ConstructionFlowerWorldGen;

@Mod(modid = Constants.MODID, name = Constants.MODNAME, version = Constants.VERSION)
public class ConstructionFlower {

	@Mod.Instance
	public static ConstructionFlower instance;
	
	public SimpleNetworkWrapper network;
	
    // Says where the client and server 'proxy' code is loaded.
	// See http://greyminecraftcoder.blogspot.com.au/2013/11/how-forge-starts-up-your-code.html
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
    	
    	// the register of plans (WorldPlanRegistry) is kept only on the
    	// server, whereas gui's are opened only on the client. So both of
    	// the commands, cfload and cfmanage, need to send messages back and
    	// forth. 
    	//
    	// The two message handlers on the server side (for learning
    	// what the player did with the gui) are registered here; the two
    	// for the client side (for tellin the gui what to display) are
    	// registered in the client proxy.
    	//
    	// Note the channel name can't be more than 20 characters or the
    	// dedicated server will crash.
    	// http://www.minecraftforum.net/forums/mapping-and-modding/minecraft-mods/modification-development/2171523-solved-the-received-string-length-is-longer-than?comment=11
    	network = new SimpleNetworkWrapper(StringUtils.right(Constants.MODID, 20));
     	network.registerMessage(LoadPlanMessage.Handler.class, LoadPlanMessage.class, 2, Side.SERVER);
    	network.registerMessage(LoadedPlansMessage.ServerHandler.class, LoadedPlansMessage.class, 5, Side.SERVER);

    	proxy.init();
    }

    // I believe this is primarily for the purpose of integrating
    // with other mods? Nothing to put here as of now.
    //@Mod.EventHandler
    //public void postInit(FMLPostInitializationEvent event) {}

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
    	event.registerServerCommand(new LoadPlanCommand());
    	event.registerServerCommand(new ManagePlansCommand());
    }


}
