package com.abocalypse.constructionflower.blocks;


import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
public final class ModBlocks {
	
	public static Block constructionFlower;
	
	public static void preInit() {
		
		// GameRegistry get a single instance of every block.
		constructionFlower = new BlockConstructionFlower();
		GameRegistry.registerBlock(constructionFlower, "constructionFlower");
		
	}

}
