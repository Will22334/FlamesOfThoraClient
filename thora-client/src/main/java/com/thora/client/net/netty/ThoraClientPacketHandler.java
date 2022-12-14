package com.thora.client.net.netty;

import org.apache.logging.log4j.Logger;

import com.thora.client.FlamesOfThoraClient;
import com.thora.core.net.LoginTransaction;
import com.thora.core.net.message.BasicTileMessage;
import com.thora.core.net.message.ChatMessage;
import com.thora.core.net.message.LoginResponseMessage;
import com.thora.core.net.message.StateChangeMessage;
import com.thora.core.net.message.ThoraMessage;
import com.thora.core.net.message.WorldDefinitionMessage;
import com.thora.core.net.netty.PodHandler;
import com.thora.core.world.Location;
import com.thora.core.world.TileData;
import com.thora.core.world.World;

import io.netty.channel.ChannelHandlerContext;

public class ThoraClientPacketHandler extends PodHandler<ThoraMessage> {
	
	private final FlamesOfThoraClient client;
	private final NettyNetworkManager manager;
	
	protected ThoraClientPacketHandler(NettyNetworkManager manager, Logger logger) {
		super(logger);
		this.manager = manager;
		this.client = manager.client();
	}
	
	protected final NettyNetworkManager getManager() {
		return manager;
	}
	
	protected final FlamesOfThoraClient client() {
		return client;
	}
	
	@Override
	protected void populate() {
		addHandler(new LoginResponseConsumer());
		addHandler(new ChatMessageConsumer());
		addHandler(new WorldDefinitionConsumer());
		addHandler(new TileMessageConsumer());
		addHandler(new StateChangeMessageConsumer());
	}
	
	public class LoginResponseConsumer extends MessageConsumer<LoginResponseMessage> {
		
		@Override
		public void consume(ChannelHandlerContext ctx, LoginResponseMessage message) {
			LoginTransaction t = getManager().loginTransaction;
			t.response = message;
			getManager().loginPromise.setSuccess(t);
			ctx.channel().writeAndFlush(new ChatMessage("Secret 2.1327"));
		}
		
	}
	
	public class ChatMessageConsumer extends MessageConsumer<ChatMessage> {

		@Override
		public void consume(ChannelHandlerContext ctx, ChatMessage message) {
			PlayerSession session = PlayerSession.findSession(ctx);
			logger().info("Got Message \"{}\" from {}", message.message, session);
		}
		
	}
	
	public class WorldDefinitionConsumer extends MessageConsumer<WorldDefinitionMessage> {
		@Override
		public void consume(ChannelHandlerContext ctx, WorldDefinitionMessage message) {
			logger().debug("New World Definition = {}", message.world().getName());
			client().setWorld(message.world());
		}
	}
	
	public class TileMessageConsumer extends MessageConsumer<BasicTileMessage> {

		@Override
		public void consume(ChannelHandlerContext ctx, BasicTileMessage message) {
			PlayerSession session = PlayerSession.findSession(ctx);
			if(message.isGroup()) {
				final World world = client().world();
				final Location bottomLeft = message.bottomLeft;
				final TileData[][] tiles = message.tiles;
				final int height = tiles.length, width = tiles[0].length;
				logger().trace("Set Tiles[{}-{}] = {}", message.bottomLeft, message.bottomLeft.clone().shift(width, height));
				for(int y=0; y<height; ++y) {
					for(int x=0; x<width; ++x) {
						world.setTile(bottomLeft.clone().shift(x, y), tiles[y][x]);
					}
				}
				
			} else {
				logger().trace("Set Tile {} = {}", message.bottomLeft, message.data);
				client().world().setTile(message.bottomLeft, message.data);
			}
			
		}
		
	}
	
	public class StateChangeMessageConsumer extends MessageConsumer<StateChangeMessage> {
		@Override
		public void consume(ChannelHandlerContext ctx, StateChangeMessage message) {
			logger().debug("Recieved StateChange[{}]", message.stateID);
			client().addTask(() -> {
				client().States.setActiveState(message.stateID);
			});
		}
	}
	
}
