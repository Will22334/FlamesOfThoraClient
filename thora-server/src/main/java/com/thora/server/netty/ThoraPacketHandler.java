package com.thora.server.netty;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.thora.core.net.message.BasicTileMessage;
import com.thora.core.net.message.ChatMessage;
import com.thora.core.net.message.LoginRequestMessage;
import com.thora.core.net.message.LoginResponseMessage;
import com.thora.core.net.message.StateChangeMessage;
import com.thora.core.net.message.ThoraMessage;
import com.thora.core.net.message.WorldDefinitionMessage;
import com.thora.core.net.netty.PodHandler;
import com.thora.core.world.Location;
import com.thora.core.world.WeakVectorLocation;
import com.thora.core.world.World;
import com.thora.server.world.PlayerEntity;
import com.thora.server.world.ServerHashChunkWorld;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

public class ThoraPacketHandler extends PodHandler<ThoraMessage> {
	
	protected final NettyThoraServer server;
	
	ThoraPacketHandler(NettyThoraServer server, Logger logger) {
		super(logger);
		this.server = server;
	}
	
	protected final NettyThoraServer server() {
		return server;
	}
	
	@Override
	protected void populate() {
		this.addHandler(new LoginRequestHandler());
		this.addHandler(new ChatMessageHandler());
	}
	
	private final class LoginRequestHandler extends MessageConsumer<LoginRequestMessage> {
		@Override
		public void consume(ChannelHandlerContext ctx, LoginRequestMessage message) {
			final ClientSession session = ClientSession.get(ctx);
			logger().atLevel(Level.TRACE).log("Received login request = {}", message);
			session.generateSymmetricCipher(server.publicKey(), message.sessionKey, message.timeStamp);
			
			LoginResponseMessage response = new LoginResponseMessage(true, "Successfully logged in!");
			ChannelFuture cf = session.rawChannel().write(response);
			if(response.isAccepted()) {
				final ServerHashChunkWorld w = server().getWorld();
				final Location l = new WeakVectorLocation<>(w,0,0);
				final PlayerEntity p = new PlayerEntity(message.username, l);
				w.register(p);
				
				
				
				session.write(new WorldDefinitionMessage(w));
				
				w.informSurroundingTiles(p, session);
				
				session.write(new StateChangeMessage(3));
				
			}
			session.rawChannel().flush();
		}
	}
	
	private final class ChatMessageHandler extends MessageConsumer<ChatMessage> {
		@Override
		public void consume(ChannelHandlerContext ctx, ChatMessage message) {
			ClientSession session = ClientSession.get(ctx);
			logger().info("Got message \"{}\" from {}", message.message, session);
		}
	}
	
}
