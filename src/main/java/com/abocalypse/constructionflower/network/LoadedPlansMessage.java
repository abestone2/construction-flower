package com.abocalypse.constructionflower.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import com.abocalypse.constructionflower.client.gui.GuiManagePlans;
import com.abocalypse.constructionflower.plan.WorldPlanRegistry;

import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class LoadedPlansMessage implements IMessage {
	
	private Map<String, WorldPlanRegistry.PlanInfo> loadedPlans;
	
	public LoadedPlansMessage () {}
	
	public LoadedPlansMessage(Map<String, WorldPlanRegistry.PlanInfo> loadedPlans) {
		Iterator<Entry<String, WorldPlanRegistry.PlanInfo>> it = loadedPlans.entrySet().iterator();
		while ( it.hasNext() ) {
			if ( it.next().getValue().status == WorldPlanRegistry.PlanStatus.REMOVE ) {
				it.remove();
			}
		}
		this.loadedPlans = loadedPlans;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.loadedPlans = new HashMap<String, WorldPlanRegistry.PlanInfo>();
		NBTTagCompound payload = ByteBufUtils.readTag(buf);
		NBTTagList planList = payload.getTagList("plan list", 10); // NBTBase type 10 = compound
		for (  int i = 0; i < planList.tagCount(); ++ i ) {
			NBTTagCompound planTag = planList.getCompoundTagAt(i);
			String planName = planTag.getString("name");
			WorldPlanRegistry.PlanInfo planInfo = new WorldPlanRegistry.PlanInfo();
			planInfo.readFromNBT(planTag.getCompoundTag("info"));
			this.loadedPlans.put(planName, planInfo);
		}
		
	}

	@Override
	public void toBytes(ByteBuf buf) {
		NBTTagCompound payload = new NBTTagCompound();
		NBTTagList planList = new NBTTagList();
		for ( Map.Entry<String, WorldPlanRegistry.PlanInfo> entry : this.loadedPlans.entrySet() ) {
			WorldPlanRegistry.PlanInfo planInfo = entry.getValue();
			NBTTagCompound planInfoTag = new NBTTagCompound();
			planInfo.writeToNBT(planInfoTag);
			NBTTagCompound planTag = new NBTTagCompound();
			planTag.setString("name", entry.getKey());
			planTag.setTag("info", planInfoTag);
			planList.appendTag(planTag);
		}
		payload.setTag("plan list", planList);
		ByteBufUtils.writeTag(buf, payload);
	}
	
	@SideOnly(Side.CLIENT)
	public static class ClientHandler implements IMessageHandler<LoadedPlansMessage, IMessage> {

		@Override
		public IMessage onMessage(LoadedPlansMessage message, MessageContext ctx) {
			Minecraft mc = Minecraft.getMinecraft();
			mc.displayGuiScreen(new GuiManagePlans(null, message.loadedPlans));
			return null;
		}
		
	}
	
	public static class ServerHandler implements IMessageHandler<LoadedPlansMessage, SpawnedOntoBlocksMessage> {

		@Override
		public SpawnedOntoBlocksMessage onMessage(LoadedPlansMessage message, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			WorldPlanRegistry registry = WorldPlanRegistry.get(world);
			List<ChunkCoordinates> spawnedOnto = new ArrayList<ChunkCoordinates>();
			registry.updatePlans(message.loadedPlans, spawnedOnto);
			return new SpawnedOntoBlocksMessage(spawnedOnto);
		}
		
	}

}

