package com.creativemd.littletiles.common.utils.selection.selector;

import com.creativemd.littletiles.common.tiles.LittleTile;
import com.creativemd.littletiles.common.tiles.LittleTileBlock;

import net.minecraft.nbt.NBTTagCompound;

public class NoStructureSelector extends TileSelector {
	
	public NoStructureSelector() {
		
	}
	
	@Override
	protected void saveNBT(NBTTagCompound nbt) {
		
	}
	
	@Override
	protected void loadNBT(NBTTagCompound nbt) {
		
	}
	
	@Override
	public boolean is(LittleTile tile) {
		return !((LittleTileBlock) tile).isChildOfStructure();
	}
	
}
