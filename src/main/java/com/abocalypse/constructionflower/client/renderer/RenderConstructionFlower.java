package com.abocalypse.constructionflower.client.renderer;

import com.abocalypse.constructionflower.blocks.BlockConstructionFlower;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class RenderConstructionFlower implements ISimpleBlockRenderingHandler{

	private static double flowerBottom = 0.5D;
	private static double flowerHalfDiagonal = 0.25D;
	private static double flowerTop = flowerBottom + 2.0*flowerHalfDiagonal;

	private static double sepalHalfLength = 0.15D;
	private static double sepalHalfWidth = 0.07D;
	private static double sepalEastWestAngle = Math.PI/3D; // sixty degrees
	private static double sepalNorthSouthAngle = 2D*Math.PI/9D; // forty degrees
	
	private static double sepalBottom = flowerBottom;

	private static double sepalNorthSouthYDelta = sepalHalfLength*MathHelper.sin((float)sepalNorthSouthAngle);
	private static double sepalNorthSouthYCenter = sepalBottom + sepalNorthSouthYDelta;
	private static double sepalNorthSouthTop = sepalBottom + 2D*sepalNorthSouthYDelta;
	private static double sepalNorthSouthCenterXZDelta = sepalHalfLength*MathHelper.cos((float)sepalNorthSouthAngle);
	private static double sepalNorthSouthTopXZDelta = 2D*sepalHalfLength*MathHelper.cos((float)sepalNorthSouthAngle);

	private static double sepalEastWestYDelta = sepalHalfLength*MathHelper.sin((float)sepalEastWestAngle);
	private static double sepalEastWestYCenter = sepalBottom + sepalEastWestYDelta;
	private static double sepalEastWestTop = sepalBottom + 2D*sepalEastWestYDelta;
	private static double sepalEastWestCenterXZDelta = sepalHalfLength*MathHelper.cos((float)sepalEastWestAngle);
	private static double sepalEastWestTopXZDelta = 2D*sepalHalfLength*MathHelper.cos((float)sepalEastWestAngle);

	
	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelId,
			RenderBlocks renderer) {
	}
	
	@SideOnly(Side.CLIENT)
	private void renderFlower(IBlockAccess world, Block block, int x, int y, int z, RenderBlocks renderer)
	{
		Tessellator tessellator = Tessellator.instance;
		tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));

		double xWest = (double)x;
		double xCenter = xWest + 0.5D;
		double xEast = xWest + 1.0D;
		
		double yBottom = (double)y;
		double yTop = yBottom + 1.0D;
		double ySepalBottom = yBottom + sepalBottom;
		double ySepalNorthSouthCenter = yBottom + sepalNorthSouthYCenter;
		double ySepalNorthSouthTop = yBottom + sepalNorthSouthTop;
		double ySepalEastWestCenter = yBottom + sepalEastWestYCenter;
		double ySepalEastWestTop = yBottom + sepalEastWestTop;
		double yFlowerBottom = yBottom + flowerBottom;
		double yFlowerCenter = yFlowerBottom + flowerHalfDiagonal;
		double yFlowerTop = yBottom + flowerTop;
		
		double zNorth = (double)z;
		double zCenter = zNorth + 0.5D;
		double zSouth = zNorth + 1.0D;
		double zFlowerNorth = zCenter - flowerHalfDiagonal;
		double zFlowerSouth = zCenter + flowerHalfDiagonal;
		
		// Draw the stem and sepals in the local grass color
		int rgbLeafColorMultiplier = block.colorMultiplier(world, x, y, z); 
		float redLeafColorMultiplier = (float)(rgbLeafColorMultiplier >> 16 & 255) / 255.0F;
	    float greenLeafColorMultiplier = (float)(rgbLeafColorMultiplier >> 8 & 255) / 255.0F;
	    float blueLeafColorMultiplier = (float)(rgbLeafColorMultiplier & 255) / 255.0F;
		tessellator.setColorOpaque_F(redLeafColorMultiplier, greenLeafColorMultiplier, blueLeafColorMultiplier);

		// crossed squares
		IIcon stemIcon = ((BlockConstructionFlower) block).getWorldIcon(BlockConstructionFlower.PlantPart.STEM);
		double stemMinU = (double)stemIcon.getMinU();
		double stemMaxU = (double)stemIcon.getMaxU();
		double stemMinV = (double)stemIcon.getMinV();
		double stemMaxV = (double)stemIcon.getMaxV();
		
		// the northwest-southeast part of the cross
		tessellator.addVertexWithUV(xWest, yTop, zNorth, stemMinU, stemMinV);
		tessellator.addVertexWithUV(xWest, yBottom, zNorth, stemMinU, stemMaxV);
		tessellator.addVertexWithUV(xEast, yBottom, zSouth, stemMaxU, stemMaxV);
		tessellator.addVertexWithUV(xEast, yTop, zSouth, stemMaxU, stemMinV);

		// backwards to draw the other side
		tessellator.addVertexWithUV(xEast, yTop, zSouth, stemMaxU, stemMinV);
		tessellator.addVertexWithUV(xEast, yBottom, zSouth, stemMaxU, stemMaxV);
		tessellator.addVertexWithUV(xWest, yBottom, zNorth, stemMinU, stemMaxV);
		tessellator.addVertexWithUV(xWest, yTop, zNorth, stemMinU, stemMinV);
		
		// the southwest-northeast part of the cross
		tessellator.addVertexWithUV(xWest, yTop, zSouth, stemMinU, stemMinV);
		tessellator.addVertexWithUV(xWest, yBottom, zSouth, stemMinU, stemMaxV);
		tessellator.addVertexWithUV(xEast, yBottom, zNorth, stemMaxU, stemMaxV);		
		tessellator.addVertexWithUV(xEast, yTop, zNorth, stemMaxU, stemMinV);

		// backwards to draw the other side
		tessellator.addVertexWithUV(xEast, yTop, zNorth, stemMaxU, stemMinV);
		tessellator.addVertexWithUV(xEast, yBottom, zNorth, stemMaxU, stemMaxV);		
		tessellator.addVertexWithUV(xWest, yBottom, zSouth, stemMinU, stemMaxV);
		tessellator.addVertexWithUV(xWest, yTop, zSouth, stemMinU, stemMinV);
		
		// sepals
		IIcon sepalIcon = ((BlockConstructionFlower) block).getWorldIcon(BlockConstructionFlower.PlantPart.SEPAL);
		double sepalMinU = (double)sepalIcon.getMinU();
		double sepalMaxU = (double)sepalIcon.getInterpolatedU(2.0F);
		double sepalMinV = (double)sepalIcon.getMinV();
		double sepalMaxV = (double)sepalIcon.getInterpolatedV(2.0F);

		// east sepal
		// bottom corner, north corner, top corner, south corner
		tessellator.addVertexWithUV(xCenter, ySepalBottom, zCenter, sepalMinU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter + sepalEastWestCenterXZDelta, ySepalEastWestCenter, zCenter - sepalHalfWidth, sepalMaxU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter + sepalEastWestTopXZDelta, ySepalEastWestTop, zCenter, sepalMaxU, sepalMinV);
		tessellator.addVertexWithUV(xCenter + sepalEastWestCenterXZDelta, ySepalEastWestCenter, zCenter + sepalHalfWidth, sepalMinU, sepalMinV);
		//backwards to draw the other side
		tessellator.addVertexWithUV(xCenter + sepalEastWestCenterXZDelta, ySepalEastWestCenter, zCenter + sepalHalfWidth, sepalMinU, sepalMinV);
		tessellator.addVertexWithUV(xCenter + sepalEastWestTopXZDelta, ySepalEastWestTop, zCenter, sepalMaxU, sepalMinV);
		tessellator.addVertexWithUV(xCenter + sepalEastWestCenterXZDelta, ySepalEastWestCenter, zCenter - sepalHalfWidth, sepalMaxU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter, ySepalBottom, zCenter, sepalMinU, sepalMaxV);

		// south sepal
		// bottom corner, west corner, top corner, east corner
		tessellator.addVertexWithUV(xCenter, ySepalBottom, zCenter, sepalMinU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter - sepalHalfWidth, ySepalNorthSouthCenter, zCenter  + sepalNorthSouthCenterXZDelta, sepalMaxU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter, ySepalNorthSouthTop, zCenter + sepalNorthSouthTopXZDelta, sepalMaxU, sepalMinV);
		tessellator.addVertexWithUV(xCenter + sepalHalfWidth, ySepalNorthSouthCenter, zCenter + sepalNorthSouthCenterXZDelta, sepalMinU, sepalMinV);
		//backwards to draw the other side
		tessellator.addVertexWithUV(xCenter + sepalHalfWidth, ySepalNorthSouthCenter, zCenter + sepalNorthSouthCenterXZDelta, sepalMinU, sepalMinV);
		tessellator.addVertexWithUV(xCenter, ySepalNorthSouthTop, zCenter + sepalNorthSouthTopXZDelta, sepalMaxU, sepalMinV);
		tessellator.addVertexWithUV(xCenter - sepalHalfWidth, ySepalNorthSouthCenter, zCenter  + sepalNorthSouthCenterXZDelta, sepalMaxU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter, ySepalBottom, zCenter, sepalMinU, sepalMaxV);

		// north sepal
		// bottom corner, west corner, top corner, east corner
		tessellator.addVertexWithUV(xCenter, ySepalBottom, zCenter, sepalMinU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter - sepalHalfWidth, ySepalNorthSouthCenter, zCenter - sepalNorthSouthCenterXZDelta, sepalMaxU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter, ySepalNorthSouthTop, zCenter - sepalNorthSouthTopXZDelta, sepalMaxU, sepalMinV);
		tessellator.addVertexWithUV(xCenter + sepalHalfWidth, ySepalNorthSouthCenter, zCenter - sepalNorthSouthCenterXZDelta, sepalMinU, sepalMinV);
		//backwards to draw the other side
		tessellator.addVertexWithUV(xCenter + sepalHalfWidth, ySepalNorthSouthCenter, zCenter - sepalNorthSouthCenterXZDelta, sepalMinU, sepalMinV);
		tessellator.addVertexWithUV(xCenter, ySepalNorthSouthTop, zCenter - sepalNorthSouthTopXZDelta, sepalMaxU, sepalMinV);
		tessellator.addVertexWithUV(xCenter - sepalHalfWidth, ySepalNorthSouthCenter, zCenter - sepalNorthSouthCenterXZDelta, sepalMaxU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter, ySepalBottom, zCenter, sepalMinU, sepalMaxV);

		// west sepal
		// bottom corner, north corner, top corner, south corner
		tessellator.addVertexWithUV(xCenter, ySepalBottom, zCenter, sepalMinU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter - sepalEastWestCenterXZDelta, ySepalEastWestCenter, zCenter - sepalHalfWidth, sepalMaxU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter - sepalEastWestTopXZDelta, ySepalEastWestTop, zCenter, sepalMaxU, sepalMinV);
		tessellator.addVertexWithUV(xCenter - sepalEastWestCenterXZDelta, ySepalEastWestCenter, zCenter + sepalHalfWidth, sepalMinU, sepalMinV);
		//backwards to draw the other side
		tessellator.addVertexWithUV(xCenter - sepalEastWestCenterXZDelta, ySepalEastWestCenter, zCenter + sepalHalfWidth, sepalMinU, sepalMinV);
		tessellator.addVertexWithUV(xCenter - sepalEastWestTopXZDelta, ySepalEastWestTop, zCenter, sepalMaxU, sepalMinV);
		tessellator.addVertexWithUV(xCenter - sepalEastWestCenterXZDelta, ySepalEastWestCenter, zCenter - sepalHalfWidth, sepalMaxU, sepalMaxV);
		tessellator.addVertexWithUV(xCenter, ySepalBottom, zCenter, sepalMinU, sepalMaxV);

		
		// The sign should not be grass colored
		tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
		
		IIcon flowerIcon =  ((BlockConstructionFlower) block).getWorldIcon(BlockConstructionFlower.PlantPart.FLOWER);
		double flowerMinU = (double)flowerIcon.getMinU();
		double flowerMaxU = (double)flowerIcon.getMaxU();
		double flowerMinV = (double)flowerIcon.getMinV();
		double flowerMaxV = (double)flowerIcon.getMaxV();
		// the sign faces East-West
		// upper left corner of the icon is the north corner of the sign
		tessellator.addVertexWithUV(xCenter, yFlowerCenter, zFlowerNorth, flowerMinU, flowerMinV);
		// upper right corner of the icon is the top of the sign
		tessellator.addVertexWithUV(xCenter, yFlowerTop, zCenter, flowerMaxU, flowerMinV);
		// lower right corner of the icon is the south corner of the sign
		tessellator.addVertexWithUV(xCenter, yFlowerCenter, zFlowerSouth, flowerMaxU, flowerMaxV);
		// lower left corner of the icon is the bottom of the sign
		tessellator.addVertexWithUV(xCenter, yFlowerBottom, zCenter, flowerMinU, flowerMaxV);		

		// now backwards to draw the other side
		tessellator.addVertexWithUV(xCenter, yFlowerBottom, zCenter, flowerMinU, flowerMaxV);		
		tessellator.addVertexWithUV(xCenter, yFlowerCenter, zFlowerSouth, flowerMaxU, flowerMaxV);
		tessellator.addVertexWithUV(xCenter, yFlowerTop, zCenter, flowerMaxU, flowerMinV);
		tessellator.addVertexWithUV(xCenter, yFlowerCenter, zFlowerNorth, flowerMinU, flowerMinV);
		
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z,
			Block block, int modelId, RenderBlocks renderer) {
		renderFlower(world, block, x, y, z, renderer);
		return true;
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId) {
		return false;
	}

	@Override
	public int getRenderId() {
		return 0;
	}

}
