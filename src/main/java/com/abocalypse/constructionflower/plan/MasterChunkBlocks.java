package com.abocalypse.constructionflower.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.abocalypse.constructionflower.lib.ConfigHandler;

import net.minecraft.world.ChunkCoordIntPair;

/* In the master ChunkBlocks only, we keep track of all the blocks from which it is
   possible to spread from a given block (its "neighbors"), stored in the private field
   neighborMap.
    
   The neighbors are found (by a call to findNeighbors) after everything has finished
   loading in. To make that calculation easy, everything in the spreadChunkBlockMap is
   stored in sorted order. That way we can loop through from northwest to southeast and
   be sure to miss nothing. */

public class MasterChunkBlocks extends ChunkBlocks {
	
	// these SortedMap's shadows the Map of the superclass (but the constructor
	// ensures that both are references to the same underlying TreeMap)
 	protected SortedMap<ChunkCoordIntPair,IBlockSet> spreadChunkBlockMap;
 	protected SortedMap<ChunkCoordIntPair,IBlockSet> spawnChunkBlockMap;

 	// this is filled in or updated only by findNeighbors()
	private Map<BlockMember, Set<BlockMember>> neighborMap;
 	
	// lexical order comparator for ordering chunks	
	private final static Comparator<? super ChunkCoordIntPair> comparator = new Comparator<ChunkCoordIntPair>() {

		@Override
		public int compare(ChunkCoordIntPair c1, ChunkCoordIntPair c2) {
			if ( c1.chunkZPos < c2.chunkZPos ) {
				return -1;
			} else if ( c1.chunkZPos > c2.chunkZPos ) {
				return 1;
			} else if ( c1.chunkXPos < c2.chunkXPos ) {
				return -1;
			} else if ( c1.chunkXPos > c2.chunkXPos ) {
				return 1;
			} else {
				return 0;
			}

		}
				
	};
		
	public boolean adjacentChunksToNorth(ChunkCoordIntPair c) {
		int x = c.chunkXPos;
		int z = c.chunkZPos;
		return this.spreadChunkBlockMap.containsKey(new ChunkCoordIntPair(x-1, z-1)) ||
				this.spreadChunkBlockMap.containsKey(new ChunkCoordIntPair(x, z-1)) ||
				this.spreadChunkBlockMap.containsKey(new ChunkCoordIntPair(x+1, z-1));
	}
		
	public boolean adjacentChunksToSouth(ChunkCoordIntPair c) {
		int x = c.chunkXPos;
		int z = c.chunkZPos;
		return this.spreadChunkBlockMap.containsKey(new ChunkCoordIntPair(x-1, z+1)) ||
				this.spreadChunkBlockMap.containsKey(new ChunkCoordIntPair(x, z+1)) ||
				this.spreadChunkBlockMap.containsKey(new ChunkCoordIntPair(x+1, z+1));
	}

	
	public MasterChunkBlocks() {
	    super.spawnChunkBlockMap = this.spawnChunkBlockMap = new TreeMap<ChunkCoordIntPair, IBlockSet>(comparator);
	    super.spreadChunkBlockMap = this.spreadChunkBlockMap = new TreeMap<ChunkCoordIntPair,IBlockSet>(comparator);
	}
	

	private static final Comparator<? super BlockMember> blockComparator =

			new Comparator<BlockMember>() {

				@Override
				public int compare(BlockMember c1, BlockMember c2) {
					if ( c1.z() < c2.z() ) {
						return -1;
					} else if ( c1.z() > c2.z() ) {
						return 1;
					} else if ( c1.x() < c2.x() ) {
						return -1;
					} else if ( c1.x() > c2.x() ) {
						return 1;
					} else {
						return 0;
					}

				}
				
			};


	
	// IBlockSet implemented as a TreeSet, supplying a Comparator for BlockMember's
	// (again, in lexical order).
	@SuppressWarnings("serial")
	public static class BlockSet extends TreeSet<BlockMember> implements IBlockSet {
    	
    	public BlockSet() {
			super(blockComparator);
		}

    	public BlockSet(IBlockSet blockSet) {
			super(blockComparator);
			this.addAll(blockSet);
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
    	@Override
    	public List<BlockMember> getRandom(int n, Random random) {
    		n = Math.min(n, this.size());
	    	List<BlockMember> list = new LinkedList<BlockMember>(this);
	    	Collections.shuffle(list, random);
	    	return new ArrayList<BlockMember>(list.subList(0, n));
    	}

			
    }
		
    @Override
    public IBlockSet createBlockSet() {
    	return new BlockSet();
    }
		
    @Override
    public IBlockSet createBlockSet(IBlockSet blocks) {
    	return new BlockSet(blocks);
    }
	
    public List<BlockMember> randomNeighborsOf(int x, int z, int n, Random random) {
    	BlockMember block = new BlockMember(x, z);
    	if ( this.neighborMap.containsKey(block) ) {
    		List<BlockMember> list = new LinkedList<BlockMember>(this.neighborMap.get(block));
	    	n = Math.min(n, list.size());
    		java.util.Collections.shuffle(list, random);
    		return new ArrayList<BlockMember>(list.subList(0, n));
    	} else {
    		// TODO or should throw exception?
    		return new ArrayList<BlockMember>();
    	}
    }
    
    private void neighbors(BlockMember b1, BlockMember b2) {
    	neighborMap.get(b1).add(b2);
    	neighborMap.get(b2).add(b1);
    }
    
    public void findNeighbors() {
    	
    	// TODO It should not be necessary to recheck everything no matter how
    	// little has changed.
    	neighborMap = new HashMap<BlockMember, Set<BlockMember>>();
    	if ( this.spreadChunkBlockMap.size() == 0 ) {
    		return;
    	}
			
    	int oldChunkZPos = this.spreadChunkBlockMap.firstKey().chunkZPos;
    	
    	int marginNorthAndWest = ConfigHandler.horizontalSpreadDistance;
    	int marginSouthAndEast = 16 - ConfigHandler.horizontalSpreadDistance;
 
    	Set<BlockMember> oldEastBlocks = new HashSet<BlockMember>();
    	Set<BlockMember> oldSouthBlocks = new HashSet<BlockMember>();
    	Set<BlockMember> newEastBlocks = new HashSet<BlockMember>();
    	Set<BlockMember> newSouthBlocks = new HashSet<BlockMember>();
    	
    	for ( ChunkCoordIntPair chunk : this.spreadChunkBlockMap.keySet() ) {
 
    		boolean adjacentChunkToNorth = this.adjacentChunksToNorth(chunk);
    		boolean adjacentChunkToWest = this.spreadChunkBlockMap.containsKey(new ChunkCoordIntPair(chunk.chunkXPos - 1, chunk.chunkZPos)); 
    		boolean adjacentChunkToEast = this.spreadChunkBlockMap.containsKey(new ChunkCoordIntPair(chunk.chunkXPos + 1, chunk.chunkZPos));
    		boolean adjacentChunkToSouth = this.adjacentChunksToSouth(chunk);

    		
    		// These keep track of whether we need to check for neighbors in
    		// chunks to our north and/or west, i.e. chunks previously processed (since
    		// we are looping from northwest to southeast).
    		// If there are immediately adjacent chunks to the north, northRows will
    		// be set true to begin with, then turn false as we get farther south. Otherwise
    		// it will just stay false. Similarly, mutatis mutandis, for westColumns.
    		// (Note that the diagonally adjacent chunks are considered as north and south,
    		// not as west and east.)
    		boolean northRows = adjacentChunkToNorth;
    		boolean westColumns = adjacentChunkToWest;

    		// These keep track of whether we should start storing up information to pass
    		// on the chunks on our east and south (if any). They start out false and
    		// eventually change to true if there are such chunks.
    		boolean eastColumns = false;
    		boolean southRows = false;
    		
       		if ( chunk.chunkZPos > oldChunkZPos ) { // starting a new row of chunks
    			if ( adjacentChunkToEast ) {
    				newEastBlocks = new HashSet<BlockMember>();
    			}
    			if ( chunk.chunkZPos == oldChunkZPos + 1 ) {
    				oldSouthBlocks = new HashSet<BlockMember>(newSouthBlocks);
    			}
    			newSouthBlocks = new HashSet<BlockMember>();
    			oldChunkZPos = chunk.chunkZPos;
    		} else {                               // continuing on the same row of chunks
    			if ( adjacentChunkToWest ) {
    				oldEastBlocks = new HashSet<BlockMember>(newEastBlocks);
    			}
    			if ( adjacentChunkToEast ) {
    				newEastBlocks = new HashSet<BlockMember>();
    			}
    		}
 
    		BlockSet blocks = (BlockSet)(this.spreadChunkBlockMap.get(chunk));
    		int westEdge = chunk.chunkXPos << 4;
    		int northEdge = chunk.chunkZPos << 4;
    		int eastEdge = westEdge + 15;
    		int southEdge = northEdge + 15;
    		
    		for ( BlockMember  block : blocks ) {
    			
    			// Sometimes the entry for this block has already been created, when it
    			// was recognized as the neighbor of a previous block; if not, we create
    			// it now.
    			if ( !neighborMap.containsKey(block) ) {
    				this.neighborMap.put(block, new HashSet<BlockMember>());
    			}
    			
    			int xRelativeToChunk = block.x() - westEdge;
    			int zRelativeToChunk = block.z() - northEdge;
    			
    			if ( adjacentChunkToNorth && zRelativeToChunk > marginNorthAndWest ) {
    				northRows = false;
    			}
    			if ( adjacentChunkToSouth && zRelativeToChunk >= marginSouthAndEast ) {
    					southRows = true;
    			}
    			if ( xRelativeToChunk <= marginNorthAndWest ) {
    				westColumns = adjacentChunkToWest;
    				eastColumns = false;
    			} else {
    				eastColumns = false;
    				if ( xRelativeToChunk >= marginSouthAndEast ) {
    					eastColumns = adjacentChunkToEast;
    				}
    			}
    			
    			if ( southRows ) {
    				newSouthBlocks.add(block);
    			}
    			if ( eastColumns ) {
    				newEastBlocks.add(block);
    			}

    			// the chunks to our north and west (if any) may have left blocks for
    			// us to process

    			int eastLimit = Math.min(eastEdge, block.x() + ConfigHandler.horizontalSpreadDistance);
    			int southLimit = Math.min(southEdge, block.z() + ConfigHandler.horizontalSpreadDistance);
    			int northLimit = Math.max(northEdge, block.z() - ConfigHandler.horizontalSpreadDistance);
    			
    			if ( northRows ) {
    				for ( int neighborZ = block.z() - ConfigHandler.horizontalSpreadDistance; neighborZ < northEdge; ++neighborZ) {
    					for ( int neighborX = block.x() - ConfigHandler.horizontalSpreadDistance; neighborX <= block.x(); ++neighborX ) {
    						BlockMember neighbor = new BlockMember(neighborX, neighborZ);
    						if ( oldSouthBlocks.contains(neighbor) ) {
    							this.neighbors(block, neighbor);
    						}
    					}
    				}
    			}

    			if ( westColumns ) {
    				for ( int neighborZ = northLimit; neighborZ <= southLimit; ++neighborZ) {
    					for ( int neighborX = block.x() - ConfigHandler.horizontalSpreadDistance; neighborX < westEdge ; ++neighborX ) {
    						BlockMember neighbor = new BlockMember(neighborX, neighborZ);
    						if ( oldEastBlocks.contains(neighbor) ) {
    							this.neighbors(block, neighbor);
    						}
    					}
    				}
    			}

    			// Blocks north and west of us in *this* chunk have already been processed;
    			// look only at those south and east of us.
          			
    			// first the blocks to the east of us, on our row
    			for ( int neighborX = block.x() + 1; neighborX <= eastLimit; ++neighborX ) {
    				BlockMember neighbor = new BlockMember(neighborX, block.z());
    				if ( blocks.contains(neighbor) ) {
    					if ( !neighborMap.containsKey(neighbor) ) {
    						neighborMap.put(neighbor, new HashSet<BlockMember>());
    					}
    					this.neighbors(block, neighbor);
    				}
    			}
    			
    			// then the blocks on the rows to the south of us
    			for ( int neighborZ = block.z() + 1; neighborZ <= southLimit; ++neighborZ ) {
    				for ( int neighborX = block.x(); neighborX <= eastLimit; ++neighborX ) {
    					BlockMember neighbor = new BlockMember(neighborX, neighborZ);
    					if ( blocks.contains(neighbor) ) {
    						if ( !neighborMap.containsKey(neighbor) ) {
    								neighborMap.put(neighbor, new HashSet<BlockMember>());
    							}
    							this.neighbors(block, neighbor);
    						}
    					}
    				}
    			}

    		}

			
    }

	@Override
	protected Map<ChunkCoordIntPair, IBlockSet> createMap() {
		return new TreeMap<ChunkCoordIntPair, IBlockSet>();
	}
		
}
