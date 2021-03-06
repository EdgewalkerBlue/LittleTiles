package com.creativemd.littletiles.common.structure.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Nullable;

import com.creativemd.creativecore.common.gui.CoreControl;
import com.creativemd.creativecore.common.gui.container.GuiParent;
import com.creativemd.creativecore.common.gui.controls.gui.GuiCheckBox;
import com.creativemd.creativecore.common.gui.controls.gui.GuiLabel;
import com.creativemd.creativecore.common.gui.controls.gui.GuiSteppedSlider;
import com.creativemd.creativecore.common.gui.event.gui.GuiControlChangedEvent;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.type.HashMapList;
import com.creativemd.creativecore.common.utils.type.PairList;
import com.creativemd.creativecore.common.utils.type.UUIDSupplier;
import com.creativemd.creativecore.common.world.SubWorld;
import com.creativemd.littletiles.common.action.block.LittleActionActivated;
import com.creativemd.littletiles.common.action.block.LittleActionPlaceStack;
import com.creativemd.littletiles.common.entity.DoorController;
import com.creativemd.littletiles.common.entity.EntityAnimation;
import com.creativemd.littletiles.common.packet.LittleDoorPacket;
import com.creativemd.littletiles.common.structure.LittleStructure;
import com.creativemd.littletiles.common.structure.attribute.LittleStructureAttribute;
import com.creativemd.littletiles.common.structure.registry.LittleStructureGuiParser;
import com.creativemd.littletiles.common.structure.registry.LittleStructureRegistry;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType;
import com.creativemd.littletiles.common.structure.relative.StructureAbsolute;
import com.creativemd.littletiles.common.structure.type.LittleAdvancedDoor.LittleAdvancedDoorParser;
import com.creativemd.littletiles.common.structure.type.LittleAxisDoor.LittleAxisDoorParser;
import com.creativemd.littletiles.common.structure.type.LittleDoorActivator.LittleDoorActivatorParser;
import com.creativemd.littletiles.common.structure.type.LittleSlidingDoor.LittleSlidingDoorParser;
import com.creativemd.littletiles.common.tileentity.TileEntityLittleTiles;
import com.creativemd.littletiles.common.tiles.LittleTile;
import com.creativemd.littletiles.common.tiles.place.PlacePreviewTile;
import com.creativemd.littletiles.common.tiles.place.PlacePreviews;
import com.creativemd.littletiles.common.tiles.preview.LittleAbsolutePreviewsStructure;
import com.creativemd.littletiles.common.tiles.preview.LittlePreviews;
import com.creativemd.littletiles.common.tiles.vec.LittleTileVec;
import com.creativemd.littletiles.common.utils.animation.AnimationGuiHandler;
import com.creativemd.littletiles.common.utils.animation.AnimationTimeline;
import com.creativemd.littletiles.common.utils.grid.LittleGridContext;
import com.creativemd.littletiles.common.utils.placing.PlacementMode;
import com.n247s.api.eventapi.eventsystem.CustomEventSubscribe;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class LittleDoorBase extends LittleStructure implements ILittleDoor {
	
	public LittleDoorBase(LittleStructureType type) {
		super(type);
	}
	
	public int duration = 50;
	public boolean stayAnimated = false;
	
	@Override
	protected void loadFromNBTExtra(NBTTagCompound nbt) {
		if (nbt.hasKey("duration"))
			duration = nbt.getInteger("duration");
		else
			duration = 50;
		stayAnimated = nbt.getBoolean("stayAnimated");
	}
	
	@Override
	protected void writeToNBTExtra(NBTTagCompound nbt) {
		nbt.setInteger("duration", duration);
		if (stayAnimated)
			nbt.setBoolean("stayAnimated", stayAnimated);
	}
	
	public boolean place(World world, EntityPlayer player, LittleAbsolutePreviewsStructure previews, DoorController controller, UUID uuid, StructureAbsolute absolute) {
		List<PlacePreviewTile> placePreviews = new ArrayList<>();
		previews.getPlacePreviews(placePreviews, null, true, LittleTileVec.ZERO);
		
		HashMap<BlockPos, PlacePreviews> splitted = LittleActionPlaceStack.getSplittedTiles(previews.context, placePreviews, previews.pos);
		if (LittleActionPlaceStack.canPlaceTiles(player, world, splitted, PlacementMode.all.getCoordsToCheck(splitted, previews.pos), PlacementMode.all)) {
			ArrayList<TileEntityLittleTiles> blocks = new ArrayList<>();
			SubWorld fakeWorld = SubWorld.createFakeWorld(world);
			LittleActionPlaceStack.placeTilesWithoutPlayer(fakeWorld, previews.context, splitted, previews.getStructure(), PlacementMode.all, previews.pos, null, null, null, null);
			
			controller.activator = player;
			
			if (world.isRemote) {
				controller.markWaitingForApprove();
				
				for (TileEntityLittleTiles te : tiles.keySet())
					if (te.waitingAnimation != null)
						te.clearWaitingAnimations();
			}
			
			LittleStructure newDoor = previews.getStructure();
			
			EntityAnimation animation = new EntityAnimation(world, fakeWorld, controller, previews.pos, uuid, absolute, newDoor.getAbsoluteIdentifier());
			
			if (parent != null) {
				LittleStructure parentStructure = parent.getStructure(world);
				parentStructure.updateChildConnection(parent.getChildID(), newDoor);
				newDoor.updateParentConnection(parent.getChildID(), parentStructure);
			}
			
			world.spawnEntity(animation);
			return true;
		}
		
		return false;
	}
	
	public boolean activate(World world, @Nullable EntityPlayer player, BlockPos pos, @Nullable LittleTile tile) {
		if (!hasLoaded() || !loadChildren()) {
			player.sendStatusMessage(new TextComponentTranslation("Cannot interact with door! Not all tiles are loaded!"), true);
			return false;
		}
		
		if (isChildMoving()) {
			player.sendStatusMessage(new TextComponentTranslation("A child is still in motion!"), true);
			return false;
		}
		
		UUID uuid = UUID.randomUUID();
		if (world.isRemote)
			PacketHandler.sendPacketToServer(new LittleDoorPacket(tile != null ? tile : getMainTile(), uuid));
		
		openDoor(world, player, new UUIDSupplier(uuid));
		
		return true;
	}
	
	@Override
	public void openDoor(World world, @Nullable EntityPlayer player, UUIDSupplier uuid) {
		HashMapList<TileEntityLittleTiles, LittleTile> tempTiles = getAllTiles(new HashMapList<>());
		HashMap<TileEntityLittleTiles, LittleGridContext> tempContext = new HashMap<>();
		
		StructureAbsolute absolute = getAbsoluteAxis();
		
		for (TileEntityLittleTiles te : tempTiles.keySet()) {
			tempContext.put(te, te.getContext());
		}
		
		for (Entry<TileEntityLittleTiles, ArrayList<LittleTile>> entry : tempTiles.entrySet()) {
			entry.getKey().preventUpdate = true;
			entry.getKey().removeTiles(entry.getValue());
			entry.getKey().preventUpdate = false;
		}
		
		if (tryToPlacePreviews(world, player, uuid.next(), absolute)) {
			for (Entry<TileEntityLittleTiles, ArrayList<LittleTile>> entry : tempTiles.entrySet()) {
				entry.getKey().updateTiles();
			}
			return;
		}
		
		for (Entry<TileEntityLittleTiles, ArrayList<LittleTile>> entry : tempTiles.entrySet()) {
			entry.getKey().convertTo(tempContext.get(entry.getKey()));
			entry.getKey().addTiles(entry.getValue());
		}
	}
	
	public abstract boolean tryToPlacePreviews(World world, EntityPlayer player, UUID uuid, StructureAbsolute absolute);
	
	@Override
	public boolean onBlockActivated(World world, LittleTile tile, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ, LittleActionActivated action) {
		if (world.isRemote) {
			activate(world, player, pos, tile);
			action.preventInteraction = true;
		}
		return true;
	}
	
	public abstract StructureAbsolute getAbsoluteAxis();
	
	public static void initDoors() {
		LittleStructureRegistry.registerStructureType("door", "door", LittleAxisDoor.class, LittleStructureAttribute.NONE, LittleAxisDoorParser.class);
		LittleStructureRegistry.registerStructureType("slidingDoor", "door", LittleSlidingDoor.class, LittleStructureAttribute.NONE, LittleSlidingDoorParser.class);
		LittleStructureRegistry.registerStructureType("advancedDoor", "door", LittleAdvancedDoor.class, LittleStructureAttribute.NONE, LittleAdvancedDoorParser.class);
		LittleStructureRegistry.registerStructureType("doorActivator", "door", LittleDoorActivator.class, LittleStructureAttribute.NONE, LittleDoorActivatorParser.class);
	}
	
	public static abstract class LittleDoorBaseParser extends LittleStructureGuiParser {
		
		public LittleDoorBaseParser(GuiParent parent, AnimationGuiHandler handler) {
			super(parent, handler);
		}
		
		@SideOnly(Side.CLIENT)
		@CustomEventSubscribe
		public void onChanged(GuiControlChangedEvent event) {
			if (event.source.is("duration_s"))
				updateTimeline();
		}
		
		@Override
		@SideOnly(Side.CLIENT)
		public void createControls(LittlePreviews previews, LittleStructure structure) {
			parent.controls.add(new GuiCheckBox("stayAnimated", CoreControl.translate("gui.door.stayAnimated"), 0, 120, structure instanceof LittleDoorBase ? ((LittleDoorBase) structure).stayAnimated : false).setCustomTooltip(CoreControl.translate("gui.door.stayAnimatedTooltip")));
			parent.controls.add(new GuiLabel(CoreControl.translate("gui.door.duration") + ":", 90, 122));
			parent.controls.add(new GuiSteppedSlider("duration_s", 140, 122, 50, 6, structure instanceof LittleDoorBase ? ((LittleDoorBase) structure).duration : 50, 1, 500));
			
			updateTimeline();
		}
		
		@Override
		@SideOnly(Side.CLIENT)
		public LittleDoorBase parseStructure(LittlePreviews previews) {
			GuiSteppedSlider slider = (GuiSteppedSlider) parent.get("duration_s");
			GuiCheckBox checkBox = (GuiCheckBox) parent.get("stayAnimated");
			return parseStructure((int) slider.value, checkBox.value);
		}
		
		@SideOnly(Side.CLIENT)
		public abstract LittleDoorBase parseStructure(int duration, boolean stayAnimated);
		
		@SideOnly(Side.CLIENT)
		public abstract void populateTimeline(AnimationTimeline timeline);
		
		public void updateTimeline() {
			GuiSteppedSlider slider = (GuiSteppedSlider) parent.get("duration_s");
			AnimationTimeline timeline = new AnimationTimeline((int) slider.value, new PairList<>());
			populateTimeline(timeline);
			handler.setTimeline(timeline);
		}
		
	}
}
