package com.abocalypse.constructionflower.client.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.lwjgl.input.Keyboard;
// import org.lwjgl.opengl.GL11;





import com.abocalypse.constructionflower.ConstructionFlower;
import com.abocalypse.constructionflower.lib.EnumAnchorMode;
import com.abocalypse.constructionflower.lib.EnumOrientation;
import com.abocalypse.constructionflower.network.LoadedPlansMessage;
import com.abocalypse.constructionflower.plan.BlockXZCoords;
import com.abocalypse.constructionflower.plan.WorldPlanRegistry;
import com.abocalypse.constructionflower.truetyper.FontHelper;
import com.abocalypse.constructionflower.util.ArrayCycler;
import com.abocalypse.constructionflower.util.EnumCycler;

import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChunkCoordinates;

@SideOnly(Side.CLIENT)
public class GuiManagePlans extends GuiScreen {

	private SortedMap<String, WorldPlanRegistry.PlanInfo> plans;
	private Map<String, WorldPlanRegistry.PlanInfo> savedInfo;
	private boolean planIsSelected;
	private boolean planSelectedIsToBeRemoved;
	private int planSelected;
	private String nameSelected;

	private static final String REMOVE_PLAN_TEXT = "Remove";
	private static final String DONT_REMOVE_PLAN_TEXT = "Don't remove";
	private static final String REFRESH_PLAN_TEXT = "Reload from Disk";
	private static final String DEREFRESH_TEXT = "Do not reload";
	private static final String DONE_TEXT = "Done";
	private static final String CANCEL_CHANGES_TEXT  = "Cancel changes";

	private PlanList selectPlan;

	private GuiTextField planNameField;
	private GuiTextField xAnchorField;
	private GuiTextField zAnchorField;
	private GuiTextField xAnchorRelativeToField;
	private GuiTextField zAnchorRelativeToField;

	private EnumCycler<EnumOrientation> orientation;
	private EnumCycler<EnumAnchorMode> anchorMode;
	private ArrayCycler<String> planNameCycler;

    private static enum ButtonID {
		REMOVE, REFRESH, ORIENTATION, ANCHOR_MODE, RELATIVE_TO_PLAN, SCROLL_UP, SCROLL_DOWN, DONE, CANCEL_CHANGES
	}
    
	private EnumMap<ButtonID, GuiButton> buttons;
	
	private static enum HeaderID {
		AT_X, AT_Z, PLAN_LIST_HEADER, RELATIVE_TO_X, RELATIVE_TO_Z, PLAN_NAME;
	}
	private static final EnumMap<HeaderID, String> headerText = new EnumMap<HeaderID, String>(HeaderID.class);
	static {
		headerText.put(HeaderID.AT_X, "Anchor at X:");
		headerText.put(HeaderID.AT_Z, "Z:");
		headerText.put(HeaderID.PLAN_LIST_HEADER, "Plan:");
		headerText.put(HeaderID.RELATIVE_TO_X, "X");
		headerText.put(HeaderID.RELATIVE_TO_Z, "Z");
		headerText.put(HeaderID.PLAN_NAME, "Plan name:");
	}
	private EnumMap<HeaderID, Integer> headerXPos;
	private EnumMap<HeaderID, Integer> headerYPos;
	private EnumSet<HeaderID> activeHeaders;

	
	public GuiManagePlans(GuiScreen screen, Map<String, WorldPlanRegistry.PlanInfo> loadedPlans) {
		plans = new TreeMap<String, WorldPlanRegistry.PlanInfo>();
		savedInfo = new HashMap<String, WorldPlanRegistry.PlanInfo>();
		for ( Map.Entry<String, WorldPlanRegistry.PlanInfo> entry : loadedPlans.entrySet() ) {
			plans.put(entry.getKey(), entry.getValue());
			savedInfo.put(entry.getKey(), entry.getValue());
		}
		planIsSelected = false;
		planSelected = -1;
		buttons = new EnumMap<ButtonID, GuiButton>(ButtonID.class);
		activeHeaders = EnumSet.noneOf(HeaderID.class);
		headerXPos = new EnumMap<HeaderID, Integer>(HeaderID.class);
		headerYPos = new EnumMap<HeaderID, Integer>(HeaderID.class);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void initGui() {
		
		if ( plans.size() == 0 ) {
			this.mc.displayGuiScreen(new GuiErrorScreen("No plans available", ""));
			return;
		}

		Keyboard.enableRepeatEvents(true);
		buttonList.clear();

		int textFieldWidth = GuiConstants.BUTTON_WIDTH;
		int smallTextFieldWidth = (textFieldWidth - GuiConstants.HORIZONTAL_GUTTER)/2; 
		int bothHeight = Math.max(GuiConstants.TEXT_FIELD_HEIGHT, GuiConstants.BUTTON_HEIGHT);
		
		int x = 0;
		int y = this.height;
		
		x += 2*GuiConstants.HORIZONTAL_GUTTER + textFieldWidth;
		y -=  GuiConstants.VERTICAL_GUTTER + bothHeight;
		planNameCycler = new ArrayCycler<String>(new ArrayList<String>(this.plans.keySet()));
		buttons.put(ButtonID.RELATIVE_TO_PLAN, new GuiButton(ButtonID.RELATIVE_TO_PLAN.ordinal(), x, y, GuiConstants.BUTTON_WIDTH, GuiConstants.BUTTON_HEIGHT, ""));
		buttonList.add(buttons.get(ButtonID.RELATIVE_TO_PLAN));
		buttons.get(ButtonID.RELATIVE_TO_PLAN).enabled = false;
		buttons.get(ButtonID.RELATIVE_TO_PLAN).visible = false;
		xAnchorRelativeToField = new GuiTextField(this.fontRendererObj, x, y, smallTextFieldWidth, GuiConstants.TEXT_FIELD_HEIGHT);
		addHeadersAbove(new HeaderID[]{HeaderID.RELATIVE_TO_X}, x, y);
		x += textFieldWidth - smallTextFieldWidth;
		zAnchorRelativeToField = new GuiTextField(this.fontRendererObj, x, y, smallTextFieldWidth, GuiConstants.TEXT_FIELD_HEIGHT);
		addHeadersAbove(new HeaderID[]{HeaderID.RELATIVE_TO_Z}, x, y);

		x = GuiConstants.HORIZONTAL_GUTTER; 
		y -= GuiConstants.VERTICAL_GUTTER + bothHeight + GuiConstants.SPACE_FOR_HEADER_ROW;
		this.xAnchorField = new GuiTextField(this.fontRendererObj, x, y, smallTextFieldWidth, GuiConstants.TEXT_FIELD_HEIGHT);
		addHeadersAbove(new HeaderID[]{HeaderID.AT_X}, x, y);
		x += textFieldWidth - smallTextFieldWidth;
		this.zAnchorField = new GuiTextField(this.fontRendererObj, x, y, smallTextFieldWidth, GuiConstants.TEXT_FIELD_HEIGHT);
		addHeadersAbove(new HeaderID[]{HeaderID.AT_Z}, x, y);
		
		x += smallTextFieldWidth + GuiConstants.HORIZONTAL_GUTTER;
		this.anchorMode = new EnumCycler<EnumAnchorMode>(EnumAnchorMode.class);
		buttons.put(ButtonID.ANCHOR_MODE, new GuiButton(ButtonID.ANCHOR_MODE.ordinal(), x, y, GuiConstants.BUTTON_WIDTH, GuiConstants.BUTTON_HEIGHT, ""));
		buttonList.add(buttons.get(ButtonID.ANCHOR_MODE));
		buttons.get(ButtonID.ANCHOR_MODE).enabled = false;
		buttons.get(ButtonID.ANCHOR_MODE).visible = false;

		x = GuiConstants.HORIZONTAL_GUTTER;
		y -= GuiConstants.VERTICAL_GUTTER + bothHeight + GuiConstants.SPACE_FOR_HEADER_ROW;
		this.planNameField = new GuiTextField(this.fontRendererObj, x, y, textFieldWidth, GuiConstants.TEXT_FIELD_HEIGHT);
		addHeadersAbove(new HeaderID[]{HeaderID.PLAN_NAME}, x, y);
		
		x += textFieldWidth + GuiConstants.HORIZONTAL_GUTTER;
		this.orientation = new EnumCycler<EnumOrientation>(EnumOrientation.class);
		buttons.put(ButtonID.ORIENTATION, new GuiButton(ButtonID.ORIENTATION.ordinal(), x, y, 2*GuiConstants.BUTTON_WIDTH - textFieldWidth, GuiConstants.BUTTON_HEIGHT, ""));
		buttonList.add(buttons.get(ButtonID.ORIENTATION));
		buttons.get(ButtonID.ORIENTATION).enabled = false;
		buttons.get(ButtonID.ORIENTATION).visible = false;

		int bottom = y - GuiConstants.VERTICAL_GUTTER - GuiConstants.SPACE_FOR_HEADER_ROW;
		
		int yDown = GuiConstants.VERTICAL_GUTTER;
		yDown = addSideButton(ButtonID.REFRESH, REFRESH_PLAN_TEXT, yDown);
		buttons.get(ButtonID.REFRESH).enabled = false;
		yDown = addSideButton(ButtonID.REMOVE, REMOVE_PLAN_TEXT, yDown);
		buttons.get(ButtonID.REMOVE).enabled = false;
		yDown = addSideButton(ButtonID.DONE, DONE_TEXT, yDown);
		yDown = addSideButton(ButtonID.CANCEL_CHANGES, CANCEL_CHANGES_TEXT, yDown);
		
		yDown = GuiConstants.VERTICAL_GUTTER + GuiConstants.SPACE_FOR_HEADER_ROW;
		selectPlan = new PlanList(yDown, bottom);
		selectPlan.registerScrollButtons(ButtonID.SCROLL_UP.ordinal(), ButtonID.SCROLL_DOWN.ordinal());
		addHeadersAbove(new HeaderID[]{HeaderID.PLAN_LIST_HEADER}, GuiConstants.HORIZONTAL_GUTTER, yDown);
		activeHeaders.add(HeaderID.PLAN_LIST_HEADER);
		
		setButtonTexts();
	}
	
	class PlanList extends GuiSlot {

		public PlanList(int top, int bottom) {
			super(GuiManagePlans.this.mc, GuiManagePlans.this.width - 2*GuiConstants.HORIZONTAL_GUTTER - GuiConstants.BUTTON_WIDTH, bottom - 2*GuiConstants.VERTICAL_GUTTER, top, bottom, GuiConstants.SLOT_HEIGHT);
			this.setHasListHeader(false, 0);
		}

		@Override
		protected int getSize() {
			return plans.size();
		}

		@Override
		// Not clear what the last two arguments are supposed to be for
		protected void elementClicked(int slotClicked, boolean doubleClicked,
									  int p_148144_3_, int p_148144_4_) {
			if ( planIsSelected ) {
				setPlanFromFields();
			}
			GuiManagePlans.this.planSelected = slotClicked;
			if ( slotClicked >= 0 && slotClicked < this.getSize() ) {
				planIsSelected = true;
				nameSelected = planNameAtSlot(slotClicked);
				setFieldsFromPlanPosition(GuiManagePlans.this.nameSelected, GuiManagePlans.this.plans.get(GuiManagePlans.this.nameSelected).position);
				buttons.get(ButtonID.REFRESH).enabled = true;
				buttons.get(ButtonID.REMOVE).enabled = true;
				buttons.get(ButtonID.ORIENTATION).visible = true;
				buttons.get(ButtonID.ANCHOR_MODE).visible = true;
				setRemoved(plans.get(GuiManagePlans.this.nameSelected).status == WorldPlanRegistry.PlanStatus.REMOVE);
				activeHeaders.add(HeaderID.AT_X);
				activeHeaders.add(HeaderID.AT_Z);
				activeHeaders.add(HeaderID.PLAN_NAME);
			} else {
				planIsSelected = false;
				nameSelected = null;
				buttons.get(ButtonID.REFRESH).enabled = true;
				buttons.get(ButtonID.REMOVE).enabled = true;
				buttons.get(ButtonID.ORIENTATION).enabled = false;
				buttons.get(ButtonID.ORIENTATION).visible = false;
				buttons.get(ButtonID.ANCHOR_MODE).enabled = false;
				buttons.get(ButtonID.ANCHOR_MODE).visible = false;
				activeHeaders.remove(HeaderID.AT_X);
				activeHeaders.remove(HeaderID.AT_Z);
				activeHeaders.remove(HeaderID.PLAN_NAME);
			}
			setButtonTexts();
		}

		@Override
		protected boolean isSelected(int slot) {
			return slot == GuiManagePlans.this.planSelected;
		}

		@Override
		protected void drawBackground() {
			GuiManagePlans.this.drawDefaultBackground();
		}
		
		// TODO ugh can it really be that this is the only way?
		@SuppressWarnings("unchecked")
		private String planNameAtSlot(int slot) {
			Collection<Entry<String, WorldPlanRegistry.PlanInfo>> entries = GuiManagePlans.this.plans.entrySet();
			Object[] entryArray = entries.toArray(new Object[entries.size()]);
			return ((Entry<String, WorldPlanRegistry.PlanInfo>)entryArray[slot]).getKey();
		}

		@Override
		// Not clear what the last four arguments are supposed to be for.
		protected void drawSlot(int slot, int x, int y, int p_148126_4_, Tessellator p_148126_5_, int p_148126_6_, int p_148126_7_) {
			String planName = planNameAtSlot(slot);
			if ( plans.get(planName).status == WorldPlanRegistry.PlanStatus.REFRESH ) {
				planName += " ↻";
			} else if ( plans.get(planName).status == WorldPlanRegistry.PlanStatus.REMOVE ) {
				planName = "§7§m" + planName;
			}
			GuiManagePlans.this.drawString(GuiManagePlans.this.fontRendererObj, planName, x + 2, y + 1, 0xFFFFFF);
		}

	}
	
	public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

	protected void setButtonTexts() {
		buttons.get(ButtonID.ANCHOR_MODE).displayString = GuiConstants.ANCHOR_MODE_TEXT.get(anchorMode.value());
		if ( this.anchorMode.value() == EnumAnchorMode.RELATIVE_TO_PLAN ) {
			buttons.get(ButtonID.RELATIVE_TO_PLAN).displayString = planNameCycler.value();
		}
		buttons.get(ButtonID.ORIENTATION).displayString = GuiConstants.ORIENTATION_TEXT.get(orientation.value());
	}

	protected void switchButtons(EnumSet<ButtonID> buttons, boolean on) {
		for ( ButtonID id : buttons ) {
			this.buttons.get(id).enabled = on;
		}
	}
	
	protected void setPlanFromFields() {

		int xAnchor;
		try {
			xAnchor = Integer.parseInt(xAnchorField.getText());
		} catch (NumberFormatException e) {
			xAnchor = 0;
		}
		int zAnchor;
		try {
			zAnchor = Integer.parseInt(zAnchorField.getText());
		} catch (NumberFormatException e) {
			zAnchor = 0;
		}

		BlockXZCoords anchor = new BlockXZCoords(xAnchor, zAnchor);
		BlockXZCoords anchorRelativeTo;
		String planRelativeTo = null;
		switch (this.anchorMode.value()) {
		
		case RELATIVE_TO_ORIGIN :
			anchorRelativeTo = BlockXZCoords.origin();
			break;
		case RELATIVE_TO_SPAWN :
			ChunkCoordinates spawnPoint = this.mc.thePlayer.worldObj.getSpawnPoint();
			anchorRelativeTo = new BlockXZCoords(spawnPoint);
			break;
		case RELATIVE_TO_POSITION :
			anchorRelativeTo = new BlockXZCoords(this.mc.thePlayer);
			break;
		case RELATIVE_TO_PLAN :
			planRelativeTo = planNameCycler.value();
			anchorRelativeTo = new BlockXZCoords(plans.get(planRelativeTo).position.anchor);
			break;
		case RELATIVE_TO_COORDS :
			anchorRelativeTo = anchorRelativeToFromFields();
			savedInfo.get(nameSelected).position.anchorMode = EnumAnchorMode.RELATIVE_TO_COORDS;
			savedInfo.get(nameSelected).position.anchorRelativeToCoords = anchorRelativeTo;
			break;
			default :
				throw new RuntimeException("Unknown anchor mode");
		}
		anchor.add(anchorRelativeTo);
		
		EnumAnchorMode anchorModeToStore = ( anchorMode.value() == EnumAnchorMode.RELATIVE_TO_POSITION ) ? EnumAnchorMode.RELATIVE_TO_COORDS : anchorMode.value();
				
		WorldPlanRegistry.PlanPosition position = new WorldPlanRegistry.PlanPosition(anchor, anchorModeToStore, anchorRelativeTo, planRelativeTo, orientation.value());

		String planName = this.planNameField.getText();

		WorldPlanRegistry.PlanInfo planInfo = plans.get(nameSelected);
		planInfo.position = position;
		this.plans.put(planName, planInfo);
		if ( planName != nameSelected ) {
			plans.remove(nameSelected);
		}
		setFieldsFromPlanPosition(planName, planInfo.position);

	}
	
	protected BlockXZCoords anchorRelativeToFromFields() {
					int xAnchorRelativeTo;
		int zAnchorRelativeTo;
		try {
			xAnchorRelativeTo = Integer.parseInt(xAnchorRelativeToField.getText());
		} catch (NumberFormatException e) {
			xAnchorRelativeTo = 0;
		}
		try {
			zAnchorRelativeTo = Integer.parseInt(zAnchorRelativeToField.getText());
		} catch (NumberFormatException e) {
				zAnchorRelativeTo = 0;
		}
		return new BlockXZCoords(xAnchorRelativeTo, zAnchorRelativeTo);
	}

	protected void setFieldsFromPlanPosition(String planName, WorldPlanRegistry.PlanPosition position) {
		this.planNameField.setText(planName);
		this.xAnchorField.setText(Integer.toString(position.anchor.x - position.anchorRelativeToCoords.x));
		this.zAnchorField.setText(Integer.toString(position.anchor.z - position.anchorRelativeToCoords.z));
		this.orientation.advanceTo(position.orientation);

		boolean staleAnchor = false;
		if ( position.anchorMode == EnumAnchorMode.RELATIVE_TO_SPAWN ) {
			ChunkCoordinates spawnPoint = this.mc.thePlayer.worldObj.getSpawnPoint();
			BlockXZCoords spawnBlockXZCoords = new BlockXZCoords(spawnPoint);
			staleAnchor =  !position.anchorRelativeToCoords.equals(spawnBlockXZCoords);
		} else if ( position.anchorMode == EnumAnchorMode.RELATIVE_TO_PLAN ) {
			staleAnchor = !this.plans.containsKey(position.anchorRelativeToPlan) || (this.plans.get(position.anchorRelativeToPlan).position.anchor != position.anchor);
		}
		if ( staleAnchor ) {
			this.anchorMode.advanceTo(EnumAnchorMode.RELATIVE_TO_COORDS);
		} else {
			this.anchorMode.advanceTo(position.anchorMode);
		}
		if ( this.anchorMode.value() == EnumAnchorMode.RELATIVE_TO_COORDS ) {
			this.xAnchorRelativeToField.setText(Integer.toString(position.anchorRelativeToCoords.x));
			this.zAnchorRelativeToField.setText(Integer.toString(position.anchorRelativeToCoords.z));
		}
		
	}
	

	protected void actionPerformed(GuiButton button) {
		
		if ( button.enabled ) {
		
			switch ( ButtonID.values()[button.id] ) {

			case REMOVE :
				
				if ( plans.get(nameSelected).status == WorldPlanRegistry.PlanStatus.REMOVE ) {
					plans.get(nameSelected).status = WorldPlanRegistry.PlanStatus.OLD;
					setRemoved(false);
					buttons.get(ButtonID.REMOVE).displayString = REMOVE_PLAN_TEXT;
				} else {
					plans.get(nameSelected).status = WorldPlanRegistry.PlanStatus.REMOVE;
					setRemoved(true);
					buttons.get(ButtonID.REMOVE).displayString = DONT_REMOVE_PLAN_TEXT;
				}
				break;
				
			case REFRESH :
				
				if ( this.plans.get(nameSelected).status == WorldPlanRegistry.PlanStatus.REFRESH ) {
					this.plans.get(nameSelected).status = WorldPlanRegistry.PlanStatus.OLD;
					this.buttons.get(ButtonID.REFRESH).displayString = REFRESH_PLAN_TEXT;
				} else {
					this.plans.get(nameSelected).status = WorldPlanRegistry.PlanStatus.REFRESH;
					this.buttons.get(ButtonID.REFRESH).displayString = DEREFRESH_TEXT;
				}
				break;
			
			case ORIENTATION :
				
				orientation.advance();
				setButtonTexts();
				break;
				
			case ANCHOR_MODE :
				
				if ( anchorMode.value() == EnumAnchorMode.RELATIVE_TO_COORDS ) {
					BlockXZCoords anchorRelativeTo = anchorRelativeToFromFields();
					savedInfo.get(nameSelected).position.anchorMode = EnumAnchorMode.RELATIVE_TO_COORDS;
					savedInfo.get(nameSelected).position.anchorRelativeToCoords = anchorRelativeTo;
				}
				anchorMode.advance();
				setButtonTexts();
				if ( anchorMode.value() == EnumAnchorMode.RELATIVE_TO_COORDS ) {
					if ( savedInfo.get(nameSelected).position.anchorMode == EnumAnchorMode.RELATIVE_TO_COORDS ) {
						plans.get(nameSelected).position.anchorRelativeToCoords = savedInfo.get(nameSelected).position.anchorRelativeToCoords;
					}
					xAnchorRelativeToField.setText(Integer.toString(plans.get(nameSelected).position.anchorRelativeToCoords.x));
					zAnchorRelativeToField.setText(Integer.toString(plans.get(nameSelected).position.anchorRelativeToCoords.z));
					activeHeaders.add(HeaderID.RELATIVE_TO_X);
					activeHeaders.add(HeaderID.RELATIVE_TO_Z);
				} else {
					activeHeaders.remove(HeaderID.RELATIVE_TO_X);
					activeHeaders.remove(HeaderID.RELATIVE_TO_Z);
				}
				break;
				
			case RELATIVE_TO_PLAN :
				
				planNameCycler.advance();
				setButtonTexts();
				break;
				
			case DONE :

				setPlanFromFields();
				ConstructionFlower.instance.network.sendToServer(new LoadedPlansMessage(plans));

			case CANCEL_CHANGES :
				
				this.mc.displayGuiScreen((GuiScreen)null);
				
			default : 
				
				selectPlan.actionPerformed(button);
				break;
		
			}
		}
		
	}
	
	private void setRemoved(boolean isRemoved) {
		planSelectedIsToBeRemoved = isRemoved;
		buttons.get(ButtonID.ORIENTATION).enabled = !isRemoved;
		buttons.get(ButtonID.ANCHOR_MODE).enabled = !isRemoved;
		xAnchorField.setEnabled(!isRemoved);
		zAnchorField.setEnabled(!isRemoved);
		xAnchorRelativeToField.setEnabled(!isRemoved);
		zAnchorRelativeToField.setEnabled(!isRemoved);
		planNameField.setEnabled(!isRemoved);
	}
	
	@Override
	public void drawScreen(int p_73863_1_, int p_73863_2_, float p_73863_3_) {
		this.drawDefaultBackground();
		this.selectPlan.drawScreen(p_73863_1_, p_73863_2_, p_73863_3_);
		if ( this.planIsSelected ) {
			this.xAnchorField.drawTextBox();
			this.zAnchorField.drawTextBox();
			this.planNameField.drawTextBox();
			if ( this.anchorMode.value() == EnumAnchorMode.RELATIVE_TO_COORDS ) {
				this.xAnchorRelativeToField.drawTextBox();
				this.zAnchorRelativeToField.drawTextBox();
			}
			if ( anchorMode.value() != EnumAnchorMode.RELATIVE_TO_PLAN ) {
				buttons.get(ButtonID.RELATIVE_TO_PLAN).enabled = false;
				buttons.get(ButtonID.RELATIVE_TO_PLAN).visible = false;
			} else {
				buttons.get(ButtonID.RELATIVE_TO_PLAN).enabled = true;
				buttons.get(ButtonID.RELATIVE_TO_PLAN).visible = true;
			}
		}
		if ( activeHeaders.size() > 0 ) {
			// GL11.glPushMatrix();
			// GL11.glScalef(GuiConstants.HEADER_SCALE, GuiConstants.HEADER_SCALE, GuiConstants.HEADER_SCALE);
			for ( HeaderID header : activeHeaders ) {
				FontHelper.drawString(headerText.get(header), headerXPos.get(header) , headerYPos.get(header), GuiHeaderFont.getFont(), 1.0F, 1.0F, GuiConstants.HEADER_COLOR);
			}
			// GL11.glPopMatrix();
		}
		super.drawScreen(p_73863_1_, p_73863_2_, p_73863_3_);
	}
	
	@Override
	public void updateScreen() {
		super.updateScreen();
		if ( planIsSelected & !planSelectedIsToBeRemoved ) {
			xAnchorField.updateCursorCounter();
			zAnchorField.updateCursorCounter();
			planNameField.updateCursorCounter();
			if ( anchorMode.value() == EnumAnchorMode.RELATIVE_TO_COORDS ) {
				xAnchorRelativeToField.updateCursorCounter();
				zAnchorRelativeToField.updateCursorCounter();
			}
		}
	}
	
	@Override
	protected void keyTyped(char par1, int par2) {
        if ( xAnchorField.isFocused() ) {
            xAnchorField.textboxKeyTyped(par1, par2);
        }
        if ( zAnchorField.isFocused() ) {
            zAnchorField.textboxKeyTyped(par1, par2);
        }
        if ( planNameField.isFocused() ) {
        	planNameField.textboxKeyTyped(par1,  par2);
        }
        if ( xAnchorRelativeToField.isFocused() ) {
        	xAnchorRelativeToField.textboxKeyTyped(par1, par2);
        }
        if ( zAnchorRelativeToField.isFocused() ) {
        	zAnchorRelativeToField.textboxKeyTyped(par1, par2);
        }
    }
	
	@Override
	protected void mouseClicked(int x, int y, int buttonClicked) {
		super.mouseClicked(x, y, buttonClicked);
		if ( planIsSelected & !planSelectedIsToBeRemoved ) {
			xAnchorField.mouseClicked(x, y, buttonClicked);
			zAnchorField.mouseClicked(x, y, buttonClicked);
			planNameField.mouseClicked(x, y, buttonClicked);
			if ( anchorMode.value() == EnumAnchorMode.RELATIVE_TO_COORDS ) {
				xAnchorRelativeToField.mouseClicked(x, y, buttonClicked);
				zAnchorRelativeToField.mouseClicked(x, y, buttonClicked);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private int addSideButton(ButtonID id, String text, int yDown) {
		yDown += GuiConstants.VERTICAL_GUTTER;
		buttons.put(id, new GuiButton(id.ordinal(), this.width - GuiConstants.BUTTON_WIDTH - GuiConstants.HORIZONTAL_GUTTER, yDown, GuiConstants.BUTTON_WIDTH, GuiConstants.BUTTON_HEIGHT, text));
		buttonList.add(buttons.get(id));
		yDown += GuiConstants.BUTTON_HEIGHT;
		return yDown;
	}

	private void addHeadersAbove(HeaderID[] headers, int x, int y) {
		for ( HeaderID header : headers ) {
			y -= GuiConstants.SPACE_FOR_HEADER_ROW;
			headerXPos.put(header, x);
			headerYPos.put(header, y);
		}
	}

}
