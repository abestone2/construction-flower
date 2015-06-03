package com.abocalypse.constructionflower.event;

import com.abocalypse.constructionflower.client.gui.GuiCreateConstructionFlowerWorld;

import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class ClientEventListener {
	
	@SubscribeEvent
	public void worldCreationGuiOpenEvent(GuiOpenEvent event) {
		
		if ( event.gui instanceof GuiCreateWorld && !(event.gui instanceof GuiCreateConstructionFlowerWorld) ) {
			GuiScreen screen = ReflectionHelper.getPrivateValue(GuiCreateWorld.class, (GuiCreateWorld)event.gui, "field_146332_f");
			event.gui = new GuiCreateConstructionFlowerWorld(screen);
		}
		
	}
	
}
