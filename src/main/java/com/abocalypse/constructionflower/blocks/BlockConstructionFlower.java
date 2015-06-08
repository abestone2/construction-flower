package com.abocalypse.constructionflower.blocks;

import java.util.EnumMap;
import java.util.List;
import java.util.Random;

import com.abocalypse.constructionflower.ConstructionFlower;
import com.abocalypse.constructionflower.plan.WorldPlanRegistry;
import com.abocalypse.constructionflower.world.SpawnHandler;

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

	public static enum PlantPart {
		STEM, SEPAL, FLOWER
	}
	
	protected EnumMap<PlantPart,IIcon> iconMap = new EnumMap<PlantPart, IIcon>(PlantPart.class);
	
	public BlockConstructionFlower() {
		// It's not clear to me what depends on the argument to the
		// the super (i.e., BlockFlower) constructor. Hope this works.
		super(0);
		this.setStepSound(Block.soundTypeGrass);
		this.setCreativeTab(CreativeTabs.tabDecorations);
		this.setTickRandomly(true);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(int side, int meta) {
		return this.blockIcon;
	}
	
	@SideOnly(Side.CLIENT)
	public IIcon getWorldIcon(PlantPart part) {
		return this.iconMap.get(part);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons (IIconRegister iconRegister)
	{
		this.blockIcon = iconRegister.registerIcon("constructionflower:constructionFlower");
		
		this.iconMap.put(PlantPart.STEM, iconRegister.registerIcon("constructionflower:stem"));
		this.iconMap.put(PlantPart.SEPAL, iconRegister.registerIcon("constructionflower:sepal"));
		this.iconMap.put(PlantPart.FLOWER, iconRegister.registerIcon("constructionflower:flower"));

	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public int getRenderType() {
		return ConstructionFlower.constructionFlowerRenderId;
	}
	
	// The way grass and foliage works is that there's a grayscale
	// texture, which then gets multiplied by a different color value
	// depending on the biome and height. Currently the construction
	// flower leaves and sepals are the color of grass (but this probably
	// should be changed, they can be hard to see against the background).
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

    // This list maybe should be made longer? Although there are
    // usually enough of these blocks around (and I don't like the
    // idea that the flower could "grow" on stone, gravel, etc.).
    private boolean canThisPlantGrowOnThisBlock(Block block)
    {
    	return (block.equals(Blocks.grass)
    			|| block.equals(Blocks.dirt)
    			|| block.equals(Blocks.farmland)
    			|| block.equals(Blocks.sand));
    }
    
    // This is the only part that's really special to the mod.
    // On random ticks, check to see if there's still a plan under this
    // flower; if not, it disappears (currently without dropping
    // anything). Or, on the other hand, if there is still a plan
    // under it, try to spread to neighboring blocks.
    public void updateTick(World world, int x, int y, int z, Random random) {
    	if ( !world.isRemote ) {
    		WorldPlanRegistry registry = WorldPlanRegistry.get(world);
    		if ( !registry.containsBlock(x, z) ) {
    			world.setBlock(x, y, z, Blocks.air);
    		} else {
    			SpawnHandler.spreadFromBlock(world, x, y, z, random);
    		}
         }
    }

    
}
