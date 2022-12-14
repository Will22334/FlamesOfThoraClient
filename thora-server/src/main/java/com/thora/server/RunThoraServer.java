package com.thora.server;

import java.net.InetSocketAddress;
import java.security.KeyPair;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.badlogic.ashley.core.PooledEngine;
import com.thora.core.net.netty.EncodingUtils;
import com.thora.core.world.Pole;
import com.thora.core.world.TileGenerator;
import com.thora.core.world.generator.PerlinTileGenerator;
import com.thora.server.netty.NettyThoraServer;
import com.thora.server.world.ServerHashChunkWorld;

public class RunThoraServer {
	
	public static final int DEFAULT_CHUNK_SIZE = 20;
	
	public static final float DEFAULT_WORLD_SCALE = 35f;
	public static final float DEFAULT_WORLD_FREQ = 20f;
	
	static final Logger logger = LogManager.getLogger(RunThoraServer.class);
	
	static final Logger logger() {
		return logger;
	}
	
	public static void main(String[] args) throws Exception {
		
		final InetSocketAddress bindSocket = EncodingUtils.parseSocketAddress(args[0]);
		
		KeyPair keyIdentity;
		try {
			keyIdentity = ThoraServer.readKeyPair(ThoraServer.PATH_KEYS_DIR);
		} catch (Throwable t) {
			logger().atLevel(Level.ERROR).withThrowable(t).log("Failed to read server identity from {}", ThoraServer.PATH_KEYS_DIR);
			throw t;
		}
		
		logger().trace("Server Public Key = {}", keyIdentity.getPublic().hashCode());
		
		int sideThreads = 4;
		int bossIOThreads = 1;
		int workerIOThreads = 2;
		
		Pole origin = new Pole("Origin",0,0);
		TileGenerator gen = new PerlinTileGenerator((int)System.currentTimeMillis(), DEFAULT_WORLD_SCALE, DEFAULT_WORLD_FREQ);
		//World world = new HashChunkWorld("Earth", origin, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_SIZE, gen);
		//AbstractWorld world = new QuadTreeWorld("Earth", 1000, 1000, gen);
		
		PooledEngine engine = new PooledEngine();
		
		
		ServerHashChunkWorld world = new ServerHashChunkWorld("Earth", origin, 25, 25, engine, gen);
		
		world.initialize();
		
		
		
		NettyThoraServer server = new NettyThoraServer(keyIdentity, sideThreads, bossIOThreads, workerIOThreads, world);
		
		server.start(bindSocket);
		
	}
	
}
