package com.abocalypse.constructionflower.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.abocalypse.constructionflower.plan.WorldPlanRegistry;

import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class LoadPlanMessage implements IMessage {
	
	private String planName;
	private WorldPlanRegistry.PlanPosition position;
	private String planSpecFileName;
	
	public LoadPlanMessage() {}
		
	public LoadPlanMessage(WorldPlanRegistry.PlanPosition position, String planName, String planSpecFileName) {
		this.position = new WorldPlanRegistry.PlanPosition(position);
		this.planName = planName;
		this.planSpecFileName = planSpecFileName;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		NBTTagCompound payload = ByteBufUtils.readTag(buf);
		this.planName = payload.getString("plan name");
		this.planSpecFileName = payload.getString("plan spec file name");
		NBTTagCompound positionTag = payload.getCompoundTag("position");
		this.position = new WorldPlanRegistry.PlanPosition();
		position.readFromNBT(positionTag);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		NBTTagCompound payload = new NBTTagCompound();
		payload.setString("plan name", this.planName);
		payload.setString("plan spec file name", this.planSpecFileName);
		NBTTagCompound positionTag = new NBTTagCompound();
		this.position.writeToNBT(positionTag);
		payload.setTag("position", positionTag);
		ByteBufUtils.writeTag(buf, payload);
	}
	
	public static class Handler implements IMessageHandler<LoadPlanMessage, SpawnedOntoBlocksMessage> {

		@Override
		public SpawnedOntoBlocksMessage onMessage(LoadPlanMessage message, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			WorldPlanRegistry registry = WorldPlanRegistry.get(world);
			registry.loadPlanSpec(message.planSpecFileName, message.position, message.planName);
			List<ChunkCoordinates> spawnedOnto = new ArrayList<ChunkCoordinates>();
			registry.respawnOntoPlan(message.planName, spawnedOnto);
			return new SpawnedOntoBlocksMessage(spawnedOnto);
		}
		
	}

}
