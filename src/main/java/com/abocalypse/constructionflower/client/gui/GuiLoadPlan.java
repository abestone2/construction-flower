package com.abocalypse.constructionflower.client.gui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import com.abocalypse.constructionflower.ConstructionFlower;
import com.abocalypse.constructionflower.lib.EnumAnchorMode;
import com.abocalypse.constructionflower.lib.EnumOrientation;
import com.abocalypse.constructionflower.network.LoadPlanMessage;
import com.abocalypse.constructionflower.plan.BlockXZCoords;
import com.abocalypse.constructionflower.plan.WorldPlanRegistry;
import com.abocalypse.constructionflower.truetyper.FontHelper;
import com.abocalypse.constructionflower.util.ArrayCycler;
import com.abocalypse.constructionflower.util.EnumCycler;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;

@SideOnly(Side.CLIENT)
public class GuiLoadPlan extends GuiScreen {
	
	private GuiScreen parentScreen;
	private boolean initial;
	private int planSpecSelected;
	private List<String> availablePlanSpecs;
	private Map<String, BlockXZCoords> existingPlans;
	
	private final String doneText;
	private final String noSelectionCancelText;
	private final String selectionCancelText;
	private static final String cancelWorldText = "Cancel World Creation";
	
	private PlanSpecList selectPlanSpec;
	private boolean relativeToPlanAllowed;
	private String planSelected;
	private GuiTextField xAnchorField;
	private GuiTextField zAnchorField;
	private GuiTextField xAnchorRelativeToField;
	private GuiTextField zAnchorRelativeToField;
	private EnumCycler<EnumOrientation> orientation;
	private EnumCycler<EnumAnchorMode> anchorMode;
	private EnumSet<EnumAnchorMode> skipAnchorModes;
	private ArrayCycler<String> existingPlansCycler;
	private GuiTextField planNameField;
	
	private static enum ButtonID {
		CANCEL, DONE, CANCEL_WORLD, ORIENTATION, ANCHOR_MODE, RELATIVE_TO_PLAN, SCROLL_UP, SCROLL_DOWN
	}
	
	private EnumMap<ButtonID, GuiButton> buttons;
	
	private static enum HeaderID {
		ANCHOR_AT, AT_X, AT_Z, PLAN_SPEC_HEADER, RELATIVE_TO_X, RELATIVE_TO_Z, PLAN_NAME;
	}
	private static final EnumMap<HeaderID, String> headerText = new EnumMap<HeaderID, String>(HeaderID.class);
	static {
		headerText.put(HeaderID.ANCHOR_AT, "Anchor at:");
		headerText.put(HeaderID.AT_X, "X");
		headerText.put(HeaderID.AT_Z, "Z");
		headerText.put(HeaderID.PLAN_SPEC_HEADER, "Choose plan specification file:");
		headerText.put(HeaderID.RELATIVE_TO_X, "X");
		headerText.put(HeaderID.RELATIVE_TO_Z, "Z");
		headerText.put(HeaderID.PLAN_NAME, "Plan name:");
	}
	private EnumMap<HeaderID, Integer> headerXPos;
	private EnumMap<HeaderID, Integer> headerYPos;
	private EnumSet<HeaderID> activeHeaders;
	
	@SideOnly(Side.CLIENT)
	class PlanSpecList extends GuiSlot {

		public PlanSpecList(int bottom, int top) {
			super(GuiLoadPlan.this.mc, GuiLoadPlan.this.width - 2*GuiConstants.HORIZONTAL_GUTTER - GuiConstants.BUTTON_WIDTH - GuiConstants.SCROLL_BAR_WIDTH, bottom - 2*GuiConstants.VERTICAL_GUTTER, GuiConstants.VERTICAL_GUTTER, bottom, GuiConstants.SLOT_HEIGHT);
		}

		@Override
		protected int getSize() {
			return availablePlanSpecs.size();
		}

		@Override
		// Not clear what the last two arguments are supposed to be for
		protected void elementClicked(int slotClicked, boolean doubleClicked,
				int p_148144_3_, int p_148144_4_) {
			GuiLoadPlan.this.planSpecSelected = slotClicked;
			boolean validPlan = GuiLoadPlan.this.planSpecSelected >= 0 && GuiLoadPlan.this.planSpecSelected < this.getSize();
			GuiLoadPlan.this.buttons.get(ButtonID.DONE).enabled = validPlan;
			GuiLoadPlan.this.buttons.get(ButtonID.ORIENTATION).enabled = validPlan;
			GuiLoadPlan.this.buttons.get(ButtonID.ANCHOR_MODE).enabled = validPlan;
			GuiLoadPlan.this.setButtonTexts();
		}

		@Override
		protected boolean isSelected(int slot) {
			return slot == GuiLoadPlan.this.planSpecSelected;
		}

		@Override
		protected void drawBackground() {
			GuiLoadPlan.this.drawDefaultBackground();
		}

		@Override
		// Not clear what the last four arguments are supposed to be for.
		protected void drawSlot(int slot, int x, int y, int p_148126_4_, Tessellator p_148126_5_, int p_148126_6_, int p_148126_7_) {
            GuiLoadPlan.this.drawString(GuiLoadPlan.this.fontRendererObj, GuiLoadPlan.this.availablePlanSpecs.get(slot), x + 2, y + 1, 0xFFFFFF);
		}
		
	}
	
	// the first arg is the usual parent screen that every GuiScreen takes
	// the second tells whether we are loading an initial plan or loading
	//  a new plan in-game
	public GuiLoadPlan(GuiScreen screen, boolean initial, List<String> planSpecFiles, Map<String, BlockXZCoords> existingPlans) {
		this.parentScreen = screen;
		this.initial = initial;
		this.availablePlanSpecs = planSpecFiles;
		this.existingPlans = existingPlans;
		planSpecSelected = -1;
		if ( initial ) {
			doneText = "Create With Selected Plan";
			noSelectionCancelText = "Continue With No Plan";
			selectionCancelText = "Cancel Plan and Continue";
		} else {
			doneText = "Done";
			noSelectionCancelText = "Cancel";
			selectionCancelText = "Cancel";
		}
		buttons = new EnumMap<ButtonID, GuiButton>(ButtonID.class);
		activeHeaders = EnumSet.noneOf(HeaderID.class);
		headerXPos = new EnumMap<HeaderID, Integer>(HeaderID.class);
		headerYPos = new EnumMap<HeaderID, Integer>(HeaderID.class);
	}
	
	@Override
	public void initGui() {

		// if initial is true, we are being called by a client which is en route to
		// launching a local integrated server; therefore, should look for the plan spec
		// files locally
		// (when initial is false OTOH, we have been called from the client-side handler
		//  of an AvailablePlanSpecsMessage, which, in case the client is connected to
		//  a remote server, has transmitted a list of plan specs stored remotely; in that
		//  case this.availablePlans was already loaded up in the constructor)
		if ( initial ) {
			this.availablePlanSpecs = WorldPlanRegistry.getAvailablePlanSpecFiles();
		}
		
		if ( availablePlanSpecs.size() == 0 ) {
			if ( initial ) {
				((GuiCreateConstructionFlowerWorld)parentScreen).continueCreatingWorld();
			} else {
				this.mc.displayGuiScreen(new GuiErrorScreen("No plans available", ""));
				return;
			}
		}

		
		Keyboard.enableRepeatEvents(true);
		buttonList.clear();
		
		int y = this.height;
		if ( initial ) {
			y = addButtonRow(ButtonID.CANCEL_WORLD, cancelWorldText, y);
		}
		y = addButtonRow(ButtonID.CANCEL, "", ButtonID.DONE, doneText, y);
		buttons.get(ButtonID.DONE).enabled = false;

		int x;
		int yDown = GuiConstants.VERTICAL_GUTTER + 2*GuiConstants.SPACE_FOR_HEADER_ROW;
		int textFieldWidth = (GuiConstants.BUTTON_WIDTH - GuiConstants.HORIZONTAL_GUTTER)/2;
		
		int sideButtonFirstColumn = width - GuiConstants.HORIZONTAL_GUTTER - GuiConstants.BUTTON_WIDTH; 
		int sideButtonSecondColumn = width - GuiConstants.HORIZONTAL_GUTTER - textFieldWidth; 
		
		x = sideButtonFirstColumn;
		xAnchorField = new GuiTextField(this.fontRendererObj, x, yDown, textFieldWidth, GuiConstants.TEXT_FIELD_HEIGHT);
		xAnchorField.setText("0");
		addHeadersAbove(new HeaderID[]{HeaderID.AT_X, HeaderID.ANCHOR_AT}, x, yDown);
		activeHeaders.add(HeaderID.ANCHOR_AT);
		activeHeaders.add(HeaderID.AT_X);
		x = sideButtonSecondColumn;
		zAnchorField = new GuiTextField(this.fontRendererObj, x, yDown, textFieldWidth, GuiConstants.TEXT_FIELD_HEIGHT);
		zAnchorField.setText("0");
		addHeadersAbove(new HeaderID[]{HeaderID.AT_Z}, x, yDown);
		activeHeaders.add(HeaderID.AT_Z);

		yDown += GuiConstants.TEXT_FIELD_HEIGHT + GuiConstants.VERTICAL_GUTTER;
		anchorMode = new EnumCycler<EnumAnchorMode>(EnumAnchorMode.class);
		if ( initial ) {
			skipAnchorModes = EnumSet.of(EnumAnchorMode.RELATIVE_TO_PLAN, EnumAnchorMode.RELATIVE_TO_POSITION);
			relativeToPlanAllowed = false;
		} else if ( this.existingPlans.size() == 0 ) {
			skipAnchorModes = EnumSet.of(EnumAnchorMode.RELATIVE_TO_PLAN);
			relativeToPlanAllowed = false;
		} else {
			relativeToPlanAllowed = true;
		}

		x = sideButtonFirstColumn;
		yDown = addSideButton(ButtonID.ANCHOR_MODE, "", x, yDown);
		buttons.get(ButtonID.ANCHOR_MODE).enabled = false;

		yDown += GuiConstants.VERTICAL_GUTTER;
		if ( relativeToPlanAllowed ) {
			existingPlansCycler = new ArrayCycler<String>(new ArrayList<String>(this.existingPlans.keySet()));
			addSideButton(ButtonID.RELATIVE_TO_PLAN, "", x, yDown);
			buttons.get(ButtonID.RELATIVE_TO_PLAN).enabled = false;
			buttons.get(ButtonID.RELATIVE_TO_PLAN).visible = false;
		} else {
			planSelected = null;
		}

		yDown += GuiConstants.SPACE_FOR_HEADER_ROW;
		xAnchorRelativeToField = new GuiTextField(this.fontRendererObj, x, yDown, textFieldWidth, GuiConstants.TEXT_FIELD_HEIGHT);
		addHeadersAbove(new HeaderID[]{HeaderID.RELATIVE_TO_X}, x, yDown);
		xAnchorRelativeToField.setText("0");
		x = sideButtonSecondColumn;
		zAnchorRelativeToField = new GuiTextField(this.fontRendererObj, x, yDown, textFieldWidth, GuiConstants.TEXT_FIELD_HEIGHT);
		addHeadersAbove(new HeaderID[]{HeaderID.RELATIVE_TO_Z}, x, yDown);
		zAnchorRelativeToField.setText("0");

		x = sideButtonFirstColumn;
		yDown += GuiConstants.VERTICAL_GUTTER + GuiConstants.TEXT_FIELD_HEIGHT;
		this.orientation = new EnumCycler<EnumOrientation>(EnumOrientation.class);
		yDown = addSideButton(ButtonID.ORIENTATION, "", x, yDown);
		buttons.get(ButtonID.ORIENTATION).enabled = false;
		if ( !initial ) {
			yDown += GuiConstants.VERTICAL_GUTTER + GuiConstants.SPACE_FOR_HEADER_ROW;
			this.planNameField = new GuiTextField(this.fontRendererObj, this.width - GuiConstants.HORIZONTAL_GUTTER - GuiConstants.BUTTON_WIDTH, yDown, GuiConstants.BUTTON_WIDTH, GuiConstants.TEXT_FIELD_HEIGHT);
			this.planNameField.setText("New Plan");
			addHeadersAbove(new HeaderID[]{HeaderID.PLAN_NAME}, x, yDown);
			activeHeaders.add(HeaderID.PLAN_NAME);
			yDown += GuiConstants.TEXT_FIELD_HEIGHT;
		}
		
		yDown = GuiConstants.VERTICAL_GUTTER + GuiConstants.SPACE_FOR_HEADER_ROW;
		this.selectPlanSpec = new PlanSpecList(y, yDown);
		this.selectPlanSpec.registerScrollButtons(ButtonID.SCROLL_UP.ordinal(), ButtonID.SCROLL_DOWN.ordinal());
		addHeadersAbove(new HeaderID[]{HeaderID.PLAN_SPEC_HEADER}, GuiConstants.HORIZONTAL_GUTTER, yDown);
		activeHeaders.add(HeaderID.PLAN_SPEC_HEADER);
		
		setButtonTexts();
	}
	
	private void setButtonTexts() {
		buttons.get(ButtonID.ANCHOR_MODE).displayString = GuiConstants.ANCHOR_MODE_TEXT.get(anchorMode.value());
		buttons.get(ButtonID.ORIENTATION).displayString = GuiConstants.ORIENTATION_TEXT.get(orientation.value());
		if ( this.relativeToPlanAllowed ) {
			buttons.get(ButtonID.RELATIVE_TO_PLAN).displayString = existingPlansCycler.value();
		}
		if ( planSpecSelected >= 0 ) {
			buttons.get(ButtonID.CANCEL).displayString = selectionCancelText;
		} else {
			buttons.get(ButtonID.CANCEL).displayString = noSelectionCancelText;
		}
	}
	
	public void onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false);
    }

	protected void actionPerformed(GuiButton button) {
		
		if ( button.enabled ) {
		
			switch ( ButtonID.values()[button.id]) {
		
			case CANCEL :
				if ( initial ) {
					((GuiCreateConstructionFlowerWorld)parentScreen).continueCreatingWorld();
				} else {
					this.mc.displayGuiScreen((GuiScreen)null);
				}
				break;
			case DONE :
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
				switch (anchorMode.value()) {
				case RELATIVE_TO_ORIGIN :
					anchorRelativeTo = BlockXZCoords.origin();
					break;
				case RELATIVE_TO_SPAWN :
					if ( initial ) {
						anchorRelativeTo = BlockXZCoords.origin();
					} else {
						anchorRelativeTo = new BlockXZCoords(this.mc.thePlayer.worldObj.getSpawnPoint());
					}
					break;
				case RELATIVE_TO_POSITION :
					anchorRelativeTo = new BlockXZCoords(this.mc.thePlayer);
					break;
				case RELATIVE_TO_PLAN :
					this.planSelected = existingPlansCycler.value();
					anchorRelativeTo = existingPlans.get(this.planSelected);
					break;
				case RELATIVE_TO_COORDS :
					int xAnchorRelativeTo;
					try {
						xAnchorRelativeTo = Integer.parseInt(xAnchorRelativeToField.getText());
					} catch (NumberFormatException e) {
						xAnchorRelativeTo = 0;
					}
					int zAnchorRelativeTo;
					try {
						zAnchorRelativeTo = Integer.parseInt(zAnchorRelativeToField.getText());
					} catch (NumberFormatException e) {
						zAnchorRelativeTo = 0;
					}
					anchorRelativeTo = new BlockXZCoords(xAnchorRelativeTo, zAnchorRelativeTo);
					break;
				default :
					throw new RuntimeException("Unknown anchor mode");
				}
				anchor.add(anchorRelativeTo);
				
				EnumAnchorMode anchorModeToStore = ( anchorMode.value() == EnumAnchorMode.RELATIVE_TO_POSITION ) ? EnumAnchorMode.RELATIVE_TO_COORDS : anchorMode.value();
				
				WorldPlanRegistry.PlanPosition position = new WorldPlanRegistry.PlanPosition(anchor, anchorModeToStore, anchorRelativeTo, this.planSelected, orientation.value());
				
				if ( initial ) {

					String levelName = ReflectionHelper.getPrivateValue(GuiCreateWorld.class, (GuiCreateWorld)parentScreen, "field_146336_i");
					WorldPlanRegistry.initialPlan(levelName, position, availablePlanSpecs.get(planSpecSelected));
					((GuiCreateConstructionFlowerWorld)parentScreen).continueCreatingWorld();

				} else {
					
					String planName = planNameField.getText();
					ConstructionFlower.instance.network.sendToServer(new LoadPlanMessage(position, planName, availablePlanSpecs.get(planSpecSelected)));
					this.mc.displayGuiScreen((GuiScreen)null);

				}
				break;
				
			case CANCEL_WORLD :
				if ( initial ) {
					((GuiCreateConstructionFlowerWorld)parentScreen).cancelCreatingWorld();
				}
				break;
			case ORIENTATION :
				orientation.advance();
				setButtonTexts();
				break;
			case ANCHOR_MODE :
				anchorMode.advanceToNot(this.skipAnchorModes);
				if ( this.relativeToPlanAllowed ) {
					if ( anchorMode.value() == EnumAnchorMode.RELATIVE_TO_PLAN ) {
						buttons.get(ButtonID.RELATIVE_TO_PLAN).enabled = true;
						buttons.get(ButtonID.RELATIVE_TO_PLAN).visible = true;
					} else {
						buttons.get(ButtonID.RELATIVE_TO_PLAN).enabled = false;
						buttons.get(ButtonID.RELATIVE_TO_PLAN).visible = false;
					}
				}
				if ( anchorMode.value() == EnumAnchorMode.RELATIVE_TO_COORDS ) {
					activeHeaders.add(HeaderID.RELATIVE_TO_X);
					activeHeaders.add(HeaderID.RELATIVE_TO_Z);
				} else {
					activeHeaders.remove(HeaderID.RELATIVE_TO_X);
					activeHeaders.remove(HeaderID.RELATIVE_TO_Z);
				}
				setButtonTexts();
				break;
			case RELATIVE_TO_PLAN :
				existingPlansCycler.advance();
				setButtonTexts();
				break;
			default : 
				selectPlanSpec.actionPerformed(button);
				break;
		
			}
		}
		
	}

	@Override
	public void drawScreen(int p_73863_1_, int p_73863_2_, float p_73863_3_) {
		this.drawDefaultBackground();
		this.selectPlanSpec.drawScreen(p_73863_1_, p_73863_2_, p_73863_3_);
		this.xAnchorField.drawTextBox();
		this.zAnchorField.drawTextBox();
		if ( !initial ) {
			this.planNameField.drawTextBox();
		}
		if ( this.anchorMode.value() == EnumAnchorMode.RELATIVE_TO_COORDS ) {
				this.xAnchorRelativeToField.drawTextBox();
				this.zAnchorRelativeToField.drawTextBox();
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
		this.xAnchorField.updateCursorCounter();
		this.zAnchorField.updateCursorCounter();
		if ( !initial ) {
			this.planNameField.updateCursorCounter();
		}
		if ( this.anchorMode.value() == EnumAnchorMode.RELATIVE_TO_COORDS ) {
			this.xAnchorRelativeToField.updateCursorCounter();
			this.zAnchorRelativeToField.updateCursorCounter();
		}
	}
	
	@Override
	protected void keyTyped(char par1, int par2) {
        if ( this.xAnchorField.isFocused() ) {
            this.xAnchorField.textboxKeyTyped(par1, par2);
        }
        if ( this.zAnchorField.isFocused() ) {
            this.zAnchorField.textboxKeyTyped(par1, par2);
        }
        if ( !initial && this.planNameField.isFocused() ) {
        	this.planNameField.textboxKeyTyped(par1,  par2);
        }
        if ( this.anchorMode.value() == EnumAnchorMode.RELATIVE_TO_COORDS ) {
        	if ( this.xAnchorRelativeToField.isFocused() ) {
        		this.xAnchorRelativeToField.textboxKeyTyped(par1, par2);
        	}
        	if ( this.zAnchorRelativeToField.isFocused() ) {
        		this.zAnchorRelativeToField.textboxKeyTyped(par1, par2);
        	}
        }
    }
	
	@Override
	protected void mouseClicked(int x, int y, int buttonClicked) {
		super.mouseClicked(x, y, buttonClicked);
		this.xAnchorField.mouseClicked(x, y, buttonClicked);
		this.zAnchorField.mouseClicked(x, y, buttonClicked);
		if ( !initial ) {
			this.planNameField.mouseClicked(x, y, buttonClicked);
		}
		if ( this.anchorMode.value() == EnumAnchorMode.RELATIVE_TO_COORDS ) {
			this.xAnchorRelativeToField.mouseClicked(x, y, buttonClicked);
			this.zAnchorRelativeToField.mouseClicked(x, y, buttonClicked);
		}
	}
	
	@SuppressWarnings("unchecked")
	private int addButtonRow(ButtonID firstID, String firstText, ButtonID secondID, String secondText, int y) {
		y -= GuiConstants.BUTTON_HEIGHT + GuiConstants.VERTICAL_GUTTER;
		buttons.put(firstID, new GuiButton(firstID.ordinal(), this.width/2 - GuiConstants.BUTTON_WIDTH - GuiConstants.HORIZONTAL_GUTTER, y, GuiConstants.BUTTON_WIDTH, GuiConstants.BUTTON_HEIGHT, firstText));
		buttonList.add(buttons.get(firstID)); 
		buttons.put(secondID, new GuiButton(secondID.ordinal(), this.width/2 + GuiConstants.HORIZONTAL_GUTTER, y, GuiConstants.BUTTON_WIDTH, GuiConstants.BUTTON_HEIGHT, secondText));
		buttonList.add(buttons.get(secondID));
		return y;
	}

	@SuppressWarnings("unchecked")
	private int addButtonRow(ButtonID id, String firstText, int y) {
		y -= GuiConstants.BUTTON_HEIGHT + GuiConstants.VERTICAL_GUTTER;
		buttons.put(id, new GuiButton(id.ordinal(), this.width/2 - GuiConstants.BUTTON_WIDTH/2, y, GuiConstants.BUTTON_WIDTH, GuiConstants.BUTTON_HEIGHT, firstText));
		buttonList.add(buttons.get(id));
		return y;
	}
	
	@SuppressWarnings("unchecked")
	private int addSideButton(ButtonID id, String text, int x, int yDown) {
		yDown += GuiConstants.VERTICAL_GUTTER;
		buttons.put(id, new GuiButton(id.ordinal(), x, yDown, GuiConstants.BUTTON_WIDTH, GuiConstants.BUTTON_HEIGHT, text));
		buttonList.add(buttons.get(id));
		yDown += GuiConstants.BUTTON_HEIGHT;
		return yDown;
	}

	private void addHeadersAbove(HeaderID[] headers, int x, int y) {
		for ( HeaderID header : headers ) {
			y -= GuiConstants.HEADER_VERTICAL_GUTTER + GuiConstants.HEADER_HEIGHT;
			headerXPos.put(header, x);
			headerYPos.put(header, y);
		}
	}
}
