package com.abocalypse.constructionflower.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import net.minecraft.world.ChunkCoordIntPair;

public class ChunkPlanBlocks extends ChunkBlocks {

	public ChunkPlanBlocks() {
		this.spawnChunkBlockMap = new HashMap<ChunkCoordIntPair,IBlockSet>();
		this.spreadChunkBlockMap = new HashMap<ChunkCoordIntPair,IBlockSet>();
	}
	
	@SuppressWarnings("serial")
	protected static class BlockSet extends HashSet<BlockMember> implements IBlockSet {

		public BlockSet() {
			super();
		}

		public BlockSet(IBlockSet blockSet) {
			super(blockSet);
		}

		// This is the method is called by ChunkBlocks.randomSpawnBlocksFromChunk(),
    	// which in turn is called by WorldPlanRegistry.randomSpawnBlocksFromChunk(),
    	// which in turn is called by SpawnHandler.spawnOntoChunk() when it needs some
    	// random blocks to spawn onto.
    	//
		// Can't figure out how to avoid writing it twice (in ChunkPlanBlocks
    	// and here). :(
		//
		// Based on https://stackoverflow.com/questions/7191325/get-a-random-subset-from-a-result-set-in-java
		public List<BlockMember> getRandom(int n, Random random) {
			n = Math.min(n, this.size());
			List<BlockMember> list = new LinkedList<BlockMember>(this);
			java.util.Collections.shuffle(list, random);
			return new ArrayList<BlockMember>(list.subList(0, n));
		}

	}

	@Override
	protected IBlockSet createBlockSet() {
		return new BlockSet();
	}
	
	@Override
	protected IBlockSet createBlockSet(IBlockSet blocks) {
		return new BlockSet(blocks);
	}
	
}
