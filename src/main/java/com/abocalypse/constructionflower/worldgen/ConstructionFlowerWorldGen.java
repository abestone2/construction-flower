package com.abocalypse.constructionflower.worldgen;

import java.util.ArrayList;
import java.util.Random;

import com.abocalypse.constructionflower.blocks.BlockConstructionFlower;
import com.abocalypse.constructionflower.blocks.ModBlocks;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import cpw.mods.fml.common.IWorldGenerator;

public class ConstructionFlowerWorldGen  implements IWorldGenerator {

	private int chancesToSpawn;
	private int inverseDensity;
	
	public ConstructionFlowerWorldGen(int chances, int invDen) {
		this.chancesToSpawn = chances;
		this.inverseDensity = invDen;
	}
	
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world,
			IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		
		ArrayList<int[]> mappedBlocks = mappedBlocksForChunk(chunkX, chunkZ);
		int nMappedBlocks = mappedBlocks.size();
		int nChances = Math.min(this.chancesToSpawn, nMappedBlocks);

		for (int chance = 0; chance < nChances; chance++) {
			
			int[] xz = mappedBlocks.get(random.nextInt(nMappedBlocks));
	
			if (random.nextInt(this.inverseDensity) == 0) {

				int y = world.getHeightValue(xz[0], xz[1]);
				if (((BlockConstructionFlower)(ModBlocks.constructionFlower)).canBlockStay(world, xz[0], y, xz[1])) {
					world.setBlock(xz[0], y, xz[1], ModBlocks.constructionFlower, 0, 0);
				}
				
			}
		}
		
	}

	private static ArrayList<int[]> mappedBlocksForChunk(int chunkX, int chunkZ) {

		ArrayList<int[]> blocks = new ArrayList<int[]>();
		
		if (chunkZ % 5 == 0) {
			int xMin = chunkX * 16;
			int zMin = chunkZ * 16;
			for (int x = xMin; x < xMin + 16; ++x) {
				for (int z = zMin; z < zMin + 6; ++z) {
					blocks.add(new int[]{x, z});
				}
			}
		}
		
		return blocks;
	}
	

}
