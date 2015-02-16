package com.abocalypse.constructionflower;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

import com.abocalypse.constructionflower.blocks.ModBlocks;
import com.abocalypse.constructionflower.lib.Constants;

@Mod(modid = Constants.MODID, name = Constants.MODNAME, version = Constants.VERSION)
public class constructionflower {
	
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	
    	ModBlocks.preInit();

    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }


}
