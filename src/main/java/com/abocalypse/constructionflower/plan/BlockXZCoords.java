package com.abocalypse.constructionflower.plan;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;

public class BlockXZCoords {

	public int x;
	public int z;
		
	public BlockXZCoords(int x, int z) {
		this.x = x;
		this.z = z;
	}
	
	public BlockXZCoords(BlockXZCoords c) {
		this.x = c.x;
		this.z = c.z;
	}
		
	public BlockXZCoords(ChunkCoordinates chunkCoords) {
		this.x = chunkCoords.posX;
		this.z = chunkCoords.posZ;
	}
		
	public BlockXZCoords(Entity e) {
		this.x = MathHelper.floor_double(e.posX);
		this.z = MathHelper.floor_double(e.posZ);
	}
		
	public BlockXZCoords(NBTTagCompound tagCompound) {
		this.x = tagCompound.getInteger("x");
		this.z = tagCompound.getInteger("z");
	}

	public NBTTagCompound tagCompound() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("x", this.x);
		tag.setInteger("z", this.z);
		return tag;
	}
		
	public static BlockXZCoords origin() {
		return new BlockXZCoords(0, 0);
	}
		
	public void add(BlockXZCoords other) {
		this.x += other.x;
		this.z += other.z;
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
		BlockXZCoords other = (BlockXZCoords) obj;
		if (x != other.x)
			return false;
		if (z != other.z)
			return false;
		return true;
	}

}
