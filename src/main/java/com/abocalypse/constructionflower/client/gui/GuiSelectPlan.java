package com.abocalypse.constructionflower.client.gui;

import java.util.EnumMap;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.abocalypse.constructionflower.ConstructionFlower;
import com.abocalypse.constructionflower.network.LoadPlanMessage;
import com.abocalypse.constructionflower.plan.PlanPartSpec;
import com.abocalypse.constructionflower.plan.WorldPlanRegistry;
import com.abocalypse.constructionflower.util.CyclicalEnum;

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
import net.minecraft.world.World;

@SideOnly(Side.CLIENT)
public class GuiSelectPlan extends GuiScreen {
	
	private GuiScreen parentScreen;
	private boolean initial;
	private int planSelected;
	private List<String> availablePlans;
	
	private final String doneText;
	private final String noSelectionCancelText;
	private final String selectionCancelText;
	private static final String cancelWorldText = "Cancel World Creation";
	
	private static final EnumMap<PlanPartSpec.Orientation, String> orientationText;
	static {
		orientationText = new EnumMap<PlanPartSpec.Orientation, String>(PlanPartSpec.Orientation.class);
		orientationText.put(PlanPartSpec.Orientation.TOPNORTH, "Top of Plan is North");
		orientationText.put(PlanPartSpec.Orientation.TOPEAST, "Top of Plan is East");
		orientationText.put(PlanPartSpec.Orientation.TOPSOUTH, "Top of Plan is South");
		orientationText.put(PlanPartSpec.Orientation.TOPWEST, "Top of Plan is West");
	}
	
	private static final EnumMap<WorldPlanRegistry.AnchorMode, String> anchorModeText;
	static {
		anchorModeText= new EnumMap<WorldPlanRegistry.AnchorMode, String>(WorldPlanRegistry.AnchorMode.class);
		anchorModeText.put(WorldPlanRegistry.AnchorMode.RELATIVE_TO_ORIGIN, "Relative to Origin");
		anchorModeText.put(WorldPlanRegistry.AnchorMode.RELATIVE_TO_SPAWN, "Relative to Spawn Point");
	}

	private PlanList selectPlanSpec;
	private GuiTextField xAnchorField;
	private GuiTextField zAnchorField;
	private CyclicalEnum<PlanPartSpec.Orientation> orientation;
	private CyclicalEnum<WorldPlanRegistry.AnchorMode> anchorMode;
	private GuiTextField planNameField;
	
	private static enum ButtonID {
		CANCEL, DONE, CANCEL_WORLD, ORIENTATION, ANCHOR_MODE, SCROLL_UP, SCROLL_DOWN
	}
	
	private EnumMap<ButtonID, GuiButton> buttons;
	
	@SideOnly(Side.CLIENT)
	class PlanList extends GuiSlot {

		public PlanList(int bottom) {
			super(GuiSelectPlan.this.mc, GuiSelectPlan.this.width - 2*GuiConstants.HORIZONTAL_GUTTER - GuiConstants.BUTTON_WIDTH, bottom - 2*GuiConstants.VERTICAL_GUTTER, GuiConstants.VERTICAL_GUTTER, bottom, GuiConstants.SLOT_HEIGHT);
		}

		@Override
		protected int getSize() {
			return availablePlans.size();
		}

		@Override
		// Not clear what the last two arguments are supposed to be for
		protected void elementClicked(int slotClicked, boolean doubleClicked,
				int p_148144_3_, int p_148144_4_) {
			GuiSelectPlan.this.planSelected = slotClicked;
			boolean validPlan = GuiSelectPlan.this.planSelected >= 0 && GuiSelectPlan.this.planSelected < this.getSize();
			GuiSelectPlan.this.buttons.get(ButtonID.DONE).enabled = validPlan;
			GuiSelectPlan.this.buttons.get(ButtonID.ORIENTATION).enabled = validPlan;
			GuiSelectPlan.this.buttons.get(ButtonID.ANCHOR_MODE).enabled = validPlan;
			GuiSelectPlan.this.setButtonTexts();
		}

		@Override
		protected boolean isSelected(int slot) {
			return slot == GuiSelectPlan.this.planSelected;
		}

		@Override
		protected void drawBackground() {
			GuiSelectPlan.this.drawDefaultBackground();
		}

		@Override
		// Not clear what the last four arguments are supposed to be for.
		protected void drawSlot(int slot, int x, int y, int p_148126_4_, Tessellator p_148126_5_, int p_148126_6_, int p_148126_7_) {
            GuiSelectPlan.this.drawString(GuiSelectPlan.this.fontRendererObj, GuiSelectPlan.this.availablePlans.get(slot), x + 2, y + 1, 0xFFFFFF);
		}
		
	}
	
	// the first argument says whether this is an initial plan load (during world
	// creation) or not
	// the second is the usual parent screen that every GuiScreen takes
	// the third is the world we are loading a plan into; for initial loads this
	//  will be null (or whatever, it won't be used)
	public GuiSelectPlan(GuiScreen screen, World world, List<String> planSpecFiles) {
		this.parentScreen = screen;
		this.initial = world == null;
		this.availablePlans = planSpecFiles;
		this.planSelected = -1;
		if ( initial ) {
			this.doneText = "Create With Selected Plan";
			this.noSelectionCancelText = "Continue With No Plan";
			this.selectionCancelText = "Cancel Plan and Continue";
		} else {
			this.doneText = "Done";
			this.noSelectionCancelText = "Cancel";
			this.selectionCancelText = "Cancel";
		}
		this.buttons = new EnumMap<ButtonID, GuiButton>(ButtonID.class);
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
			this.availablePlans = WorldPlanRegistry.getAvailablePlans();
		}
		
		if ( availablePlans.size() == 0 ) {
			if ( initial ) {
				this.mc.displayGuiScreen(parentScreen);
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
		
		int yDown = GuiConstants.VERTICAL_GUTTER;
		int textFieldWidth = (GuiConstants.BUTTON_WIDTH - GuiConstants.HORIZONTAL_GUTTER)/2;
		this.xAnchorField = new GuiTextField(this.fontRendererObj, this.width - GuiConstants.HORIZONTAL_GUTTER - GuiConstants.BUTTON_WIDTH, yDown, textFieldWidth, GuiConstants.TEXT_FIELD_HEIGHT);
		this.zAnchorField = new GuiTextField(this.fontRendererObj, this.width - GuiConstants.HORIZONTAL_GUTTER - textFieldWidth, yDown, textFieldWidth, GuiConstants.TEXT_FIELD_HEIGHT);
		this.xAnchorField.setText("0");
		this.zAnchorField.setText("0");
		yDown += GuiConstants.TEXT_FIELD_HEIGHT;
		this.anchorMode = new CyclicalEnum<WorldPlanRegistry.AnchorMode>(WorldPlanRegistry.AnchorMode.class);
		yDown = addSideButton(ButtonID.ANCHOR_MODE, "", yDown);
		buttons.get(ButtonID.ANCHOR_MODE).enabled = false;
		this.orientation = new CyclicalEnum<PlanPartSpec.Orientation>(PlanPartSpec.Orientation.class);
		yDown = addSideButton(ButtonID.ORIENTATION, "", yDown);
		buttons.get(ButtonID.ORIENTATION).enabled = false;
		if ( !initial ) {
			this.planNameField = new GuiTextField(this.fontRendererObj, this.width - GuiConstants.HORIZONTAL_GUTTER - GuiConstants.BUTTON_WIDTH, yDown, GuiConstants.BUTTON_WIDTH, GuiConstants.TEXT_FIELD_HEIGHT);
			this.planNameField.setText("New Plan");
			yDown += GuiConstants.TEXT_FIELD_HEIGHT;
		}
		selectPlanSpec = new PlanList(y);
		// Not sure which is actually up and which is down.
		selectPlanSpec.registerScrollButtons(ButtonID.SCROLL_UP.ordinal(), ButtonID.SCROLL_DOWN.ordinal());
		
		setButtonTexts();
	}
	
	private void setButtonTexts() {
		buttons.get(ButtonID.ANCHOR_MODE).displayString = anchorModeText.get(anchorMode.value());
		buttons.get(ButtonID.ORIENTATION).displayString = orientationText.get(orientation.value());
		if ( planSelected >= 0 ) {
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
				if ( initial ) {

					String levelName = ReflectionHelper.getPrivateValue(GuiCreateWorld.class, (GuiCreateWorld)parentScreen, "field_146336_i");
					WorldPlanRegistry.initialPlan(levelName, xAnchor, zAnchor, anchorMode.value(), orientation.value(), availablePlans.get(planSelected));
					((GuiCreateConstructionFlowerWorld)parentScreen).continueCreatingWorld();

				} else {
					
					String planName = planNameField.getText();
					ConstructionFlower.instance.network.sendToServer(new LoadPlanMessage(xAnchor, zAnchor, anchorMode.value(), orientation.value(), planName, availablePlans.get(planSelected)));
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
				anchorMode.advance();
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
		this.planNameField.drawTextBox();
		super.drawScreen(p_73863_1_, p_73863_2_, p_73863_3_);
	}
	
	@Override
	public void updateScreen() {
		super.updateScreen();
		this.xAnchorField.updateCursorCounter();
		this.zAnchorField.updateCursorCounter();
		this.planNameField.updateCursorCounter();
	}
	
	@Override
	protected void keyTyped(char par1, int par2) {
        if ( this.xAnchorField.isFocused() ) {
            this.xAnchorField.textboxKeyTyped(par1, par2);
        }
        if ( this.zAnchorField.isFocused() ) {
            this.zAnchorField.textboxKeyTyped(par1, par2);
        }
        if ( this.planNameField.isFocused() ) {
        	this.planNameField.textboxKeyTyped(par1,  par2);
        }
    }
	
	@Override
	protected void mouseClicked(int x, int y, int buttonClicked) {
		super.mouseClicked(x, y, buttonClicked);
		this.xAnchorField.mouseClicked(x, y, buttonClicked);
		this.zAnchorField.mouseClicked(x, y, buttonClicked);
		this.planNameField.mouseClicked(x, y, buttonClicked);
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
	private int addSideButton(ButtonID id, String text, int yDown) {
		yDown += GuiConstants.VERTICAL_GUTTER;
		buttons.put(id, new GuiButton(id.ordinal(), this.width - GuiConstants.BUTTON_WIDTH - GuiConstants.HORIZONTAL_GUTTER, yDown, GuiConstants.BUTTON_WIDTH, GuiConstants.BUTTON_HEIGHT, text));
		buttonList.add(buttons.get(id));
		yDown += GuiConstants.BUTTON_HEIGHT;
		return yDown;
	}

}
