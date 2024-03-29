package com.thora.core.world;

public class BasicTile implements Tile {
	
	Location point;
	protected TileData data;
	
	public BasicTile(TileData data, Location point) {
		this.point = point;
		this.data = data;
	}
	
	public BasicTile(Material material, Location point) {
		this(new BasicTileData(material), point);
	}
	
	@Override
	public Location getLocation() {
		return point;
	}
	
	@Override
	public TileData getTileData() {
		return data;
	}
	
	@Override
	public TileData setTileData(final TileData tileData) {
		final TileData old = this.data;
		this.data = tileData;
		return old;
	}
	
	@Override
	public void setMaterial(Material material) {
		this.data = new BasicTileData(material);
	}

	@Override
	public String toString() {
		return "[" + this.getMaterial() + ": " + getLocation() + "]";
	}
	
}
