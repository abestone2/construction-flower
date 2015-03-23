package com.abocalypse.constructionflower.plan;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.abocalypse.constructionflower.lib.ConfigHandler;
import com.abocalypse.constructionflower.lib.Constants;
import com.abocalypse.constructionflower.plan.ChunkBlocks.BlockMember;
import com.abocalypse.constructionflower.world.SpawnHandler;

public class WorldPlanRegistry extends WorldSavedData {
	
	private final static String PLAN_REGISTRY_NAME = Constants.MODDIR + "." + Constants.MODID + ".planregistry";

	private World worldObj;
	private final Map<String, Plan> plans = new HashMap<String, Plan>();
	private final MasterChunkBlocks masterChunkBlocks = new MasterChunkBlocks();
	private final Set<ChunkCoordIntPair> staleChunks = new HashSet<ChunkCoordIntPair>();
	private final PlansByChunk plansByChunk = new PlansByChunk();

	private boolean initialized;
	private boolean unstaling;
	
	private static class InitialPlan {
		public final int xAnchor;
		public final int zAnchor;
		public final AnchorMode anchorMode;
		public final PlanPartSpec.Orientation orientation;
		public final String planSpecFileName;
		
		public InitialPlan(int xAnchor, int zAnchor, AnchorMode anchorMode, PlanPartSpec.Orientation orientation, String planSpecFileName) {
			this.xAnchor = xAnchor;
			this.zAnchor = zAnchor;
			this.anchorMode = anchorMode;
			this.orientation = orientation;
			this.planSpecFileName = planSpecFileName;
		}
	}
	
	private static final HashMap<String, InitialPlan> initialPlans = new HashMap<String, InitialPlan>();
	
	public static void initialPlan(String levelName, int xAnchor, int zAnchor, AnchorMode anchorMode, PlanPartSpec.Orientation orientation, String planSpecFileName) {
		initialPlans.put(levelName, new InitialPlan(xAnchor, zAnchor, anchorMode, orientation, planSpecFileName));
	}

	public static WorldPlanRegistry get(World world) {
			if ( world.isRemote ) {
				throw new RuntimeException("Attempt to get plan registry on the clinet");
			}
			WorldPlanRegistry registry = (WorldPlanRegistry)world.loadItemData(WorldPlanRegistry.class, PLAN_REGISTRY_NAME);
			if (registry == null) {
				registry = new WorldPlanRegistry();
				registry.worldObj = world;
				registry.tryToInitialize();
				world.setItemData(PLAN_REGISTRY_NAME, registry);
			} else {
				if ( registry.worldObj == null ) {
					registry.worldObj = world;
				}
				if ( !registry.initialized ) {
					registry.tryToInitialize();
					if ( registry.initialized && registry.staleChunks.size() > 0 ) {
						registry.unstaling = true;
						for ( Iterator<ChunkCoordIntPair> iterator = registry.staleChunks.iterator(); iterator.hasNext(); ) {
							ChunkCoordIntPair chunk = iterator.next();
							SpawnHandler.spawnOntoChunk(world, chunk, world.rand);
							iterator.remove();
						}
						registry.unstaling = false;
					}
				}
			}
			return registry;
	}
	
	public WorldPlanRegistry() {
		this(PLAN_REGISTRY_NAME);
		initialized = false;
	}
	
	public WorldPlanRegistry(String mapName) {
		super(mapName);
	}
	
	public void loadPlanSpec(String planSpecFileName, int xAnchor, int zAnchor, AnchorMode anchorMode, PlanSpec.Orientation orientation, String planName) {
		if (anchorMode == AnchorMode.RELATIVE_TO_SPAWN) {
			ChunkCoordinates spawnPoint = this.worldObj.getSpawnPoint();
			xAnchor += spawnPoint.posX;
			zAnchor += spawnPoint.posZ;
		}
		Plan plan = new Plan(planSpecFileName, xAnchor, zAnchor, orientation);
		plan.load();
		this.addPlan(planName, plan);
		this.markDirty();
	}
	
	private void tryToInitialize() {
		String levelName = this.worldObj.getWorldInfo().getWorldName();
		if ( initialPlans.containsKey(levelName) ) {
			InitialPlan initialPlan = initialPlans.get(levelName);
			if ( initialPlan.anchorMode == AnchorMode.RELATIVE_TO_ORIGIN  || !this.worldObj.findingSpawnPoint ) {
				this.loadPlanSpec(initialPlan.planSpecFileName, initialPlan.xAnchor, initialPlan.zAnchor, initialPlan.anchorMode, initialPlan.orientation, "Initial Plan");
				initialized = true;
			} else {
				initialized = false;
			}
		} else {
			initialized = true;
		}
	}

	public boolean initialized() {
		return initialized;
	}
	
	public boolean unstaling() {
		return unstaling;
	}
	
	public void staleChunk(ChunkCoordIntPair chunk) {
		this.staleChunks.add(chunk);
	}
	
	public void unstaleChunk(ChunkCoordIntPair chunk) {
		this.staleChunks.remove(chunk);
	}
	
	public boolean isStale(ChunkCoordIntPair chunk) {
		return this.staleChunks.contains(chunk);
	}
	
	private static class PlanFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			return pathname.toString().endsWith(".json");
		}
		
	}
	
	public static List<String> getAvailablePlans() {
		ArrayList<String> ret = new ArrayList<String>();
		File[] planFiles = ConfigHandler.planDir.listFiles(new PlanFilter());
		for ( File planFile : planFiles ) {
			ret.add(planFile.getName());
		}
		return ret;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		if ( !tagCompound.hasKey(PLAN_REGISTRY_NAME) ) {
			tagCompound.setTag(PLAN_REGISTRY_NAME, new NBTTagCompound());
		} else {
			NBTTagCompound ourTagCompound = tagCompound.getCompoundTag(PLAN_REGISTRY_NAME);
			initialized = ourTagCompound.getBoolean("initialized");
			NBTTagList staleChunksList = ourTagCompound.getTagList("stale chunks", 10);
			for ( int i = 0; i < staleChunksList.tagCount(); ++ i) {
				NBTTagCompound chunkNBT = staleChunksList.getCompoundTagAt(i);
				this.staleChunks.add(new ChunkCoordIntPair(chunkNBT.getInteger("x position"), chunkNBT.getInteger("z position")));
			}
			NBTTagList plansList = tagCompound.getCompoundTag(PLAN_REGISTRY_NAME).getTagList("plans", 10);
			for ( int i = 0; i < plansList.tagCount(); ++i) {
				NBTTagCompound planNBT = plansList.getCompoundTagAt(i);
				String planName = planNBT.getString("plan name");
				Plan plan = new Plan(planNBT.getString("planfilename"), planNBT.getInteger("worldX"), planNBT.getInteger("worldZ"), PlanPartSpec.Orientation.valueOf(planNBT.getString("orientation")));
				plan.chunkPlanBlocks.readFromNBT(planNBT);
				this.addPlan(planName, plan);
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		NBTTagCompound ourTagCompound = new NBTTagCompound();
		ourTagCompound.setBoolean("initialized", initialized);
		NBTTagList staleChunksList = new NBTTagList();
		for ( ChunkCoordIntPair chunk : this.staleChunks ) {
			NBTTagCompound chunkNBT = new NBTTagCompound();
			chunkNBT.setInteger("x position", chunk.chunkXPos);
			chunkNBT.setInteger("z position", chunk.chunkZPos);
			staleChunksList.appendTag(chunkNBT);
		}
		ourTagCompound.setTag("stale chunks", staleChunksList);
		NBTTagList plansList = new NBTTagList();
		for ( String planName : plans.keySet() ) {
			NBTTagCompound planNBT = new NBTTagCompound();
			planNBT.setString("plan name", planName);
			plans.get(planName).writeToNBT(planNBT);
			plansList.appendTag(planNBT);
		}
		ourTagCompound.setTag("plans", plansList);
		tagCompound.setTag(PLAN_REGISTRY_NAME, ourTagCompound);
	}

	public static enum AnchorMode {
		RELATIVE_TO_ORIGIN, RELATIVE_TO_SPAWN
	}
	
	private static class Plan {
		private final String planFileName;
		private final int worldX;
		private final int worldZ;
		private final PlanPartSpec.Orientation orientation;
		private final ChunkPlanBlocks chunkPlanBlocks;
		
		public Plan(String planFileName, int worldX, int worldZ, PlanPartSpec.Orientation orientation) {
			this.planFileName = planFileName;
			this.worldX = worldX;
			this.worldZ = worldZ;
			this.orientation = orientation;
			this.chunkPlanBlocks = new ChunkPlanBlocks();
		}
		
		public void writeToNBT(NBTTagCompound tagCompound) {
			tagCompound.setString("planfilename", this.planFileName);
			tagCompound.setInteger("worldX", this.worldX);
			tagCompound.setInteger("worldZ", this.worldZ);
			tagCompound.setString("orientation", this.orientation.toString());
			this.chunkPlanBlocks.writeToNBT(tagCompound);
		}
		
		@SuppressWarnings("unchecked")
		public void load() {
			PlanSpec spec = null;
			try {
				spec = new PlanSpec((JSONObject)(new JSONParser().parse(new FileReader(new File(ConfigHandler.planDir, planFileName)))), this.orientation);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			spec.load(worldX, worldZ, chunkPlanBlocks);
		}
	}
	
	@SuppressWarnings("serial")
	private static class PlansByChunk extends HashMap<ChunkCoordIntPair,HashMap<Plan, EnumConstructionFlowerLevel>> {

		public void put(ChunkCoordIntPair key, Plan plan, EnumConstructionFlowerLevel level) {
			if ( !this.containsKey(key) ) {
				this.put(key, new HashMap<Plan, EnumConstructionFlowerLevel>());
			}
			// SPAWN trumps SPREAD, so the new level will be put in only if
			// (a) the new level is SPAWN or (b) no level is set so far
			if ( level == EnumConstructionFlowerLevel.SPAWN || !this.get(key).containsKey(plan) ) {
				this.get(key).put(plan, level);
			}
		}

	}

	
	private void addPlan(String planName, Plan plan) {
		plans.put(planName, plan);
		for ( ChunkCoordIntPair chunk : plan.chunkPlanBlocks.keySet(EnumConstructionFlowerLevel.SPREAD) ) {
				masterChunkBlocks.addChunkBlocks(chunk, plan.chunkPlanBlocks.get(chunk, EnumConstructionFlowerLevel.SPREAD), EnumConstructionFlowerLevel.SPREAD);
				plansByChunk.put(chunk, plan, EnumConstructionFlowerLevel.SPREAD);
		}
		for ( ChunkCoordIntPair chunk : plan.chunkPlanBlocks.keySet(EnumConstructionFlowerLevel.SPAWN) ) {
				masterChunkBlocks.addChunkBlocks(chunk, plan.chunkPlanBlocks.get(chunk, EnumConstructionFlowerLevel.SPAWN), EnumConstructionFlowerLevel.SPAWN);
				plansByChunk.put(chunk, plan, EnumConstructionFlowerLevel.SPAWN);
		}
		masterChunkBlocks.findNeighbors();
	}
	
	public void respawnOntoPlan(String planName) {

		for (ChunkCoordIntPair chunk : plans.get(planName).chunkPlanBlocks.spawnChunkBlockMap.keySet()) {
			IChunkProvider chunkProvider = this.worldObj.getChunkProvider();
			// if the chunk is currently loaded, spawn onto it right away
			if (chunkProvider.chunkExists(chunk.chunkXPos, chunk.chunkZPos)) {
				SpawnHandler.spawnOntoChunk(this.worldObj, chunk, this.worldObj.rand);
			} else {
				// if the chunk has already been generated, but is not currently loaded,
				// mark so that it will get spawned onto next time it's loaded
				IChunkLoader loader = ((ChunkProviderServer)(chunkProvider)).currentChunkLoader;
				// Apparently the only class that implements IChunkLoader is AnvilChunkLoader?
				// I'm not sure what could be done if that fails. If we do have an AnvilChunkLoader
				// then I *think* this will test whether the chunk has already been generated.
				// (If it has not been generated then there's no need to do anything, the
				// regular world gen thing will take care of it.)
				if ( loader instanceof AnvilChunkLoader && ((AnvilChunkLoader)loader).chunkExists(this.worldObj, chunk.chunkXPos, chunk.chunkZPos) ) {
					this.staleChunks.add(chunk);
				}
			}
		}

	}
	
	
	public boolean containsChunk(ChunkCoordIntPair chunk, EnumConstructionFlowerLevel level) {
		return masterChunkBlocks.containsChunk(chunk, level);
	}
	
	public boolean containsBlock(int x, int z, EnumConstructionFlowerLevel level) {
		return masterChunkBlocks.containsBlock(x, z, level);
	}
	
	public boolean containsBlock(int x, int z) {
		return masterChunkBlocks.containsBlock(x, z, EnumConstructionFlowerLevel.SPREAD) || masterChunkBlocks.containsBlock(x, z, EnumConstructionFlowerLevel.SPAWN);
	}
	
	public List<BlockMember> randomSpawnBlocksFromChunk(ChunkCoordIntPair chunk, int n, Random random) {
		return masterChunkBlocks.randomSpawnBlocksFromChunk(chunk, n, random);
	}
	
	public List<BlockMember> randomNeighborsOf(int x, int z, int n, Random rand) {
		return masterChunkBlocks.randomNeighborsOf(x, z, n, rand);
	}
	
	public boolean removeChunk(Plan plan, ChunkCoordIntPair chunk) {
		boolean plansRemain = false;
		if ( plansByChunk.containsKey(chunk) ) {

			Map<Plan, EnumConstructionFlowerLevel> chunkPlans = plansByChunk.get(chunk);

			if ( chunkPlans.containsKey(plan) ) {
				chunkPlans.remove(plan);
				masterChunkBlocks.removeChunk(chunk);
				if ( chunkPlans.size() == 0 ) {
					plansByChunk.remove(chunk);
				} else {
					for ( Plan remainingPlan : chunkPlans.keySet() ) {
						plansRemain = true;
						masterChunkBlocks.addChunkBlocks(chunk, remainingPlan.chunkPlanBlocks.get(chunk, EnumConstructionFlowerLevel.SPREAD), EnumConstructionFlowerLevel.SPREAD);
						masterChunkBlocks.addChunkBlocks(chunk, remainingPlan.chunkPlanBlocks.get(chunk, EnumConstructionFlowerLevel.SPAWN), EnumConstructionFlowerLevel.SPAWN);
					}
				}
			}
			
		}
		return plansRemain;
	}

}
