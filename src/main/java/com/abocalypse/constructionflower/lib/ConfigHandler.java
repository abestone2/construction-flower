package com.abocalypse.constructionflower.lib;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class ConfigHandler {
	
	private static int chancesToSpawn;
	private static int percentSpawn;
	
	public static void init(File configFile) {
		
		Configuration config = new Configuration(configFile);
		config.load();

		chancesToSpawn = config.get("Generation", "Spots to try spawning per chunk", 5).getInt(5);
		percentSpawn = config.get("Generation", "Percent chance to spawn in each viable spot tried", 100).getInt(100);
		
		if( config.hasChanged() ) {
			config.save();
		}
		
	}
	
	public static int getChancesToSpawn() {
		return chancesToSpawn;
	}
	
	public static int getPercentSpawn() {
		return percentSpawn;
	}

}
