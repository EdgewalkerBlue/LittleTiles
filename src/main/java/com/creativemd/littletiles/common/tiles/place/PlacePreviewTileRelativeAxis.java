package com.creativemd.littletiles.common.tiles.place;

import java.util.List;

import com.creativemd.littletiles.client.tiles.LittleRenderingCube;
import com.creativemd.littletiles.common.structure.registry.LittleStructureType.StructureTypeRelative;
import com.creativemd.littletiles.common.structure.relative.StructureRelative;
import com.creativemd.littletiles.common.tiles.preview.LittlePreviews;
import com.creativemd.littletiles.common.tiles.vec.LittleTileBox;
import com.creativemd.littletiles.common.utils.grid.LittleGridContext;

import net.minecraft.util.EnumFacing.Axis;

public class PlacePreviewTileRelativeAxis extends PlacePreviewTileRelative {
	
	public Axis axis;
	
	public PlacePreviewTileRelativeAxis(LittleTileBox box, LittlePreviews structure, StructureRelative relative, StructureTypeRelative relativeType, Axis axis) {
		super(box, structure, relative, relativeType);
		this.axis = axis;
	}
	
	@Override
	public List<LittleRenderingCube> getPreviews(LittleGridContext context) {
		List<LittleRenderingCube> cubes = super.getPreviews(context);
		LittleRenderingCube cube = cubes.get(0);
		int max = 40 * context.size;
		int min = -max;
		switch (axis) {
		case X:
			cube.minX = min;
			cube.maxX = max;
			break;
		case Y:
			cube.minY = min;
			cube.maxY = max;
			break;
		case Z:
			cube.minZ = min;
			cube.maxZ = max;
			break;
		default:
			break;
		}
		cubes.add(cube);
		return cubes;
	}
}
