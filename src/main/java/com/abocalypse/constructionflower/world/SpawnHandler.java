package com.abocalypse.constructionflower.world;

import java.util.List;
import java.util.Random;

import com.abocalypse.constructionflower.blocks.BlockConstructionFlower;
import com.abocalypse.constructionflower.blocks.ModBlocks;
import com.abocalypse.constructionflower.lib.ConfigHandler;
import com.abocalypse.constructionflower.plan.ChunkBlocks.BlockMember;
import com.abocalypse.constructionflower.plan.EnumConstructionFlowerLevel;
import com.abocalypse.constructionflower.plan.WorldPlanRegistry;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

public class SpawnHandler {
	
	public static void spawnOntoChunk(World world, ChunkCoordIntPair chunk, Random random) {

		spawnOntoPlanChunk(world, chunk, random, null, null);

	}
	
	public static void spawnOntoPlanChunk(World world, ChunkCoordIntPair chunk, Random random, String planName, List<ChunkCoordinates> spawnedOnto) {
		// this check shouldn't be necessary because only called from contexts where the
		// check has already happened?
		if ( !world.isRemote) {
			
			WorldPlanRegistry registry = WorldPlanRegistry.get(world);
			if (registry.initialized()) {

				if (registry.containsChunk(planName, chunk,
						EnumConstructionFlowerLevel.SPAWN)) {

					List<BlockMember> randomBlocks = WorldPlanRegistry.get(world).randomSpawnBlocksFromChunk(chunk, ConfigHandler.chancesToSpawn, random);

					for (BlockMember block : randomBlocks) {

						if (ConfigHandler.percentSpawn == 100
								|| random.nextInt(100) < ConfigHandler.percentSpawn) {

							int y = world.getHeightValue(block.x(), block.z());
							if (((BlockConstructionFlower) (ModBlocks.constructionFlower)).canPlaceBlockAt(world, block.x(), y, block.z())) {
								world.setBlock(block.x(), y, block.z(), ModBlocks.constructionFlower, 0, 0);
								if ( spawnedOnto != null ) {
									spawnedOnto.add(new ChunkCoordinates(block.x(), y, block.z()));
								}
							}

						}
					}
				}

			} else {

				registry.staleChunk(chunk);

			}
			
		}

	}
	
	public static void spawnOntoIfStale(World world, ChunkCoordIntPair chunk, Random random) {
		
		if ( !world.isRemote ) {

			WorldPlanRegistry registry = WorldPlanRegistry.get(world);
			if (registry != null && registry.initialized() && !registry.unstaling() && registry.isStale(chunk)) {
				spawnOntoChunk(world, chunk, random);
				registry.unstaleChunk(chunk);
			}
		}
		

	}
	
	public static void spreadFromBlock(World world, int x, int y, int z, Random random) {
		
		if ( !world.isRemote ) {

			WorldPlanRegistry registry = WorldPlanRegistry.get(world);
			if ( !registry.containsBlock(x, z) ) {
				return; // TODO maybe should throw an exception instead?
			}

			for (BlockMember block : registry.randomNeighborsOf(x, z, ConfigHandler.chancesToSpawn, random)) {
				int xSpread = block.x();
				int zSpread = block.z();
				int ySpread = world.getHeightValue(xSpread, zSpread);
				boolean gradeOK = true;
				if (ConfigHandler.maxSpreadGrade > 0) {
					int grade = Math.abs(ySpread - y) / (Math.max(Math.abs(xSpread - x), Math.abs(zSpread - z))); // close enough
					gradeOK = grade <= ConfigHandler.maxSpreadGrade;
				}

				if ( gradeOK && ModBlocks.constructionFlower.canBlockStay(world, xSpread, ySpread, zSpread) ) {
					if ( ConfigHandler.maxSpreadAdjacent > 0 && ConfigHandler.maxSpreadAdjacent < 8 ) {
						// seems like there must be a better way to do the following
						int adjacent = 0;
						for ( int adjacentX = xSpread - 1; adjacentX < xSpread + 2; adjacentX++ ) {
							for ( int adjacentZ = zSpread - 1; adjacentZ < zSpread + 2; adjacentZ++ ) {
								if ( adjacentX != xSpread || adjacentZ != zSpread ) {
									int adjacentY = world.getHeightValue(adjacentX, adjacentZ);
									if (world.getBlock(adjacentX, adjacentY, adjacentZ).equals(ModBlocks.constructionFlower)) {
										adjacent++;
										if (adjacent > ConfigHandler.maxSpreadAdjacent) {
											return;
										}
									}
								}
							}
						}
					}
					world.setBlock(xSpread, ySpread, zSpread, ModBlocks.constructionFlower, 0, 3);
				}
			}
		}
		

	}

}
