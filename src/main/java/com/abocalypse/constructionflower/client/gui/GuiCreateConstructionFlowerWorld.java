package com.abocalypse.constructionflower.client.gui;

import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;

@SideOnly(Side.CLIENT)
public class GuiCreateConstructionFlowerWorld extends GuiCreateWorld {
	
	private GuiScreen parentScreen;
	private boolean haveShownLoadPlan;
	private GuiButton createWorldButton;
	
	public GuiCreateConstructionFlowerWorld(GuiScreen screen) {
		super(screen);
		this.parentScreen = screen;
		this.haveShownLoadPlan = false;
	}
	
	@Override
	public void initGui() {

		super.initGui();

	}

	@Override
	public void actionPerformed(GuiButton button) {
		if ( button.id == 0 && !haveShownLoadPlan ) {
			createWorldButton = button; // keep track of this
			mc.displayGuiScreen(new GuiLoadPlan(this, true, null, null));
		} else {
			super.actionPerformed(button);
		}
	}
		
	public void continueCreatingWorld() {
		haveShownLoadPlan = true;
		actionPerformed(createWorldButton);
	}
	
	public void cancelCreatingWorld() {
		this.mc.displayGuiScreen(this.parentScreen);
	}
	
}
