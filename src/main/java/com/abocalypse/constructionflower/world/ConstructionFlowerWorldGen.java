package com.abocalypse.constructionflower.world;

import java.util.Random;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import cpw.mods.fml.common.IWorldGenerator;

public class ConstructionFlowerWorldGen implements IWorldGenerator {

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world,
			IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		
		ChunkCoordIntPair chunk = new ChunkCoordIntPair(chunkX, chunkZ);
		SpawnHandler.spawnOntoChunk(world, chunk, random);
		
	}

}
