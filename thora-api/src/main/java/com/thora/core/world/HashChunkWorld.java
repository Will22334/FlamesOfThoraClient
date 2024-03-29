package com.thora.core.world;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.badlogic.ashley.core.PooledEngine;
import com.thora.core.math.FinalIntVector;
import com.thora.core.world.HashChunkWorld.HashChunk.CTile;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

public abstract class HashChunkWorld extends GeneralWorld {
	
	public static class HashChunk extends Chunk {
		
		public class CTile extends BasicTile {
			public CTile(final TileData data, final Location point) {
				super(data, point);
			}
			public CTile(final Material material, final Location point) {
				super(material, point);
			}
			@Override
			public HashChunkWorld world() {
				return getChunk().world();
			}
			protected HashChunk getChunk() {
				return HashChunk.this;
			}
			public Location getOrigin() {
				return getChunk().getOrigin();
			}
			protected int ix() {
				return getX() - getOrigin().getX();
			}
			protected int iy() {
				return getY() - getOrigin().getY();
			}
		}
		
		private final HashChunkWorld world;
		public final CTile[][] tiles;
		protected final IntObjectMap<WorldEntity> entities = new IntObjectHashMap<>();
		public final ChunkCoordinate coord;
		public final Location bottomLeft;
		
		protected HashChunk(final HashChunkWorld world, final ChunkCoordinate coord) {
			this.world = world;
			this.coord = coord;
			this.bottomLeft = world.getChunkOrigin(coord);
			this.tiles = new CTile[world.chunkHeight][world.chunkWidth];
		}
		
		@Override
		public final HashChunkWorld world() {
			return world;
		}
		
		@Override
		public Location getOrigin() {
			return bottomLeft;
		}
		
		@Override
		public int chunkWidth() {
			return world().chunkWidth;
		}
		
		@Override
		public int chunkHeight() {
			return world().chunkHeight;
		}
		
		protected int ix(int wx) {
			return wx - bottomLeft.getX();
		}
		
		protected int ix(Locatable loc) {
			return ix(loc.getX());
		}
		
		protected int iy(int wy) {
			return wy - bottomLeft.getY();
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
		
		protected CTile setTile(final CTile tile, final TileData data) {
			tile.data = data;
			return tile;
		}
		
		public CTile setTile(Material type, int wx, int wy) {
			CTile tile = getTile(wx, wy);
			tile.data = new BasicTileData(type);
			return tile;
		}
		
		@Override
		public Stream<CTile> tiles() {
			return Arrays.stream(tiles)
					.flatMap(Arrays::stream);
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
			for(int y=0; y<chunkHeight(); ++y) {
				for(int x=0; x<chunkWidth(); ++x) {
					Location point = getOrigin().clone().shift(x, y);
					tiles[y][x] = new CTile(new BasicTileData(world().generate(point)), point);
				}
			}
			return this;
		}
		
		@Override
		public Stream<? extends WorldEntity> entities() {
			return entities.values().stream();
		}
		
		public void addEntity(WorldEntity e) {
			this.entities.put(e.getID(), e);
		}
		
		public void removeEntity(WorldEntity e) {
			this.entities.remove(e.getID());
		}
		
	}
	
	protected static class ChunkCoordinate extends FinalIntVector {
		public ChunkCoordinate(int x, int y) {
			super(x, y);
		}
		@Override
		public ChunkCoordinate clone() {
			return new ChunkCoordinate(x, y);
		}
	}
	
	private static final Logger logger = LogManager.getLogger(HashChunkWorld.class);
	
	protected final int chunkWidth;
	protected final int chunkHeight;
	
	protected final Map<ChunkCoordinate,HashChunk> chunks = new ConcurrentHashMap<ChunkCoordinate,HashChunk>();
	protected final Map<Integer,WorldEntity> entities = new ConcurrentHashMap<Integer,WorldEntity>();
	
	public HashChunkWorld(final String name, final Locatable origin, final int chunkWidth, final int chunkHeight, final PooledEngine engine, final TileGenerator generator) {
		super(name, origin, engine, generator);
		
		this.chunkWidth = chunkWidth;
		this.chunkHeight = chunkHeight;
	}
	
	public HashChunkWorld(final String name, final int chunkWidth, final int chunkHeight, final PooledEngine engine, final TileGenerator generator) {
		super(name, engine, generator);
		
		this.chunkWidth = chunkWidth;
		this.chunkHeight = chunkHeight;
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
	
	protected WeakVectorLocation<HashChunkWorld> getChunkOrigin(int cx, int cy) {
		return new WeakVectorLocation<>(this, chunkWidth * cx - chunkWidth/2,
				chunkHeight * cy - chunkHeight/2);
	}
	
	protected Location getChunkOrigin(ChunkCoordinate c) {
		return getChunkOrigin(c.getIX(), c.getIY());
	}
	
	protected HashChunk createChunk(final ChunkCoordinate coord) {
		return new HashChunk(this, coord);
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
	
	public Stream<HashChunk> surroundingChunks(final Locatable p, final int chunkRange) {
		return surroundingChunks(p, chunkRange, chunkRange);
	}
	
	public Stream<HashChunk> surroundingChunks(final Locatable p, final int chunkXRange, final int ChunkYRange) {
		final HashChunk centerChunk = getGeneratedChunk(p);
		final int cx = centerChunk.coord.getIX(), cy = centerChunk.coord.getIY();
		return IntStream.rangeClosed(cy-ChunkYRange, cy+ChunkYRange)
				.mapToObj(y -> {
					return IntStream.rangeClosed(cx-chunkXRange, cx+chunkXRange)
							.mapToObj(x -> getGeneratedChunk(new ChunkCoordinate(x,y)));
				})
				.flatMap(Function.identity());
	}
	
	protected int rangeToChunkDepth(int range) {
		double r = range / (double)(Math.min(chunkWidth, chunkHeight));
		return (int) Math.ceil(r);
	}
	
	@Override
	public Stream<HashChunk.CTile> tiles() {
		return chunks()
				.flatMap(HashChunk::tiles);
	}

	@Override
	public Stream<HashChunk.CTile> surroundingTiles(Locatable center, int range) {
		return surroundingChunks(center, rangeToChunkDepth(range))
				.flatMap(HashChunk::tiles)
				.filter(t -> center.getOrthogonallDistance(t) <= range);
	}
	
	@Override
	public Stream<HashChunk.CTile> surroundingTiles(final Locatable center, final double range) {
		final int iRange = (int) Math.ceil(range);
		return surroundingChunks(center, rangeToChunkDepth(iRange))
				.flatMap(HashChunk::tiles)
				.filter(t -> center.isInRange(t, range));
	}
	
	@Override
	public Stream<HashChunk.CTile> surroundingTiles(Locatable center) {
		return surroundingChunks(center, 1)
				.flatMap(HashChunk::tiles);
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
	public Tile setTile(Material type, int wx, int wy) {
		return getGeneratedChunk(wx, wy).setTile(type, wx, wy);
	}
	
	@Override
	public Tile setTile(Material type, Location p) {
		return setTile(type, p.getX(), p.getY());
	}
	
	@Override
	public Tile setTile(Location point, TileData data) {
		return setTile(data.material(), point);
	}
	
	@Override
	public WeakVectorLocation<HashChunkWorld> getLocation(int x, int y) {
		return new WeakVectorLocation<HashChunkWorld>(this, x, y);
	}
	
	@Override
	public Stream<? extends WorldEntity> entities() {
		return entities.values().stream();
	}
	
	protected abstract boolean doRegister(WorldEntity e);
	
	protected abstract boolean doDeRegister(WorldEntity e);
	
	@Override
	public void moveEntity(WorldEntity e, Tile p) {
		this.moveEntity(e, (HashChunk.CTile) p);
	}
	
	public void moveEntity(final WorldEntity e, final HashChunk.CTile newTile) {
		final HashChunk.CTile oldTile = (CTile) e.getTile();
		final HashChunk oldChunk = oldTile.getChunk();
		final HashChunk newChunk = newTile.getChunk();
		if(!oldChunk.equals(newChunk)) {
			oldChunk.removeEntity(e);
			e.setLocation(newTile.getLocation());
			newChunk.addEntity(e);
		} else {
			e.setLocation(newTile.getLocation());
		}
		onMoveEntity(e, oldTile);
	}
	
	protected void onMoveEntity(final WorldEntity e, final HashChunk.CTile oldTile) {
		
	}

	@Override
	public WorldEntity getEntity(int id) {
		return entities.get(id);
	}
	
}
