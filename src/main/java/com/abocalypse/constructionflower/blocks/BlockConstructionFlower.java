package com.abocalypse.constructionflower.blocks;

import java.util.EnumMap;
import java.util.List;

import com.abocalypse.constructionflower.constructionflower;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.client.renderer.texture.IIconRegister;
// import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
// import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.IPlantable;

public class BlockConstructionFlower extends BlockFlower implements IPlantable{

	public static enum PART {
		STEM, SEPAL, FLOWER
	}
	
	protected EnumMap<PART,IIcon> iconMap = new EnumMap<PART, IIcon>(PART.class);
	
	public BlockConstructionFlower() {
		// It's not clear to me what depends on the argument to the
		// BlockFlower constructor. Hope this works.
		super(0);
		this.setStepSound(Block.soundTypeGrass);
		this.setCreativeTab(CreativeTabs.tabDecorations);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(int side, int meta) {
		return this.blockIcon;
	}
	
	@SideOnly(Side.CLIENT)
	public IIcon getWorldIcon(PART part) {
		return this.iconMap.get(part);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons (IIconRegister iconRegister)
	{
		this.blockIcon = iconRegister.registerIcon("constructionflower:constructionFlower");
		
		this.iconMap.put(PART.STEM, iconRegister.registerIcon("constructionflower:stem"));
		this.iconMap.put(PART.SEPAL, iconRegister.registerIcon("constructionflower:sepal"));
		this.iconMap.put(PART.FLOWER, iconRegister.registerIcon("constructionflower:flower"));

	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public int getRenderType() {
		return constructionflower.constructionFlowerRenderId;
	}
	
	@SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z)
    {
		return world.getBiomeGenForCoords(x, z).getBiomeGrassColor(x, y, z);
    }
	
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item id, CreativeTabs tab, List itemList)
    {
        itemList.add(new ItemStack(this, 1, 0));
    }

	
    public EnumPlantType getPlantType(IBlockAccess world, int x, int y, int z)
    {
        return EnumPlantType.Plains;
    }
    
    public Block getPlant(IBlockAccess world, int x, int y, int z)
    {
    	return this;
    }
    
    public int getPlantMetadata(IBlockAccess world, int x, int y, int z)
    {
        return world.getBlockMetadata(x, y, z);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z)
    {
        return (world.getFullBlockLightValue(x, y, z) >= 8 || world.canBlockSeeTheSky(x, y, z))
                && canThisPlantGrowOnThisBlock(world.getBlock(x, y - 1, z));
    }
    
    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z)
    {
        return super.canPlaceBlockAt(world, x, y, z) && canThisPlantGrowOnThisBlock(world.getBlock(x, y - 1, z));
    }
    
    private boolean canThisPlantGrowOnThisBlock(Block block)
    {
    	return block.equals(Blocks.grass)
    			|| block.equals(Blocks.dirt)
    			|| block.equals(Blocks.farmland)
    			|| block.equals(Blocks.sand);
    }
    
}
