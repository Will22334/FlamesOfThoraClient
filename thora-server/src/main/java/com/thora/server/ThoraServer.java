package com.thora.server;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thora.core.HasLogger;
import com.thora.core.net.netty.EncodingUtils;
import com.thora.core.world.World;

public abstract class ThoraServer implements HasLogger {
	
	public static final Path PATH_ASSETS_DIR = Paths.get("./assets");
	public static final Path PATH_KEYS_DIR = PATH_ASSETS_DIR.resolve("keys");
	
	public static enum Status {
		OFF(),
		STARTING(),
		ON(true),
		STOPPING();
		private boolean up;
		public boolean isUp() {
			return up;
		}
		private Status(boolean up) {
			this.up = up;
		}
		private Status() {
			this(false);
		}
	}
	
	public static final KeyPair readKeyPair(Path dir) throws Exception {
		KeyPair keyIdentity = EncodingUtils.readKeyPair(dir.resolve("public.key"), dir.resolve("private.key"));
		return keyIdentity;
	}
	
	private static final Logger logger = LogManager.getLogger(ThoraServer.class);
	
	public static Logger globalLogger() {
		return logger;
	}
	
	private final KeyPair identity;
	protected Status status = Status.OFF;
	
	public ThoraServer(KeyPair identity) {
		this.identity = identity;
	}
	
	@Override
	public Logger logger() {
		return globalLogger();
	}
	
	public Status getStatus() {
		return status;
	}
	
	protected KeyPair identity() {
		return identity;
	}
	
	public PublicKey publicKey() {
		return identity().getPublic();
	}
	
	public abstract InetSocketAddress getListenSocket();
	
	public abstract void start(InetSocketAddress address) throws Exception;
	
	public abstract void stop();
	
	public abstract World getWorld();
	
}