package com.abocalypse.constructionflower.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
		ChunkCoordIntPair chunk = new ChunkCoordIntPair(x >> 4, z >> 4);
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
