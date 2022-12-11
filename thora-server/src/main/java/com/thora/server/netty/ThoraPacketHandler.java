package com.thora.server.netty;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.thora.core.net.message.LoginRequestMessage;
import com.thora.core.net.message.LoginResponseMessage;
import com.thora.core.net.message.ThoraMessage;
import com.thora.core.net.netty.PodHandler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

public class ThoraPacketHandler extends PodHandler<ThoraMessage> {
	
	protected final NettyThoraServer server;
	
	ThoraPacketHandler(NettyThoraServer server, Logger logger) {
		super(logger);
		this.server = server;
	}

	@Override
	protected void populate() {
		this.addHandler(new LoginRequestHandler());
	}
	
	private final class LoginRequestHandler extends MessageConsumer<LoginRequestMessage> {
		@Override
		public void consume(ChannelHandlerContext ctx, LoginRequestMessage message) {
			ClientSession session = ClientSession.get(ctx);
			logger().atLevel(Level.TRACE).log("Received login request = {}", message);
			session.generateSymmetricCipher(server.publicKey(), message.sessionKey);
			
			LoginResponseMessage response = new LoginResponseMessage(true, "Successfully logged in!");
			ChannelFuture cf = session.rawChannel().writeAndFlush(response);
			if(response.isAccepted()) {
				
			}
		}
	}
	
}
