package com.abocalypse.constructionflower.plan;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import net.minecraft.world.ChunkCoordIntPair;

public abstract class PlanPartSpec {
	
	public static enum Orientation {
		TOPNORTH, TOPEAST, TOPSOUTH, TOPWEST
	}
	
	private int deltaXAnchor;
	private int deltaZAnchor;
	private Orientation orientation;
	private int xShift;
	private int zShift;
	
	public PlanPartSpec(Map<String, Object>properties) {
		
		this.deltaXAnchor = new Integer(((Long)(properties.getOrDefault("x anchor", 0L))).intValue());
		this.deltaZAnchor = new Integer(((Long)(properties.getOrDefault("z anchor", 0L))).intValue());
		this.orientation = Orientation.valueOf((String)(properties.getOrDefault("orientation", "TOPNORTH")));

	}
	
	private void anchorAt(int x, int z) {
		
		switch (this.orientation) {
		
		case TOPNORTH : 
			xShift = x - deltaXAnchor;
			zShift = z - deltaZAnchor;
			break;
		case TOPEAST : 
			xShift = x + deltaZAnchor;
			zShift = z - deltaXAnchor;
			break;
		case TOPSOUTH :
			xShift = z + deltaZAnchor;
			zShift = x + deltaXAnchor + z;
			break;
		case TOPWEST :
			xShift = x - deltaZAnchor;
			zShift = z + deltaXAnchor;
			break;
		}
		
	}

	public int x(int deltaX, int deltaZ) {
		int ret = 0;
		switch (orientation) {
		
		case TOPNORTH :
			ret = deltaX + xShift;
			break;
		case TOPEAST :
			ret = -deltaZ + xShift;
			break;
		case TOPSOUTH :
			ret = -deltaX + xShift;
			break;
		case TOPWEST :
			ret = deltaZ + xShift;
			break;
			
		}
		return ret;
	}

	public int z(int deltaX, int deltaZ) {
		int ret = 0;
		switch (orientation) {
		
		case TOPNORTH :
			ret = deltaZ + zShift;
			break;
		case TOPEAST :
			ret = deltaX  + zShift;
			break;
		case TOPSOUTH :
			ret = -deltaZ + zShift;
			break;
		case TOPWEST :
			ret = -deltaX + zShift;
			break;
			
		}
		return ret;
	}

	public int deltaX(int x, int z) {
		int ret = 0;
		switch (orientation) {
		
		case TOPNORTH :
			ret = x - xShift;
			break;
		case TOPEAST :
			ret = z  - zShift;
			break;
		case TOPSOUTH :
			ret = -x + xShift;
			break;
		case TOPWEST :
			ret = -z + zShift;
			break;
			
		}
		return ret;
	}

	public int deltaZ(int x, int z) {
		int ret = 0;
		switch (orientation) {
		
		case TOPNORTH :
			ret = z - zShift;
			break;
		case TOPEAST :
			ret = -x + xShift;
			break;
		case TOPSOUTH :
			ret = -z + zShift;
			break;
		case TOPWEST :
			ret = x - xShift;
			break;
			
		}
		return ret;
	}


	public static class AnchoredBlock {
		public final int x;
		public final int z;
		public final int deltaX;
		public final int deltaZ;
		
		public AnchoredBlock(int x, int z, int deltaX, int deltaZ) {
			this.x = x;
			this.z = z;
			this.deltaX = deltaX;
			this.deltaZ = deltaZ;
		}
	}

	public class AnchoredChunk implements Iterable<AnchoredBlock> {

		public final ChunkCoordIntPair coord;
		private final int xMin;
		private final int zMin;
		
		private final int deltaXMin;
		private final int deltaZMin;
		public AnchoredChunk(ChunkCoordIntPair chunk) {
			
			coord = chunk;
			
			xMin = chunk.chunkXPos << 4;
			zMin = chunk.chunkZPos << 4;
			
			switch(orientation) {
			
			case TOPNORTH :
				deltaXMin = xMin - xShift;
				deltaZMin = zMin - zShift;
				break;
			case TOPEAST :
				deltaXMin = zMin - zShift;
				deltaZMin = -xMin + xShift;
				break;
			case TOPSOUTH :
				deltaXMin = -xMin + xShift;
				deltaZMin = -zMin + zShift;
				break;
			case TOPWEST :
				deltaXMin = -zMin + zShift;
				deltaZMin = xMin - xShift;
				break;
			default : 
				throw new IllegalArgumentException("Unknown orientation.");
			}
		}
		
		public Iterator<AnchoredBlock> iterator() {

			// based on https://stackoverflow.com/questions/371026/shortest-way-to-get-an-iterator-over-a-range-of-integers-in-java
			return new Iterator<AnchoredBlock>() {

				private int nextX = xMin;
				private int nextZ = zMin;
				private final int xMax = xMin + 16;
				private final int zMax = zMin + 16;
				private int nextDeltaX = deltaXMin;
				private int nextDeltaZ = deltaZMin;

				public boolean hasNext() {
					return nextZ < zMax - 1 || nextX < xMax - 1;
				}

				public AnchoredBlock next() {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					nextX++;
					if (nextX == xMax) {
						nextX = xMin;
						nextZ++;
						switch(orientation) {
						case TOPNORTH:
							nextDeltaX = deltaXMin;
							nextDeltaZ++;
							break;
						case TOPEAST :
							nextDeltaX++;
							nextDeltaZ = deltaZMin;
							break;
						case TOPSOUTH :
							nextDeltaX--;
							nextDeltaZ = deltaZMin;
							break;
						case TOPWEST :
							nextDeltaX = deltaXMin;
							nextDeltaZ--;
						} 
					} else {
						switch(orientation) {
						case TOPNORTH :
							nextDeltaX++;
							break;
						case TOPEAST :
							nextDeltaZ++;
							break;
						case TOPSOUTH :
							nextDeltaX--;
							break;
						case TOPWEST :
							nextDeltaZ--;
							break;
						}
					}
					return new AnchoredBlock(nextX, nextZ, nextDeltaX, nextDeltaZ);
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		
	}
	
	public class AnchoredChunks implements Iterable<AnchoredChunk> {
		
		private final int chunkXMin;
		private final int chunkXMax;
		private final int chunkZMin;
		private final int chunkZMax;
		
		public AnchoredChunks(int deltaXMin, int deltaXMax, int deltaZMin, int deltaZMax) {
			
			int xMin;
			int xMax;
			int zMin;
			int zMax;
			
			switch (orientation) {
			
			case TOPNORTH :
				xMin = x(deltaXMin, deltaZMin);
				xMax = x(deltaXMax, deltaZMin);
				zMin = z(deltaZMin, deltaZMin);
				zMax = z(deltaZMax, deltaZMax);
				break;
			case TOPEAST :
				xMin = x(deltaXMin, -deltaZMax);
				xMax = x(deltaXMax, -deltaZMin);
				zMin = z(deltaXMin, -deltaZMax);
				zMax = z(deltaXMax, -deltaZMin);
				break;
			case TOPSOUTH :
				xMin = x(-deltaXMax, -deltaZMax);
				xMax = x(-deltaXMin, -deltaZMin);
				zMin = z(-deltaXMax, -deltaZMax);
				zMax = z(-deltaXMin, -deltaZMax);
				break;
			case TOPWEST :
				xMin = x(-deltaXMax, deltaZMin);
				xMax = x(-deltaXMin, deltaZMax);
				zMin = z(-deltaXMax, deltaZMin);
				zMax = z(-deltaXMin, deltaZMax);
				break;
			default:
				throw new IllegalArgumentException("Unknown orientation.");
			}
			
			chunkXMin = xMin >> 4;
			chunkXMax = xMax >> 4;
			chunkZMin = zMin >> 4;
			chunkZMax = zMax >> 4;
		}
		
		public Iterator<AnchoredChunk> iterator() {
			return new Iterator<AnchoredChunk>() {
				private int nextChunkX = chunkXMin;
				private int nextChunkZ = chunkZMin;
				
				public boolean hasNext() {
					return nextChunkZ < chunkZMax - 1 || nextChunkX < chunkXMax - 1;
				}
				
				public AnchoredChunk next() {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					nextChunkX++;
					if ( nextChunkX == chunkXMax ) {
						nextChunkX = chunkXMin;
						nextChunkZ++;
					}
					return new AnchoredChunk(new ChunkCoordIntPair(nextChunkX, nextChunkZ));
				}
			};
		}
	}

	
	public void load(int worldX, int worldZ, ChunkPlanBlocks chunkPlanBlocks) {
		anchorAt(worldX, worldZ);
		try {
			doLoad(worldX, worldZ, chunkPlanBlocks);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	abstract protected void doLoad(int worldX, int worldZ, ChunkPlanBlocks chunkPlanBlocks) throws IOException;

}
