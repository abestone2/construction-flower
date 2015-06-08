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
import java.util.Map.Entry;
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

import com.abocalypse.constructionflower.lib.EnumAnchorMode;
import com.abocalypse.constructionflower.lib.ConfigHandler;
import com.abocalypse.constructionflower.lib.Constants;
import com.abocalypse.constructionflower.lib.EnumOrientation;
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
		public final PlanPosition position;
		public final String planSpecFileName;
		
		public InitialPlan(PlanPosition position, String planSpecFileName) {
			this.position = new PlanPosition(position);
			this.planSpecFileName = planSpecFileName;
		}
	}
	
	private static final HashMap<String, InitialPlan> initialPlans = new HashMap<String, InitialPlan>();
	
	public static void initialPlan(String levelName, PlanPosition position, String planSpecFileName) {
		initialPlans.put(levelName, new InitialPlan(position, planSpecFileName));
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
	
	public void loadPlanSpec(String planSpecFileName, PlanPosition position, String planName) {
		Plan plan = new Plan(planSpecFileName, position);
		plan.load();
		this.addPlan(planName, plan);
		this.masterChunkBlocks.findNeighbors();
		this.markDirty();
	}
	
	private void tryToInitialize() {
		String levelName = this.worldObj.getWorldInfo().getWorldName();
		if ( initialPlans.containsKey(levelName) ) {
			InitialPlan initialPlan = initialPlans.get(levelName);
			if ( initialPlan.position.anchorMode == EnumAnchorMode.RELATIVE_TO_SPAWN && this.worldObj.findingSpawnPoint ) {
				initialized = false;
			} else {
				if (initialPlan.position.anchorMode == EnumAnchorMode.RELATIVE_TO_SPAWN ) {
					ChunkCoordinates spawnPoint = this.worldObj.getSpawnPoint();
					initialPlan.position.anchorRelativeToCoords = new BlockXZCoords(spawnPoint.posX, spawnPoint.posZ);
					initialPlan.position.anchor.add(initialPlan.position.anchorRelativeToCoords);
				}
				this.loadPlanSpec(initialPlan.planSpecFileName, initialPlan.position, "Initial Plan");
				initialized = true;
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
	
	public static List<String> getAvailablePlanSpecFiles() {
		ArrayList<String> ret = new ArrayList<String>();
		File[] planFiles = ConfigHandler.planDir.listFiles(new PlanFilter());
		if ( planFiles != null ) {
			for (File planFile : planFiles) {
				ret.add(planFile.getName());
			}
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
				PlanPosition position = new PlanPosition();
				position.readFromNBT(planNBT.getCompoundTag("position"));
				Plan plan = new Plan(planNBT.getString("planfilename"), position);
				plan.chunkPlanBlocks.readFromNBT(planNBT);
				this.addPlan(planName, plan);
			}
		}
		masterChunkBlocks.findNeighbors();
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

	public static class PlanPosition {

		public BlockXZCoords anchor;
		public EnumAnchorMode anchorMode;
		public BlockXZCoords anchorRelativeToCoords;
		public String anchorRelativeToPlan;
		public EnumOrientation orientation;
		
		public PlanPosition() {}
		
		public PlanPosition(BlockXZCoords anchor, EnumOrientation orientation) {
			this.anchor = anchor;
			this.anchorMode = EnumAnchorMode.RELATIVE_TO_ORIGIN;
			this.anchorRelativeToCoords = new BlockXZCoords(0,0);
			this.anchorRelativeToPlan = null;
			this.orientation = orientation;
		}

		public PlanPosition(BlockXZCoords anchor, EnumAnchorMode anchorMode, BlockXZCoords anchorRelativeToCoords, String anchorRelativeToPlan, EnumOrientation orientation) {
			this.anchor = anchor;
			this.anchorMode = anchorMode;
			this.anchorRelativeToCoords = anchorRelativeToCoords;
			this.anchorRelativeToPlan = anchorRelativeToPlan;
			this.orientation = orientation;
		}

		public PlanPosition(PlanPosition position) {
			this.anchor = new BlockXZCoords(position.anchor);
			this.anchorMode = position.anchorMode;
			this.anchorRelativeToCoords = new BlockXZCoords(position.anchorRelativeToCoords);
			this.anchorRelativeToPlan = position.anchorRelativeToPlan;
			this.orientation = position.orientation;
		}
		
		public void setTo(PlanPosition other) {
			this.anchor = other.anchor;
			this.anchorMode = other.anchorMode;
			this.anchorRelativeToCoords = other.anchorRelativeToCoords;
			this.anchorRelativeToPlan = other.anchorRelativeToPlan;
			this.orientation = other.orientation;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((anchorMode == null) ? 0 : anchorMode.hashCode());
			result = prime
					* result
					+ ((anchorRelativeToPlan == null) ? 0
							: anchorRelativeToPlan.hashCode());
			result = prime * result + anchorRelativeToCoords.x;
			result = prime * result + anchorRelativeToCoords.z;
			result = prime * result
					+ ((orientation == null) ? 0 : orientation.hashCode());
			result = prime * result + anchor.x;
			result = prime * result + anchor.z;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PlanPosition other = (PlanPosition) obj;
			if (anchorMode != other.anchorMode)
				return false;
			if (anchorRelativeToPlan == null) {
				if (other.anchorRelativeToPlan != null)
				return false;
			} else if (!anchorRelativeToPlan.equals(other.anchorRelativeToPlan))
				return false;
			if (orientation != other.orientation)
				return false;
			if ( !anchor.equals(other.anchor) )
				return false;
			if (anchor.z != other.anchor.z)
				return false;
			else
				return true;
		}
		
		public void writeToNBT(NBTTagCompound tagCompound) {
			tagCompound.setTag("anchor", this.anchor.tagCompound());
			tagCompound.setString("anchor mode", this.anchorMode.toString());
			tagCompound.setTag("anchor relative to", this.anchorRelativeToCoords.tagCompound());
			if ( this.anchorMode == EnumAnchorMode.RELATIVE_TO_PLAN ) {
				tagCompound.setString("anchor relative to plan", this.anchorRelativeToPlan);
			}
			tagCompound.setString("orientation", this.orientation.toString());
		}
		
		public void readFromNBT(NBTTagCompound tagCompound) {
			this.anchor = new BlockXZCoords(tagCompound.getCompoundTag("anchor"));
			this.anchorMode = EnumAnchorMode.valueOf(tagCompound.getString("anchor mode"));
			this.anchorRelativeToCoords = new BlockXZCoords(tagCompound.getCompoundTag("anchor relative to"));
			if ( this.anchorMode == EnumAnchorMode.RELATIVE_TO_PLAN ) {
				this.anchorRelativeToPlan = tagCompound.getString("anchor relative to plan");
			}
			this.orientation = EnumOrientation.valueOf(tagCompound.getString("orientation"));
		}

	}
	
	private static class Plan {
		private final String planFileName;
		private final PlanPosition position;
		private final ChunkPlanBlocks chunkPlanBlocks;
		
		public Plan(String planFileName, PlanPosition position) {
			this.planFileName = planFileName;
			this.position = new PlanPosition(position);
			this.chunkPlanBlocks = new ChunkPlanBlocks();
		}
		
		private Plan(Plan other) {
			this.planFileName = other.planFileName;
			this.position = new PlanPosition(other.position);
			this.chunkPlanBlocks = new ChunkPlanBlocks(other.chunkPlanBlocks);
		}
		
		public Plan moveTo(PlanPosition position) {
			Plan newPlan = new Plan(this);
			newPlan.chunkPlanBlocks.move(this.position(), position);
			newPlan.position.setTo(position);
			return newPlan;
		}
		
		public PlanPosition position() {
			return new PlanPosition(this.position);
		}
						
		public void writeToNBT(NBTTagCompound tagCompound) {
			tagCompound.setString("planfilename", this.planFileName);
			NBTTagCompound positionTag = new NBTTagCompound();
			this.position.writeToNBT(positionTag);
			tagCompound.setTag("position", positionTag);
			this.chunkPlanBlocks.writeToNBT(tagCompound);
		}
		
		@SuppressWarnings("unchecked")
		public void load() {
			PlanSpec spec = null;
			try {
				spec = new PlanSpec((JSONObject)(new JSONParser().parse(new FileReader(new File(ConfigHandler.planDir, planFileName)))), this.position.orientation);
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
			spec.load(position.anchor.x, position.anchor.z, EnumOrientation.TOPNORTH, chunkPlanBlocks);
		}
		
		public boolean containsChunk(ChunkCoordIntPair chunk, EnumConstructionFlowerLevel level) {
			return this.chunkPlanBlocks.containsChunk(chunk, level);
		}
		public boolean containsBlock(int x, int z, EnumConstructionFlowerLevel level) {
			return this.chunkPlanBlocks.containsBlock(x, z, level);
		}
	
		public boolean containsBlock(int x, int z) {
			// currently every SPAWN block is also a SPREAD block
			return this.containsBlock(x, z, EnumConstructionFlowerLevel.SPREAD);
		}
		
	}

	public static class PlanInfo {
		public String originalName;
		public PlanPosition position;
		public PlanStatus status;
		
		public void writeToNBT(NBTTagCompound tagCompound) {
			tagCompound.setString("original name", originalName);
			tagCompound.setString("status", status.name());
			NBTTagCompound positionTag = new NBTTagCompound();
			position.writeToNBT(positionTag);
			tagCompound.setTag("position", positionTag);
		}
		
		public void readFromNBT(NBTTagCompound tagCompound) {
			originalName = tagCompound.getString("original name");
			status = PlanStatus.valueOf(tagCompound.getString("status"));
			position = new PlanPosition();
			position.readFromNBT((NBTTagCompound) tagCompound.getTag("position"));
		}
	}

	public enum PlanStatus {
		OLD, REMOVE, REFRESH
	}


	public Map<String, PlanInfo> loadedPlans() {
		Map<String, PlanInfo> ret = new HashMap<String, PlanInfo>();
		for ( String name : plans.keySet() ) {
			PlanInfo planInfo = new PlanInfo();
			planInfo.position = new PlanPosition(plans.get(name).position());
			planInfo.status = PlanStatus.OLD;
			planInfo.originalName = name;
			ret.put(name, planInfo);
		}
		return ret;
	}
	
	// return value: true unless some plans we wanted to change are no longer
	// there (or have changed names since we checked them out), meaning that some
	// other player has been messing with this?
	// TODO should take steps to make sure that doesn't happen?
	public boolean updatePlans(Map<String, PlanInfo> updatedPlans, List<ChunkCoordinates> spawnedOnto) {
		Set<String> originalNames = new HashSet<String>();
		boolean ret = true;
		boolean changes = false;
		boolean blocksChanged = false;
 		for ( Map.Entry<String, PlanInfo> entry : updatedPlans.entrySet() ) {
			originalNames.add(entry.getValue().originalName);
		}
		if ( originalNames.size() != plans.size() ) {
			Iterator<Entry<String, Plan>> it = plans.entrySet().iterator();
			while ( it.hasNext() ) {
				Entry<String, Plan> entry = it.next();
				if ( !originalNames.contains(entry.getKey()) ) {
					changes = true;
					blocksChanged = true;
					removePlanChunks(entry.getKey());
					it.remove();
				}
			}
		}
		for ( Map.Entry<String, PlanInfo> entry : updatedPlans.entrySet() ) {
			String name = entry.getKey();
			PlanInfo info = entry.getValue();
			if ( !plans.containsKey(info.originalName) ) {
				ret = false;
			} else {
					Plan plan = plans.get(info.originalName);
					boolean readd = false;
					Plan newPlan = plan;
					if ( info.status == PlanStatus.REFRESH ) {
						readd = true;
						changes = true;
						blocksChanged = true;
						plan.load();
					} else if ( !plan.position().equals(info.position) ) {
						readd = true;
						changes = true;
						blocksChanged = true;
						newPlan = plan.moveTo(info.position);
					}

					if ( readd ) {
						removePlanChunks(info.originalName);
						plans.remove(info.originalName);
						addPlan(name, newPlan);
						respawnOntoPlan(name, spawnedOnto);
					} else if ( name != info.originalName ) {
						changes = true;
						plans.remove(info.originalName);
						plans.put(name, plan);
					}

				}
			}
		if ( changes ) {
			// only do this once all the dust has settled
			if ( blocksChanged ) {
				this.masterChunkBlocks.findNeighbors();
			}
			this.markDirty();
		}

		return ret;
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
	}
	
	private void removePlanChunks(String planName) {
		Plan plan = plans.get(planName);
		// SPREAD includes both SPAWN and SPREAD level blocks
		for ( ChunkCoordIntPair chunk : plan.chunkPlanBlocks.keySet(EnumConstructionFlowerLevel.SPREAD)) {
			removeChunk(plan, chunk);
		}
	}
	
	public void respawnOntoPlan(String planName, List<ChunkCoordinates> spawnedOnto) {
		for (ChunkCoordIntPair chunk : plans.get(planName).chunkPlanBlocks.spawnChunkBlockMap.keySet()) {
			IChunkProvider chunkProvider = this.worldObj.getChunkProvider();
			// if the chunk is currently loaded, spawn onto it right away
			if (chunkProvider.chunkExists(chunk.chunkXPos, chunk.chunkZPos)) {
				SpawnHandler.spawnOntoPlanChunk(this.worldObj, chunk, this.worldObj.rand, planName, spawnedOnto);
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
		// currently every SPAWN block is also a SPREAD block
		return this.containsBlock(x, z, EnumConstructionFlowerLevel.SPREAD);
	}

	public boolean containsChunk(String planName, ChunkCoordIntPair chunk, EnumConstructionFlowerLevel level) {
		if ( planName == null ) {
			return this.containsChunk(chunk, level);
		}
		return plans.get(planName).containsChunk(chunk, level);
	}
	
	public boolean containsBlock(String planName, int x, int z, EnumConstructionFlowerLevel level) {
		if ( planName == null ) {
			return this.containsBlock(x, z, level);
		}
		return plans.get(planName).containsBlock(x, z, level);
	}
	
	public boolean containsBlock(String planName, int x, int z) {
		if ( planName == null ) {
			return this.containsBlock(x, z);
		}
		return plans.get(planName).containsBlock(x, z);
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
