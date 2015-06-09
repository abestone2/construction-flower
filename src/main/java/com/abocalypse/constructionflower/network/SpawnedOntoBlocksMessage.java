package com.abocalypse.constructionflower.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.abocalypse.constructionflower.ConstructionFlower;
import com.abocalypse.constructionflower.lib.ConfigHandler;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChunkCoordinates;
import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SpawnedOntoBlocksMessage implements IMessage {
	
	private List<ChunkCoordinates> spawnedOnto;
	
	public SpawnedOntoBlocksMessage() {}
	
	public SpawnedOntoBlocksMessage(List<ChunkCoordinates> spawnedOnto) {
		int n = spawnedOnto.size();
		if ( n > ConfigHandler.maxSpawnsSentToClient ) {
			n = ConfigHandler.maxSpawnsSentToClient;
			Collections.shuffle(spawnedOnto);
		}
		this.spawnedOnto = spawnedOnto.subList(0, n);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.spawnedOnto = new ArrayList<ChunkCoordinates>();
		NBTTagCompound payload = ByteBufUtils.readTag(buf);
		NBTTagList spawnedOntoList = payload.getTagList("blocks", 10);
		for ( int i = 0; i < spawnedOntoList.tagCount(); ++i ) {
			NBTTagCompound blockTag = spawnedOntoList.getCompoundTagAt(i);
			this.spawnedOnto.add(new ChunkCoordinates(blockTag.getInteger("x"), blockTag.getInteger("y"), blockTag.getInteger("z")));
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		NBTTagCompound payload = new NBTTagCompound();
		NBTTagList spawnedOntoList = new NBTTagList();
		for ( ChunkCoordinates block : this.spawnedOnto ) {
			NBTTagCompound blockTag = new NBTTagCompound();
			blockTag.setInteger("x", block.posX);
			blockTag.setInteger("y", block.posY);
			blockTag.setInteger("z", block.posZ);
			spawnedOntoList.appendTag(blockTag);
		}
		payload.setTag("blocks", spawnedOntoList);
		ByteBufUtils.writeTag(buf, payload);
	}
	
	public static class Handler implements IMessageHandler<SpawnedOntoBlocksMessage, IMessage> {

		@Override
		public IMessage onMessage(SpawnedOntoBlocksMessage message, MessageContext ctx) {
			ConstructionFlower.proxy.onMessageForSpawnedOntoBlocksMessage(message.spawnedOnto);
			return null;
		}
		
	}

}
