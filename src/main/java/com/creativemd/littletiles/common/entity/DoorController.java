package com.creativemd.littletiles.common.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import com.creativemd.creativecore.common.utils.mc.WorldUtils;
import com.creativemd.littletiles.common.action.block.LittleActionPlaceStack;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.tiles.place.PlacePreviewTile;
import com.creativemd.littletiles.common.tiles.preview.LittleAbsolutePreviewsStructure;
import com.creativemd.littletiles.common.tiles.vec.LittleTileVec;
import com.creativemd.littletiles.common.utils.animation.AnimationController;
import com.creativemd.littletiles.common.utils.animation.AnimationState;
import com.creativemd.littletiles.common.utils.animation.AnimationTimeline;
import com.creativemd.littletiles.common.utils.placing.PlacementMode;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class DoorController extends EntityAnimationController {
	
	protected boolean isWaitingForApprove = false;
	protected int ticksToWait = -1;
	protected static final int waitTimeApprove = 300;
	protected static final int waitTimeRender = 200;
	protected Boolean placed = null;
	
	public static final String openedState = "opened";
	public static final String closedState = "closed";
	public Boolean turnBack;
	public int duration;
	public EntityPlayer activator;
	
	protected boolean modifiedTransition;
	
	@SideOnly(Side.CLIENT)
	List<TileEntityLittleTiles> waitingForRender;
	
	public DoorController() {
		
	}
	
	public DoorController(AnimationState closed, AnimationState opened, Boolean turnBack, int duration) {
		this.turnBack = turnBack;
		this.duration = duration;
		
		addState(openedState, opened);
		addStateAndSelect(closedState, closed);
		
		generateAllTransistions(duration);
		modifiedTransition = false;
		
		startTransition(openedState);
	}
	
	public DoorController(AnimationState closed, AnimationState opened, Boolean turnBack, int duration, AnimationTimeline open, AnimationTimeline close) {
		this.turnBack = turnBack;
		this.duration = duration;
		
		addState(openedState, opened);
		addStateAndSelect(closedState, closed);
		
		addTransition("closed", "opened", open);
		addTransition("opened", "closed", close);
		
		startTransition(openedState);
	}
	
	@Override
	public AnimationController addTransition(String from, String to, AnimationTimeline animation) {
		modifiedTransition = true;
		return super.addTransition(from, to, animation);
	}
	
	public DoorController(AnimationState opened, Boolean turnBack, int duration) {
		this(new AnimationState(), opened, turnBack, duration);
	}
	
	@Override
	public EntityPlayer activator() {
		return activator;
	}
	
	public void markWaitingForApprove() {
		isWaitingForApprove = true;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void removeWaitingTe(TileEntityLittleTiles te) {
		waitingForRender.remove(te);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean isWaitingForRender() {
		return waitingForRender != null;
	}
	
	@Override
	public boolean onRightClick() {
		if (placed != null)
			return false;
		
		boolean isOpen = currentState.name.equals(openedState);
		if (!isChanging()) {
			startTransition(isOpen ? closedState : openedState);
			return true;
		}
		return false;
	}
	
	@Override
	public AnimationState tick() {
		if (parent.world.isRemote && placed != null) {
			if (placed) {
				ticksToWait--;
				
				if (ticksToWait % 10 == 0) {
					List<TileEntityLittleTiles> tileEntities = null;
					for (Iterator iterator = waitingForRender.iterator(); iterator.hasNext();) {
						TileEntityLittleTiles te = (TileEntityLittleTiles) iterator.next();
						if (te != te.getWorld().getTileEntity(te.getPos())) {
							if (tileEntities == null)
								tileEntities = new ArrayList<>();
							tileEntities.add(te);
						}
					}
					if (tileEntities != null)
						waitingForRender.removeAll(tileEntities);
				}
				
				if (waitingForRender.size() == 0 || ticksToWait < 0) {
					parent.unloadRenderCache();
					parent.isDead = true;
				} else
					parent.isDead = false;
				
			} else {
				if (isWaitingForApprove) {
					if (ticksToWait < 0)
						ticksToWait = waitTimeApprove;
					else if (ticksToWait == 0)
						parent.isDead = true;
					else
						ticksToWait--;
				} else
					place();
			}
		}
		return super.tick();
	}
	
	@Override
	public void endTransition() {
		super.endTransition();
		if (turnBack != null && turnBack == currentState.name.equals(openedState)) {
			if (isWaitingForApprove)
				placed = false;
			else
				place();
		}
	}
	
	public void place() {
		LittleAbsolutePreviewsStructure previews = parent.getAbsolutePreviews();
		
		List<PlacePreviewTile> placePreviews = new ArrayList<>();
		previews.getPlacePreviews(placePreviews, null, true, LittleTileVec.ZERO);
		
		LittleStructure newDoor = previews.getStructure();
		World world = parent.world;
		
		if (LittleActionPlaceStack.placeTilesWithoutPlayer(world, previews.context, placePreviews, previews.getStructure(), PlacementMode.all, previews.pos, null, null, null, EnumFacing.EAST) != null) {
			if (parent.structure.parent != null && parent.structure.parent.isConnected(world)) {
				LittleStructure parentStructure = parent.structure.parent.getStructureWithoutLoading();
				newDoor.updateParentConnection(parent.structure.parent.getChildID(), parentStructure);
				parentStructure.updateChildConnection(parent.structure.parent.getChildID(), newDoor);
			}
		} else {
			parent.isDead = true;
			if (!world.isRemote)
				WorldUtils.dropItem(world, parent.structure.getStructureDrop(), parent.center.baseOffset);
			return;
		}
		
		if (!world.isRemote)
			parent.isDead = true;
		else {
			waitingForRender = new CopyOnWriteArrayList<>();
			ArrayList<BlockPos> coordsToCheck = new ArrayList<>(LittleActionPlaceStack.getSplittedTiles(previews.context, placePreviews, previews.pos).keySet());
			for (int i = 0; i < coordsToCheck.size(); i++) {
				TileEntity te = world.getTileEntity(coordsToCheck.get(i));
				if (te instanceof TileEntityLittleTiles) {
					((TileEntityLittleTiles) te).addWaitingAnimation(parent);
					waitingForRender.add((TileEntityLittleTiles) te);
				}
			}
			ticksToWait = waitTimeRender;
			parent.isDead = false;
			placed = true;
		}
	}
	
	@Override
	protected void writeToNBTExtra(NBTTagCompound nbt) {
		nbt.setTag("closed", getState(closedState).state.writeToNBT(new NBTTagCompound()));
		nbt.setTag("opened", getState(openedState).state.writeToNBT(new NBTTagCompound()));
		
		nbt.setBoolean("isOpen", currentState.name.equals(openedState));
		if (isChanging())
			nbt.setInteger("tick", this.tick);
		
		nbt.setInteger("duration", duration);
		nbt.setByte("turnBack", (byte) (turnBack == null ? 0 : (turnBack ? 1 : -1)));
		
		if (modifiedTransition) {
			NBTTagList list = new NBTTagList();
			for (Entry<String, AnimationTimeline> entry : stateTransition.entrySet()) {
				NBTTagCompound transitionNBT = entry.getValue().writeToNBT(new NBTTagCompound());
				transitionNBT.setString("key", entry.getKey());
				list.appendTag(transitionNBT);
			}
			nbt.setTag("transitions", list);
		}
	}
	
	@Override
	protected void readFromNBT(NBTTagCompound nbt) {
		addState(closedState, new AnimationState(nbt.getCompoundTag("closed")));
		addState(openedState, new AnimationState(nbt.getCompoundTag("opened")));
		
		duration = nbt.getInteger("duration");
		if (nbt.hasKey("transitions")) {
			NBTTagList list = nbt.getTagList("transitions", 10);
			for (int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound transitionNBT = list.getCompoundTagAt(i);
				addTransition(transitionNBT.getString("key"), new AnimationTimeline(transitionNBT));
			}
			modifiedTransition = true;
		} else
			generateAllTransistions(duration);
		
		boolean isOpen = nbt.getBoolean("isOpen");
		if (!isOpen)
			currentState = getState(closedState);
		else
			currentState = getState(closedState);
		
		if (nbt.hasKey("tick")) {
			startTransition(isOpen ? closedState : openedState);
			this.tick = nbt.getInteger("tick");
		}
		
		byte turnBackData = nbt.getByte("turnBack");
		turnBack = turnBackData == 0 ? null : (turnBackData > 0 ? true : false);
	}
	
	@Override
	public void onServerApproves() {
		isWaitingForApprove = false;
	}
	
}
