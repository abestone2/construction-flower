package com.abocalypse.constructionflower.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import com.abocalypse.constructionflower.client.gui.GuiLoadPlan;
import com.abocalypse.constructionflower.plan.BlockXZCoords;

import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class OpenGuiLoadPlanMessage implements IMessage {
	
	private List<String> planSpecFiles;
	private Map<String, BlockXZCoords> existingPlans;
	
	public OpenGuiLoadPlanMessage () {}
	
	public OpenGuiLoadPlanMessage(List<String> availablePlans, Map<String, BlockXZCoords> existingPlans) {
		this.planSpecFiles = availablePlans;
		this.existingPlans = existingPlans;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		NBTTagCompound payload = ByteBufUtils.readTag(buf);
		NBTTagList planSpecList = payload.getTagList("plan specs", 8); // String
		this.planSpecFiles = new ArrayList<String>();
		for ( int i = 0; i < planSpecList.tagCount(); ++i ) {
			this.planSpecFiles.add(planSpecList.getStringTagAt(i));
		}
		this.existingPlans = new HashMap<String, BlockXZCoords>();
		if ( payload.getBoolean("plans exist") ) {
			NBTTagList existingPlansList = payload.getTagList("existing plans",
					10); // compound Tag
			for (int i = 0; i < existingPlansList.tagCount(); ++i) {
				NBTTagCompound planTag = existingPlansList.getCompoundTagAt(i);
				this.existingPlans.put(planTag.getString("name"),
						new BlockXZCoords(planTag.getInteger("x anchor"),
								planTag.getInteger("z anchor")));
			}
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		
		NBTTagCompound payload = new NBTTagCompound();
		NBTTagList planSpecList = new NBTTagList();
		for ( String fileName : this.planSpecFiles ) {
			planSpecList.appendTag(new NBTTagString(fileName));
		}
		NBTTagList existingPlansList;
		if ( this.existingPlans.size() > 0 ) {
			payload.setBoolean("plans exist", true);
			existingPlansList = new NBTTagList();
			for (Map.Entry<String, BlockXZCoords> entry : this.existingPlans
					.entrySet()) {
				NBTTagCompound planTag = new NBTTagCompound();
				planTag.setString("name", entry.getKey());
				planTag.setInteger("x anchor", entry.getValue().x);
				planTag.setInteger("z anchor", entry.getValue().z);
				existingPlansList.appendTag(planTag);
			}
			payload.setTag("existing plans", existingPlansList);
		} else {
			payload.setBoolean("plans exist", false);
		}
		payload.setTag("plan specs", planSpecList);
		ByteBufUtils.writeTag(buf, payload);
		
	}
	
	public static class Handler implements IMessageHandler<OpenGuiLoadPlanMessage, IMessage> {

		@Override
		public IMessage onMessage(OpenGuiLoadPlanMessage message, MessageContext ctx) {
				Minecraft mc = Minecraft.getMinecraft();
				mc.displayGuiScreen(new GuiLoadPlan(null, false, message.planSpecFiles, message.existingPlans));
				return null;
		}
		
	}

}
