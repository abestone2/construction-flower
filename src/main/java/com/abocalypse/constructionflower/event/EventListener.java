package com.abocalypse.constructionflower.event;

import com.abocalypse.constructionflower.client.gui.GuiCreateConstructionFlowerWorld;
import com.abocalypse.constructionflower.world.SpawnHandler;

import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.world.ChunkEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class EventListener {
	
	@SubscribeEvent
	public void worldCreationGuiOpenEvent(GuiOpenEvent event) {
		
		if ( event.gui instanceof GuiCreateWorld && !(event.gui instanceof GuiCreateConstructionFlowerWorld) ) {
			GuiScreen screen = ReflectionHelper.getPrivateValue(GuiCreateWorld.class, (GuiCreateWorld)event.gui, "field_146332_f");
			event.gui = new GuiCreateConstructionFlowerWorld(screen);
		}
		
	}
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent.Load event) {
		Chunk chunk = event.getChunk();
		World world = chunk.worldObj;
		ChunkCoordIntPair chunkCoords = new ChunkCoordIntPair(chunk.xPosition, chunk.zPosition);
		SpawnHandler.spawnOntoIfStale(world, chunkCoords, world.rand);
	}

}
