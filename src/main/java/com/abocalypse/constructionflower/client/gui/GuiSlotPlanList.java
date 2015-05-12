package com.abocalypse.constructionflower.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.abocalypse.constructionflower.plan.BlockXZCoords;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;

@SideOnly(Side.CLIENT)
public class GuiSlotPlanList extends GuiSlot {
	
	private final List<String> planList;
	private final GuiScreen screen;
	private final FontRenderer fontRendererObj;
	private int planSelected;
	private boolean active;
	
	public GuiSlotPlanList(Minecraft mc, GuiScreen screen, FontRenderer fontRendererObj, int left, int top, Map<String, BlockXZCoords> plans) {
		super(mc, GuiConstants.BUTTON_WIDTH, 3*GuiConstants.SLOT_HEIGHT, top, top + 3*GuiConstants.SLOT_HEIGHT, GuiConstants.SLOT_HEIGHT);
		// super automatically sets left to 0 and right to width; fix
		this.left = left - GuiConstants.HORIZONTAL_GUTTER;
		this.right = left + this.width + GuiConstants.HORIZONTAL_GUTTER;

		this.screen = screen;
		this.fontRendererObj = fontRendererObj;
		
		this.active = false;
		
		this.planList = new ArrayList<String>(plans.keySet());
		this.planSelected = 0;
	}

	public void activate() {
		this.active = true;
		this.setShowSelectionBox(true);
	}
	public void deactivate() {
		this.setShowSelectionBox(false);
		this.active = false;
	}
	
	
	@Override
	protected int getSize() {
		return planList.size();
	}

	@Override
	protected void elementClicked(int slotClicked, boolean doubleClicked,
			int p_148144_3_, int p_148144_4_) {
		if ( this.active ) {
			this.planSelected = slotClicked;
		}
	}

	@Override
	protected boolean isSelected(int slot) {
		return slot == this.planSelected;
	}
	
	public String planSelected() {
		return this.planList.get(this.planSelected);
	}

	@Override
	protected void drawBackground() {
//		if ( this.active ) {
//			this.screen.drawDefaultBackground();
//		}
	}

	@Override
	protected void drawSlot(int slot, int x, int y, int p_148126_4_, Tessellator p_148126_5_, int p_148126_6_, int p_148126_7_) {
		if ( this.active ) {
			screen.drawString(this.fontRendererObj, this.planList.get(slot), x + 2, y + 1, 0xFFFFFF);
		}
	}
	
	@Override
	public int getListWidth()
    {
        return this.width;
    }

	@Override
	protected int getScrollBarX()
    {
        return this.left - GuiConstants.SCROLL_BAR_WIDTH;
    }


}
