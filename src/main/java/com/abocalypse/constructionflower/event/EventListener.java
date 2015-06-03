package com.abocalypse.constructionflower.event;

import com.abocalypse.constructionflower.world.SpawnHandler;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class EventListener {
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent.Load event) {
		Chunk chunk = event.getChunk();
		World world = chunk.worldObj;
		ChunkCoordIntPair chunkCoords = new ChunkCoordIntPair(chunk.xPosition, chunk.zPosition);
		SpawnHandler.spawnOntoIfStale(world, chunkCoords, world.rand);
	}

}
