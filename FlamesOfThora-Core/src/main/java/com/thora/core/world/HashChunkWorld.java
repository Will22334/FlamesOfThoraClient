package com.thora.core.world;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thora.core.FlamesOfThora;
import com.thora.core.IntVector;

public class HashChunkWorld extends AbstractWorld {
	
	public class HashChunk extends Chunk {
		
		public class CTile extends AbstractTile {
			protected CTile(TileType type, Location point) {
				super(type, point);
			}
			@Override
			public World getWorld() {
				return getChunk().getWorld();
			}
			protected HashChunk getChunk() {
				return HashChunk.this;
			}
			protected int ix() {
				return getX() - getOrigin().getX();
			}
			protected int iy() {
				return getY() - getOrigin().getY();
			}
		}
		
		
		
		private final CTile[][] tiles;
		protected final ChunkCoordinate coord;
		protected final Location origin;
		
		protected HashChunk(ChunkCoordinate coord) {
			this.coord = coord;
			this.origin = getChunkOrigin(coord);
			this.tiles = new CTile[chunkHeight][chunkWidth];
		}
		
		@Override
		public final HashChunkWorld getWorld() {
			return HashChunkWorld.this;
		}
		
		@Override
		public Location getOrigin() {
			return origin;
		}
		
		@Override
		public int getWidth() {
			return chunkWidth;
		}
		
		@Override
		public int getHeight() {
			return chunkHeight;
		}
		
		protected int ix(int wx) {
			return wx - origin.getX();
		}
		
		protected int ix(Locatable loc) {
			return ix(loc.getX());
		}
		
		protected int iy(int wy) {
			return wy - origin.getY();
		}
		
		protected int iy(Locatable loc) {
			return iy(loc.getY());
		}
		
		public CTile getTile(Locatable loc) {
			return getTile(loc.getX(), loc.getY());
		}
		
		public CTile getTile(int wx, int wy) {
			return tiles[iy(wy)][ix(wx)];
		}
		
		public CTile setTile(TileType type, int wx, int wy) {
			CTile tile = getTile(wx, wy);
			tile.type = type;
			return tile;
		}
		
		@Override
		public Stream<CTile> tiles() {
			return IntStream.range(0, chunkHeight)
					.mapToObj(y -> {
						return IntStream.range(0, chunkWidth)
								.mapToObj(x -> tiles[y][x]);
					})
					.flatMap(Function.identity());
		}
		
		protected boolean isGenerated() {
			return tiles[0][0] != null;
		}
		
		public HashChunk ensureGenerated() {
			return generate(false);
		}
		
		protected HashChunk generate(boolean overWrite) {
			if(!isGenerated() || overWrite) {
				generate();
			}
			return this;
		}
		
		protected HashChunk generate() {
			for(int y=0; y<chunkHeight; ++y) {
				for(int x=0; x<chunkWidth; ++x) {
					Location point = getOrigin().clone().shift(x, y);
					tiles[y][x] = new CTile(getWorld().generate(point), point);
				}
			}
			return this;
		}
		
	}
	
	private static class ChunkCoordinate extends IntVector {
		public ChunkCoordinate(int x, int y) {
			super(x, y);
		}
		
		@Override
		protected ChunkCoordinate clone() throws CloneNotSupportedException {
			return new ChunkCoordinate(x, y);
		}
	}
	
	private static final Logger logger = LogManager.getLogger(HashChunkWorld.class);
	
	private Pole InverseOrigin = new Pole("Inverse", 0 , 0);
	
	private final Dimension chunkSize;
	private final int chunkWidth, chunkHeight;
	
	private Map<ChunkCoordinate,HashChunk> chunks = new HashMap<ChunkCoordinate,HashChunk>();
	
	public HashChunkWorld(String name, Locatable origin, int chunkWidth, int chunkHeight, TileGenerator generator) {
		super(name, origin, generator);
		
		this.chunkWidth = chunkWidth;
		this.chunkHeight = chunkHeight;
		this.chunkSize = new Dimension(chunkWidth, chunkHeight);
		
		create();
		
	}
	
	@Override
	public Logger logger() {
		return logger;
	}
	
	protected HashChunk getGeneratedChunk(int wx, int wy) {
		return getGeneratedChunk(getChunkCoord(wx, wy));
	}
	
	protected HashChunk getGeneratedChunk(ChunkCoordinate coord) {
		return chunks.computeIfAbsent(coord, this::createChunk)
				.ensureGenerated();
	}
	
	protected HashChunk getGeneratedChunk(Locatable loc) {
		return getGeneratedChunk(loc.getX(), loc.getY());
	}
	
	protected HashChunk getChunk(ChunkCoordinate coord) {
		return chunks.get(coord);
	}
	
	protected HashChunk getChunk(Locatable loc) {
		return getChunk(getChunkCoord(loc));
	}
	
	protected HashChunk getChunk(int wx, int wy) {
		return getChunk(getChunkCoord(wx, wy));
	}
	
	protected ChunkCoordinate getChunkCoord(int wx, int wy) {
		int cx = (int) Math.floor((wx+(chunkWidth-1)/2d)/chunkWidth);
		int cy = (int) Math.floor((wy+(chunkHeight-1)/2d)/chunkHeight);
		return new ChunkCoordinate(cx,cy);
	}
	
	protected ChunkCoordinate getChunkCoord(Locatable loc) {
		return getChunkCoord(loc.getX(), loc.getY());
	}
	
	protected Location getChunkOrigin(int cx, int cy) {
		return new Location(chunkWidth * cx - chunkWidth/2,
				chunkHeight * cy - chunkHeight/2);
	}
	
	protected Location getChunkOrigin(ChunkCoordinate c) {
		return getChunkOrigin(c.x, c.y);
	}
	
	protected HashChunk createGeneratedChunk(ChunkCoordinate coord) {
		return createChunk(coord).ensureGenerated();
	}
	
	protected HashChunk createChunk(ChunkCoordinate coord) {
		return new HashChunk(coord);
	}
	
	@Override
	protected Rectangle getSpawnRegion() {
		int r = 20;
		return new Rectangle(-r, -r, 2*r, 2*r);
	}
	
	protected float chunkWidths(int dwx) {
		return 1f * dwx / chunkWidth;
	}
	
	protected float chunkHeights(int dwy) {
		return 1f * dwy / chunkHeight;
	}
	
	protected Stream<HashChunk> chunks() {
		return chunks.values().stream();
	}
	
	protected Stream<HashChunk> surroundingChunks(Locatable p, int chunkRange) {
		HashChunk centerChunk = getGeneratedChunk(p);
		int cx = centerChunk.coord.x, cy = centerChunk.coord.y;
		return IntStream.rangeClosed(cy-chunkRange, cy+chunkRange)
				.mapToObj(y -> {
					return IntStream.rangeClosed(cx-chunkRange, cx+chunkRange)
							.mapToObj(x -> getGeneratedChunk(new ChunkCoordinate(x,y)));
				})
				.flatMap(Function.identity());
	}
	
	@Override
	public Stream<HashChunk.CTile> tiles() {
		return chunks()
				.flatMap(HashChunk::tiles);
	}
	
	@Override
	public Stream<HashChunk.CTile> surroundingTiles(Locatable center, int range) {
		return surroundingChunks(center, 1)
				.flatMap(HashChunk::tiles)
				.filter(t -> center.getTileDistance(t) <= FlamesOfThora.DEFAULT_VIEW_RANGE);
	}
	
	@Override
	public Tile getTile(int wx, int wy) {
		return getGeneratedChunk(wx, wy).getTile(wx, wy);
	}
	
	@Override
	public Tile getTile(Location p) {
		return getTile(p.getX(), p.getY());
	}
	
	@Override
	public Tile setTile(TileType type, int wx, int wy) {
		return getGeneratedChunk(wx, wy).setTile(type, wx, wy);
	}
	
	@Override
	public Tile setTile(TileType type, Location p) {
		return setTile(type, p.getX(), p.getY());
	}
	
}
