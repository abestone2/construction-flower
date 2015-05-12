package com.abocalypse.constructionflower.lib;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class ConfigHandler {
	
	public static int chancesToSpawn;
	public static int percentSpawn;
	
	public static int maxSpawnsSentToClient;

	public static int chancesToSpread;
	public static int horizontalSpreadDistance;
	public static int spreadSquareSide;
	public static int maxSpreadGrade;
	public static int maxSpreadAdjacent;
	
	public static File planDir;
	public static File imageDir;
	
	public static void init(File configFile, File configDir) {
		
		Configuration config = new Configuration(configFile);
		config.load();

		chancesToSpawn = config.get("Generation", "Spots to try spawning per chunk", 5).getInt(5);
		percentSpawn = config.get("Generation", "Percent chance to spawn in each viable spot tried", 100).getInt(100);
		maxSpawnsSentToClient = config.get("Load", "Maximum spawns to sent to the ciient when a plan is loaded or moved", 100).getInt(100);
		
		int spreadDensity = config.get("Spread", "Percent of blocks within spread area onto which we will try to spread", 20).getInt(20);
		int h = config.get("Spread", "Maximum north/south or east/west distance we will try to spread", 2).getInt(2);
		// the horizontal spread distance must be positive (to turn spreading off completely,
		// use spreadDensity), and must be less than half a chunk (to keep bookkeeping simple)
		if ( h <= 0 ) {
			h = 1;
		} else if ( h > 8 ) {
			h = 8;
		}
		horizontalSpreadDistance = h;
		maxSpreadGrade = config.get("Spread", "Maximum grade (vertical distance/horizontal distance) up which we will try to spread", 2).getInt(2);
		maxSpreadAdjacent = config.get("Spread", "Do not spread onto a block if it already has more construction flower neighbors than this", 2).getInt(2);
		
		spreadSquareSide = 2*horizontalSpreadDistance + 1;
		chancesToSpread = spreadDensity*spreadSquareSide*spreadSquareSide/100;
		
		planDir = new File(configDir, Constants.PLAN_DIR);
		imageDir = new File(configDir, Constants.IMAGE_DIR);
		
		if( config.hasChanged() ) {
			config.save();
		}
		
	}
	
}
