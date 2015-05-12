package com.abocalypse.constructionflower.plan;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.primitives.Ints;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.ChunkCoordIntPair;

public abstract class ChunkBlocks {

	protected Map<ChunkCoordIntPair,IBlockSet> spawnChunkBlockMap;
	protected Map<ChunkCoordIntPair,IBlockSet> spreadChunkBlockMap;

	public static class BlockMember {
		private final int x;
		private final int z;
		
		public BlockMember(int x, int z) {
			this.x = x;
			this.z = z;
		}
		
		public int x() {
			return this.x;
		}
		
		public int z() {
			return this.z;
		}
		
		public ChunkCoordIntPair chunk() {
			return new ChunkCoordIntPair(this.x >> 4, this.z >> 4);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + z;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			BlockMember other = (BlockMember) obj;
			if (x != other.x)
				return false;
			if (z != other.z)
				return false;
			return true;
		}

	}


	protected abstract IBlockSet createBlockSet();

	protected abstract IBlockSet createBlockSet(IBlockSet blocks);
	
	protected abstract Map<ChunkCoordIntPair, IBlockSet> createMap();

	protected static interface IBlockSet extends Set<BlockMember> {
		public List<BlockMember> getRandom(int n, Random random);
	}
	
	
	// Note: it is up to the caller to ensure that (x, z) really is
	// inside the chunk in question. This method will happily add
	// any block to any chunk.
	public void addChunkBlock(ChunkCoordIntPair chunk, int x, int z, EnumConstructionFlowerLevel level) {
		BlockMember block = new BlockMember(x, z);
		// level SPAWN means you can spawn onto this block *and* you can spread onto it
		// (so spreadChunkBlockMap contains both SPREAD level and SPAWN level blocks, whereas
		//  spawnChunkBlockMap contains only the SPAWN level blocks)
		if ( !this.spreadChunkBlockMap.containsKey(chunk) ) {
			this.spreadChunkBlockMap.put(chunk, createBlockSet());
		}
		this.spreadChunkBlockMap.get(chunk).add(new BlockMember(x, z));
		if ( level == EnumConstructionFlowerLevel.SPAWN ) {
			if ( !this.spawnChunkBlockMap.containsKey(chunk) ) {
				this.spawnChunkBlockMap.put(chunk, createBlockSet());
			}
			this.spawnChunkBlockMap.get(chunk).add(new BlockMember(x, z));
			if ( this.spreadChunkBlockMap.containsKey(chunk) && this.spreadChunkBlockMap.get(chunk).contains(block)) {
				this.spreadChunkBlockMap.get(chunk).remove(block);
			}
		}
	}

	public void addChunkBlocks(ChunkCoordIntPair chunk, IBlockSet blocks, EnumConstructionFlowerLevel level) {
		if ( !this.spreadChunkBlockMap.containsKey(chunk) ) {
			this.spreadChunkBlockMap.put(chunk, createBlockSet());
		}
		this.spreadChunkBlockMap.get(chunk).addAll(blocks);
		if ( level == EnumConstructionFlowerLevel.SPAWN ) {
			if ( !this.spawnChunkBlockMap.containsKey(chunk) ) {
				this.spawnChunkBlockMap.put(chunk, createBlockSet());
			}
			this.spawnChunkBlockMap.get(chunk).addAll(blocks);
		}
	}
	
	public void removeChunk(ChunkCoordIntPair chunk) {
		if ( this.spreadChunkBlockMap.containsKey(chunk) ) {
			this.spreadChunkBlockMap.remove(chunk);
		}
		if ( this.spawnChunkBlockMap.containsKey(chunk) ) {
			this.spawnChunkBlockMap.remove(chunk);
		}
	}
	
	// Fails silently if the chunk is not in our map, or if the block
	// is not in the chunk's set. (Is that good?)
	public void removeChunkBlock(ChunkCoordIntPair chunk, int x, int z) {
		if ( this.spreadChunkBlockMap.containsKey(chunk) ) {
			BlockMember block = new BlockMember(x, z);
			IBlockSet set = this.spreadChunkBlockMap.get(chunk);
			if( set.contains(block) ) {
				set.remove(block);
				if ( set.size() == 0 ) {
					this.spreadChunkBlockMap.remove(chunk);
				}
			}
		} else if ( this.spawnChunkBlockMap.containsKey(chunk) ) {
			BlockMember block = new BlockMember(x, z);
			IBlockSet set = this.spawnChunkBlockMap.get(chunk);
			if( set.contains(block) ) {
				set.remove(block);
				if ( set.size() == 0 ) {
					this.spawnChunkBlockMap.remove(chunk);
				}
			}
		}

	}

	public boolean containsChunk(ChunkCoordIntPair chunk, EnumConstructionFlowerLevel level) {
		if ( level == EnumConstructionFlowerLevel.SPREAD ) {
			return spreadChunkBlockMap.containsKey(chunk);
		} else {
			return spawnChunkBlockMap.containsKey(chunk);
		}
	}
	
	public boolean containsBlock(int x, int z, EnumConstructionFlowerLevel level) {
		BlockMember block = new BlockMember(x, z);
		ChunkCoordIntPair chunk = block.chunk();
		if ( level == EnumConstructionFlowerLevel.SPREAD ) {
			return spreadChunkBlockMap.containsKey(chunk) && this.spreadChunkBlockMap.get(chunk).contains(block);
		} else {
			return spawnChunkBlockMap.containsKey(chunk) && this.spawnChunkBlockMap.get(chunk).contains(block);
		}

	}
	
	public Set<ChunkCoordIntPair> keySet(EnumConstructionFlowerLevel level) {
		if ( level == EnumConstructionFlowerLevel.SPREAD ) {
			return spreadChunkBlockMap.keySet();
		} else {
			return spawnChunkBlockMap.keySet();
		}
	}
	
	public IBlockSet get(ChunkCoordIntPair chunk, EnumConstructionFlowerLevel level) {
		IBlockSet ret = createBlockSet();
		if ( level == EnumConstructionFlowerLevel.SPREAD && spreadChunkBlockMap.containsKey(chunk) ) {
			ret.addAll( spreadChunkBlockMap.get(chunk));
		} else if ( spawnChunkBlockMap.containsKey(chunk) ) {
			ret.addAll( spawnChunkBlockMap.get(chunk) );
		}
		return ret;
	}
	
	List<BlockMember> randomSpawnBlocksFromChunk(ChunkCoordIntPair chunk, int n, Random random) {
		if (this.spawnChunkBlockMap.containsKey(chunk)) {
			return this.spawnChunkBlockMap.get(chunk).getRandom(n, random);
		} else {
			return new ArrayList<BlockMember>();
		}
	}
	
	public void move(WorldPlanRegistry.PlanPosition oldPosition, WorldPlanRegistry.PlanPosition newPosition) {
		this.spreadChunkBlockMap = move(this.spreadChunkBlockMap, oldPosition, newPosition);
		this.spawnChunkBlockMap = move(this.spawnChunkBlockMap, oldPosition, newPosition);
	}

	private Map<ChunkCoordIntPair, IBlockSet> move(Map<ChunkCoordIntPair, IBlockSet> chunkBlockMap, WorldPlanRegistry.PlanPosition oldPosition, WorldPlanRegistry.PlanPosition newPosition) {
		Rechunker rechunker = new Rechunker(oldPosition, newPosition);
		for ( Map.Entry<ChunkCoordIntPair, IBlockSet> entry : chunkBlockMap.entrySet() ) {
			rechunker.setChunk(entry.getKey());
			for ( BlockMember block : entry.getValue() ) {
				rechunker.rechunk(block);
			}
		}
		return rechunker.newMap();
	}
	
	
	private static enum ChunkBin {
		NORTHWEST, WEST, NORTH, SOUTHWEST, CENTER, NORTHEAST, SOUTH, EAST, SOUTHEAST
	}

	private class Rechunker {
		private final EnumSet<ChunkBin> chunkBins;
		private final EnumMap<ChunkBin, ChunkBinLimits> limits;
		private final Map<ChunkCoordIntPair, IBlockSet> newMap;
		private final int blockShiftEast;
		private final int blockShiftSouth;
		private final Transformer transformer;
		
		private BlockMember currentChunkNewNWCorner;
		private EnumMap<ChunkBin, ChunkCoordIntPair> chunks;
		
		public Rechunker(WorldPlanRegistry.PlanPosition oldPosition, WorldPlanRegistry.PlanPosition newPosition) {
			this.newMap = createMap();
			this.transformer = new Transformer(oldPosition, newPosition);
			this.chunkBins = EnumSet.noneOf(ChunkBin.class);
			this.limits = new EnumMap<ChunkBin, ChunkBinLimits>(ChunkBin.class);

			BlockMember oldNWCorner = chunkNWCorner(oldPosition.anchor.x, oldPosition.anchor.z);
			BlockMember newNWCorner = chunkNWCorner(newPosition.anchor.x, newPosition.anchor.z);
			int oldXEast = oldNWCorner.x() + 15;
			int oldZSouth = oldNWCorner.z() + 15;
			
			BlockMember transformedNWCorner = transformer.transform(oldNWCorner);
			BlockMember transformedNECorner = transformer.transform(new BlockMember(oldXEast, oldNWCorner.z()));
			BlockMember transformedSWCorner = transformer.transform(new BlockMember(oldNWCorner.x(), oldZSouth));
			BlockMember transformedSECorner = transformer.transform(new BlockMember(oldXEast, oldZSouth));
			int[] cornerX = {transformedNWCorner.x(), transformedNECorner.x(), transformedSWCorner.x(), transformedSECorner.x()};
			int[] cornerZ = {transformedNWCorner.z(), transformedNECorner.z(), transformedSWCorner.z(), transformedSECorner.z()};

			this.blockShiftEast = Ints.min(cornerX) - newNWCorner.x();
			this.blockShiftSouth = Ints.min(cornerZ) - newNWCorner.z();
			
			addBin(ChunkBin.CENTER);
			if ( blockShiftEast < 0 ) {
				addBin(ChunkBin.WEST);
			} else if ( blockShiftEast > 0 ) {
				addBin(ChunkBin.EAST);
			}
			if ( blockShiftSouth < 0 ) {
				addBin(ChunkBin.NORTH);
				if ( blockShiftEast < 0 ) {
					addBin(ChunkBin.NORTHWEST);
				} else if ( blockShiftEast > 0 ) {
					addBin(ChunkBin.NORTHEAST);
				}
			} else if ( blockShiftSouth > 0 ) {
				addBin(ChunkBin.SOUTH);
				if ( blockShiftEast < 0 ) {
					addBin(ChunkBin.SOUTHWEST);
				} else if ( blockShiftEast > 0 ) {
					addBin(ChunkBin.SOUTHEAST);
				}
			}
			
		}
		
		public void setChunk(ChunkCoordIntPair chunk) {
			ChunkCoordIntPair newChunk = transformer.transform(chunk);
			currentChunkNewNWCorner = new BlockMember(newChunk.chunkXPos << 4, newChunk.chunkZPos << 4);
			chunks = new EnumMap<ChunkBin, ChunkCoordIntPair>(ChunkBin.class);
			for ( ChunkBin bin : chunkBins ) {
				chunks.put(bin, chunkForBin(bin, chunk));
			}
		}
		
		public void rechunk(BlockMember block) {
			BlockMember newBlock = transformer.transform(block);
			BlockMember newBlockRelativeCoords = new BlockMember(newBlock.x() - currentChunkNewNWCorner.x(), newBlock.z() - currentChunkNewNWCorner.z());
			for ( ChunkBin bin : chunkBins ) {
				if ( limits.get(bin).isIn(newBlockRelativeCoords) ) {
					ChunkCoordIntPair chunk = chunks.get(bin);
					if ( !newMap.containsKey(chunk) ) {
						newMap.put(chunk, createBlockSet());
					}
					newMap.get(chunk).add(newBlock);
					break;
				}
			}
		}
		
		public Map<ChunkCoordIntPair, IBlockSet> newMap() {
			return newMap;
		}
		
		private BlockMember chunkNWCorner(int x, int z) {
			final int CHUNK_PART = ~(Integer.valueOf(0xF));
			return new BlockMember(x & CHUNK_PART, z & CHUNK_PART);
		}
		
		private ChunkCoordIntPair chunkForBin(ChunkBin bin, ChunkCoordIntPair chunk) {
			int x = transformer.transform(chunk).chunkXPos;
			int z = transformer.transform(chunk).chunkZPos;
			switch ( bin ) {
			case NORTHWEST : case WEST : case SOUTHWEST : x--; break;
			case NORTHEAST : case EAST : case SOUTHEAST : x++; break;
			case NORTH : case SOUTH : case CENTER : break;
			default:
				throw new RuntimeException("Unknown bin");
			}
			switch ( bin ) {
			case NORTHWEST : case NORTH : case NORTHEAST : z--; break;
			case SOUTHWEST : case SOUTH : case SOUTHEAST : z++; break;
			case EAST : case WEST : case CENTER : break;
			default:
				throw new RuntimeException("Unknown bin");
			}
			return new ChunkCoordIntPair(x, z);
		}
		
		private void addBin(ChunkBin bin) {
			chunkBins.add(bin);
			int xMin;
			int zMin;
			int xMax;
			int zMax;

			switch (bin) {
			
			case NORTHWEST : case WEST : case SOUTHWEST :
				xMin = blockShiftEast;
				xMax = 0;
				break;

			case NORTH : case CENTER : case SOUTH :
				xMin = 0;
				xMax = 16;
				break;
				
			case NORTHEAST : case EAST : case SOUTHEAST :
				xMin = 16;
				xMax = 16 + blockShiftEast;
				break;
				
			default:
				throw new RuntimeException("Unexpected value for ChunkBin");
					
			}

			switch (bin) {
			
			case NORTHWEST : case NORTH : case NORTHEAST :
				zMin = blockShiftSouth;
				zMax = 0;
				break;

			case WEST : case CENTER : case EAST :
				zMin = 0;
				zMax = 16;
				break;
				
			case SOUTHWEST : case SOUTH : case SOUTHEAST :
				zMin = 16;
				zMax = 16 + blockShiftSouth;
				break;
				
			default:
				throw new RuntimeException("Unexpected value for ChunkBin");
					
			}

			this.limits.put(bin, new ChunkBinLimits(xMin, zMin, xMax, zMax));
		}
		
		private class ChunkBinLimits {
			private final int xMin;
			private final int zMin;
			private final int xMax;
			private final int zMax;
			public ChunkBinLimits(int xMin, int zMin, int xMax, int zMax) {
				this.xMin = xMin;
				this.zMin = zMin;
				this.xMax = xMax;
				this.zMax = zMax;
			}
			public boolean isIn(BlockMember block) {
				return (block.x() >= xMin) && (block.x() < xMax) && (block.z() >= zMin) && (block.z() < zMax);
			}
		}
	}
	
	
	private static class Transformer {

		private final int fullCos;
		private final int fullSin;
		private final BlockMember oldAnchor;
		private final BlockMember newAnchor;
		private final ChunkCoordIntPair oldAnchorChunk;
		private final ChunkCoordIntPair newAnchorChunk;

		public Transformer (WorldPlanRegistry.PlanPosition oldPosition, WorldPlanRegistry.PlanPosition newPosition) {
			int oldCos = getCos(oldPosition.orientation);
			int oldSin = getSin(oldPosition.orientation);
			int newCos = getCos(newPosition.orientation);
			int newSin = getSin(newPosition.orientation);
			this.fullCos = oldCos*newCos + oldSin*newSin;
			this.fullSin = newCos*oldSin - oldCos*newSin;
			this.oldAnchor = new BlockMember(oldPosition.anchor.x, oldPosition.anchor.z);
			this.newAnchor = new BlockMember(newPosition.anchor.x, newPosition.anchor.z);
			this.oldAnchorChunk = this.oldAnchor.chunk();
			this.newAnchorChunk = this.newAnchor.chunk();
		}
		
		public BlockMember transform(BlockMember b) {
			int deltaX = b.x() - oldAnchor.x();
			int deltaZ = b.z() - oldAnchor.z();
			int x = rotateX(deltaX, deltaZ) + newAnchor.x();
			int z = rotateZ(deltaX, deltaZ) + newAnchor.z();
			return new BlockMember(x, z);
		}
		public ChunkCoordIntPair transform(ChunkCoordIntPair c) {
			int deltaX = c.chunkXPos - oldAnchorChunk.chunkXPos;
			int deltaZ = c.chunkZPos - oldAnchorChunk.chunkZPos;
			int x = rotateX(deltaX, deltaZ) + newAnchorChunk.chunkXPos;
			int z = rotateZ(deltaX, deltaZ) + newAnchorChunk.chunkZPos;
			return new ChunkCoordIntPair(x, z);
		}
		
		// rotation matrix is
		//  [  cos  -sin  ]
		//  [  sin   cos  ]
		private int rotateX(int x, int z) {
			return fullCos*x - fullSin*z;
		}
		private int rotateZ(int x, int z) {
			return fullSin*x + fullCos*z;
		}
		
		private int getCos(PlanPartSpec.Orientation orientation) {
			switch( orientation ) {
			case TOPNORTH : return 1;
			case TOPEAST : case TOPWEST : return 0;
			case TOPSOUTH : return -1;
			default: throw new RuntimeException("Unknown orientation");
			}
		}
	
		private int getSin(PlanPartSpec.Orientation orientation) {
			switch( orientation ) {
			case TOPNORTH : case TOPSOUTH : return 0;
			case TOPEAST : return -1;
			case TOPWEST : return 1;
			default: throw new RuntimeException("Unknown orientation");
			}
		}
	}
	

	
	private NBTTagList getNBTTagList(EnumConstructionFlowerLevel level) {
		Map<ChunkCoordIntPair, IBlockSet> map;
		if ( level == EnumConstructionFlowerLevel.SPAWN ) {
			map = this.spreadChunkBlockMap;
		} else {
			map = this.spawnChunkBlockMap;
		}
		NBTTagList list = new NBTTagList();
		for ( ChunkCoordIntPair chunk : map.keySet() ) {
			NBTTagCompound chunkAndBlockSet = new NBTTagCompound();
			chunkAndBlockSet.setInteger("x position", chunk.chunkXPos);
			chunkAndBlockSet.setInteger("z position", chunk.chunkZPos);
			chunkAndBlockSet.setTag("block set", getNBTList(spreadChunkBlockMap.get(chunk)));
			list.appendTag(chunkAndBlockSet);
		}
		return list;
	}
	
	private void loadFromNBTList(NBTTagList list, EnumConstructionFlowerLevel level) {
		Map<ChunkCoordIntPair, IBlockSet> map;
		if ( level == EnumConstructionFlowerLevel.SPREAD) {
			map = this.spreadChunkBlockMap;
		} else {
			map = this.spawnChunkBlockMap;
		}
		map.clear();
		for ( int i = 0; i < list.tagCount(); ++i) {
			NBTTagCompound chunkNBT = list.getCompoundTagAt(i);
			ChunkCoordIntPair chunk = new ChunkCoordIntPair(chunkNBT.getInteger("x position"), chunkNBT.getInteger("z position"));
			IBlockSet blockSet = createBlockSet();
			NBTTagList blockList = chunkNBT.getTagList("block set", 10);
			for ( int j = 0; j < blockList.tagCount(); ++j) {
				NBTTagCompound blockNBT = blockList.getCompoundTagAt(j);
				blockSet.add(new BlockMember(blockNBT.getInteger("x"), blockNBT.getInteger("z")));
			}
			map.put(chunk, blockSet);
		}
	}
	
	private NBTTagList getNBTList(IBlockSet blockSet) {
		NBTTagList list = new NBTTagList();
		for ( BlockMember block : blockSet ) {
			NBTTagCompound blockTag = new NBTTagCompound();
			blockTag.setInteger("x", block.x());
			blockTag.setInteger("z", block.z());
			list.appendTag(blockTag);
		}
		return list;
	}
	
	public void writeToNBT(NBTTagCompound tagCompound) {
		tagCompound.setTag("spread map", getNBTTagList(EnumConstructionFlowerLevel.SPREAD));
		tagCompound.setTag("spawn map", getNBTTagList(EnumConstructionFlowerLevel.SPAWN));
	}
	
	public void readFromNBT(NBTTagCompound tagCompound) {
		loadFromNBTList(tagCompound.getTagList("spread map", 10), EnumConstructionFlowerLevel.SPREAD);
		loadFromNBTList(tagCompound.getTagList("spawn map", 10), EnumConstructionFlowerLevel.SPAWN);
	}
	
}
