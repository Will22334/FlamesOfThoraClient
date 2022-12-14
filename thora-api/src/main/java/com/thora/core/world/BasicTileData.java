package com.thora.core.world;

public class BasicTileData extends AbstractTileData {
	
	protected Material material;
	
	public BasicTileData(Material material) {
		this.material = material;
	}
	
	@Override
	public Material material() {
		return material;
	}
	
	public void setMaterial(Material material) {
		this.material = material;
	}
	
}
