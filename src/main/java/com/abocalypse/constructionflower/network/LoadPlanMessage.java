package com.abocalypse.constructionflower.network;

import net.minecraft.world.World;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.abocalypse.constructionflower.plan.PlanSpec;
import com.abocalypse.constructionflower.plan.WorldPlanRegistry;

import io.netty.buffer.ByteBuf;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class LoadPlanMessage implements IMessage {
	
	private int xAnchor;
	private int zAnchor;
	private WorldPlanRegistry.AnchorMode anchorMode;
	private PlanSpec.Orientation orientation;
	private String planName;
	private String planSpecFileName;
	
	public LoadPlanMessage() {}
		
	public LoadPlanMessage(int xAnchor, int zAnchor, WorldPlanRegistry.AnchorMode anchorMode, PlanSpec.Orientation orientation, String planName, String planSpecFileName) {
		this.xAnchor = xAnchor;
		this.zAnchor = zAnchor;
		this.anchorMode = anchorMode;
		this.orientation = orientation;
		this.planName = planName;
		this.planSpecFileName = planSpecFileName;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		JSONObject payload;
		try {
			payload = (JSONObject)(new JSONParser().parse(ByteBufUtils.readUTF8String(buf)));
		} catch (ParseException e) {
			throw new RuntimeException("Could not parse LoadPlanMessage");
		}
		this.xAnchor = new Integer(((Long)(payload.get("x anchor"))).intValue());
		this.zAnchor = new Integer(((Long)(payload.get("z anchor"))).intValue());
		this.anchorMode = WorldPlanRegistry.AnchorMode.valueOf((String)(payload.get("anchor mode")));
		this.orientation = PlanSpec.Orientation.valueOf((String)(payload.get("orientation")));
		this.planName = (String)(payload.get("plan name"));
		this.planSpecFileName = (String)(payload.get("plan spec file"));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void toBytes(ByteBuf buf) {
		JSONObject payload = new JSONObject();
		payload.put("x anchor", xAnchor);
		payload.put("z anchor", zAnchor);
		payload.put("anchor mode", anchorMode.name());
		payload.put("orientation", orientation.name());
		payload.put("plan name", planName);
		payload.put("plan spec file", planSpecFileName);
		ByteBufUtils.writeUTF8String(buf, payload.toJSONString());
	}
	
	public static class Handler implements IMessageHandler<LoadPlanMessage, IMessage> {

		@Override
		public IMessage onMessage(LoadPlanMessage message, MessageContext ctx) {
			World world = ctx.getServerHandler().playerEntity.worldObj;
			WorldPlanRegistry registry = WorldPlanRegistry.get(world);
			registry.loadPlanSpec(message.planSpecFileName, message.xAnchor, message.zAnchor, message.anchorMode, message.orientation, message.planName);
			registry.respawnOntoPlan(message.planName);
			return null;
		}
		
	}

}
