package com.abocalypse.constructionflower.command;

import java.util.ArrayList;
import java.util.List;

import com.abocalypse.constructionflower.ConstructionFlower;
import com.abocalypse.constructionflower.network.AvailablePlanSpecsMessage;
import com.abocalypse.constructionflower.plan.WorldPlanRegistry;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

public class LoadPlanCommand implements ICommand {

	private static ArrayList<String> aliases = new ArrayList<String>();
	static
	{
		aliases.add("cfloadplan");
		aliases.add("cfload");
	}
	
	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCommandName() {
		return aliases.get(0);
	}

	@Override
	public String getCommandUsage(ICommandSender p_71518_1_) {
		return getCommandName();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List getCommandAliases() {
		return aliases;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if ( sender instanceof EntityPlayerMP ) {
			List<String> planSpecFiles = WorldPlanRegistry.getAvailablePlans();
			ConstructionFlower.instance.network.sendTo(new AvailablePlanSpecsMessage(planSpecFiles), (EntityPlayerMP)sender);
		}
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender p_71519_1_) {
		return true;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List addTabCompletionOptions(ICommandSender p_71516_1_,
			String[] p_71516_2_) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isUsernameIndex(String[] p_82358_1_, int p_82358_2_) {
		// TODO Auto-generated method stub
		return false;
	}

}
