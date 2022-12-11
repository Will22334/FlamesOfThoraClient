package com.thora.core.world;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class KeyMapWorld extends GeneralWorld {
	
	public class MTile extends AbstractTile {
		private MTile(Material material, Location point) {
			super(material, point);
		}
		@Override
		public KeyMapWorld getWorld() {
			return KeyMapWorld.this;
		}
	}
	
	private Map<Location,MTile> safeTileMap;
	private Map<Location,MTile> tileMap;
	
	public KeyMapWorld(Supplier<Map<Location,MTile>> mapSupplier, String name, Locatable origin, TileGenerator generator) {
		super(name, origin, generator);
		tileMap = mapSupplier.get();
		safeTileMap = Collections.unmodifiableMap(tileMap);
	}
	
	public KeyMapWorld(Map<Location,MTile> tileMap, String name, Locatable origin, TileGenerator generator) {
		super(name, origin, generator);
		this.tileMap = tileMap;
		safeTileMap = Collections.unmodifiableMap(tileMap);
	}
	
	@Override
	public final Map<Location,MTile> getTiles() {
		return safeTileMap;
	}

	@Override
	public MTile getTile(int x, int y) {
		return getTile(new WeakVectorLocation<>(this, x, y));
	}
	
	@Override
	public MTile getTile(Location point) {
		return tileMap.get(point);
	}
	
	@Override
	public MTile setTile(Material material, Location point) {
		MTile tile = tileMap.get(point);
		if(tile != null) {
			tile.data = new BasicTileData(material);
		} else {
			tile = new MTile(material, point);
			tileMap.put(point, tile);
		}
		return tile;
	}
	
	@Override
	public MTile setTile(Material material, int x, int y) {
		return setTile(material, new WeakVectorLocation<>(this, x, y));
	}

	@Override
	public Stream<MTile> tiles() {
		return tileMap.values().stream();
	}

	@Override
	public WeakVectorLocation<KeyMapWorld> getLocation(int x, int y) {
		return new WeakVectorLocation<>(x, y);
	}
	
}
