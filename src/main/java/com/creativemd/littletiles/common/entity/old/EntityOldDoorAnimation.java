package com.creativemd.littletiles.common.entity.old;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.creativemd.creativecore.common.utils.mc.WorldUtils;
import com.creativemd.creativecore.common.world.CreativeWorld;
import com.creativemd.littletiles.common.action.block.LittleActionPlaceStack;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.connection.StructureLink;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.tiles.place.PlacePreviewTile;
import com.creativemd.littletiles.common.tiles.preview.LittleAbsolutePreviewsStructure;
import com.creativemd.littletiles.common.tiles.vec.LittleTilePos;
import com.creativemd.littletiles.common.tiles.vec.LittleTileVec;
import com.creativemd.littletiles.common.utils.outdated.OldDoorTransformation;
import com.creativemd.littletiles.common.utils.placing.PlacementMode;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

@Deprecated
public class EntityOldDoorAnimation extends EntityOldAnimation {
	
	// private static final DataParameter<Float> ENTITY_PROGRESS =
	// EntityDataManager.<Double>createKey(EntityDoorAnimation.class,
	// DataSerializers.FLOAT);
	
	public EntityPlayer activator;
	
	public long started = System.currentTimeMillis();
	private double progress;
	public int duration;
	public boolean approved = true;
	
	public OldDoorTransformation transformation;
	
	public void setTransformationStartOffset() {
		preventPush = true;
		transformation.performTransformation(this, 0);
		prevWorldRotX = worldRotX;
		prevWorldRotY = worldRotY;
		prevWorldRotZ = worldRotZ;
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		preventPush = false;
	}
	
	public EntityOldDoorAnimation(World worldIn) {
		super(worldIn);
	}
	
	public EntityOldDoorAnimation(World world, CreativeWorld worldFake, ArrayList<TileEntityLittleTiles> blocks, LittleAbsolutePreviewsStructure previews, LittleTilePos axis, OldDoorTransformation transformation, UUID uuid, EntityPlayer activator, LittleTileVec additionalAxis, int duration) {
		super(world, worldFake, blocks, previews, uuid, axis, additionalAxis);
		
		this.activator = activator;
		
		this.transformation = transformation;
		this.duration = duration;
		
		setTransformationStartOffset();
		
		prevPosY -= 0.1; // To force an update
		updateBoundingBox();
		updateOrigin();
		
		if (world.isRemote)
			approved = false;
	}
	
	@Override
	public void copyExtra(EntityOldAnimation animation) {
		EntityOldDoorAnimation doorAnimation = (EntityOldDoorAnimation) animation;
		doorAnimation.progress = progress;
		doorAnimation.duration = duration;
		doorAnimation.approved = approved;
		doorAnimation.transformation = transformation;
		doorAnimation.lastSendProgress = lastSendProgress;
		animation.additionalAxis = additionalAxis.copy();
	}
	
	private float lastSendProgress = -1;
	
	public void setProgress(double progress) {
		if (progress > duration)
			this.progress = duration;
		else
			this.progress = progress;
		
		/* if(!world.isRemote && (lastSendProgress == -1 || progress - lastSendProgress
		 * > 10 || progress >= duration)) { dataManager.set(ENTITY_PROGRESS, progress);
		 * lastSendProgress = progress; } */
	}
	
	public double getProgress() {
		return progress;
	}
	
	@Override
	protected void entityInit() {
		// this.dataManager.register(ENTITY_PROGRESS, 0F);
	}
	
	@Override
	public void notifyDataManagerChange(DataParameter<?> key) {
		// if (world.isRemote && ENTITY_PROGRESS.equals(key) && !isWaitingForRender())
		// this.progress = this.dataManager.get(ENTITY_PROGRESS).intValue();
	}
	
	@Override
	public void onTick() {
		if (transformation != null)
			transformation.performTransformation(this, progress / (double) duration);
	}
	
	@Override
	public void onPostTick() {
		if (transformation == null)
			return;
		
		if (progress >= duration) {
			// Try to place door, if not drop ItemStack
			LittleStructure placedStructureParent = getParentStructure();
			reloadPreviews(placedStructureParent, previews.pos);
			
			if (!world.isRemote || approved) {
				List<PlacePreviewTile> placePreviews = new ArrayList<>();
				previews.getPlacePreviews(placePreviews, null, true, LittleTileVec.ZERO);
				
				LittleStructure newDoor = previews.getStructure();
				
				if (LittleActionPlaceStack.placeTilesWithoutPlayer(world, previews.context, placePreviews, previews.getStructure(), PlacementMode.all, previews.pos, null, null, null, EnumFacing.EAST) != null) {
					if (placedStructureParent.parent != null && placedStructureParent.parent.isConnected(world)) {
						LittleStructure parentStructure = placedStructureParent.parent.getStructureWithoutLoading();
						newDoor.parent = new StructureLink(newDoor.getMainTile().te, parentStructure.getMainTile().te.getPos(), parentStructure.getMainTile().getContext(), parentStructure.getMainTile().getIdentifier(), parentStructure.attribute, newDoor, placedStructureParent.parent.getChildID(), true);
						parentStructure.children.put(newDoor.parent.getChildID(), new StructureLink(parentStructure.getMainTile().te, newDoor.getMainTile().te.getPos(), newDoor.getMainTile().getContext(), newDoor.getMainTile().getIdentifier(), newDoor.attribute, parentStructure, newDoor.parent.getChildID(), false));
					}
				} else if (!world.isRemote)
					WorldUtils.dropItem(world, previews.getStructure().getStructureDrop(), baseOffset);
			}
			
			isDead = true;
		} else {
			setProgress(((System.currentTimeMillis() - started) / 50D));
		}
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);
		duration = compound.getInteger("duration");
		NBTBase tag = compound.getTag("progress");
		if (tag instanceof NBTTagInt)
			setProgress(((NBTTagInt) tag).getInt());
		else if (tag instanceof NBTTagFloat)
			setProgress(((NBTTagFloat) tag).getFloat());
		else
			setProgress(((NBTTagDouble) tag).getDouble());
		
		transformation = OldDoorTransformation.loadFromNBT(compound.getCompoundTag("transform"));
		setTransformationStartOffset();
		
		started = System.currentTimeMillis() - (long) (progress * 50);
		
	}
	
	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);
		
		compound.setInteger("duration", duration);
		compound.setDouble("progress", progress);
		
		compound.setTag("transform", transformation.writeToNBT(new NBTTagCompound()));
	}
	
}
