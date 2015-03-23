package com.abocalypse.constructionflower.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.abocalypse.constructionflower.client.gui.GuiSelectPlan;

import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class AvailablePlanSpecsMessage implements IMessage {
	
	private List<String> planSpecFiles;
	
	public AvailablePlanSpecsMessage () {}
	
	public AvailablePlanSpecsMessage(List<String> availablePlans) {
		this.planSpecFiles = availablePlans;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void fromBytes(ByteBuf buf) {
		JSONObject payload;
		try {
			payload = (JSONObject)(new JSONParser().parse(ByteBufUtils.readUTF8String(buf)));
		} catch (ParseException e) {
			throw new RuntimeException("Could not parse LoadPlanMessage");
		}

		this.planSpecFiles = new ArrayList<String>((JSONArray)(payload.get("plan spec files")));
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void toBytes(ByteBuf buf) {
		
		JSONObject payload = new JSONObject();
		payload.put("plan spec files", this.planSpecFiles);
		ByteBufUtils.writeUTF8String(buf, payload.toJSONString());
		
	}
	
	public static class Handler implements IMessageHandler<AvailablePlanSpecsMessage, IMessage> {

		@Override
		public IMessage onMessage(AvailablePlanSpecsMessage message, MessageContext ctx) {
				Minecraft mc = Minecraft.getMinecraft();
				World world = mc.thePlayer.worldObj;
				mc.displayGuiScreen(new GuiSelectPlan(null, world, message.planSpecFiles));
				return null;
		}
		
	}

}
