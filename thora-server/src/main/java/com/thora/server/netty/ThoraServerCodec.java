package com.thora.server.netty;

import java.io.IOException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.logging.log4j.Logger;

import com.thora.core.net.message.BasicTileMessage;
import com.thora.core.net.message.ChatMessage;
import com.thora.core.net.message.LoginRequestMessage;
import com.thora.core.net.message.LoginResponseMessage;
import com.thora.core.net.message.StateChangeMessage;
import com.thora.core.net.message.WorldDefinitionMessage;
import com.thora.core.net.netty.EncodingUtils;
import com.thora.core.net.netty.ThoraCodec;
import com.thora.core.world.TileData;
import com.thora.core.world.World;
import com.thora.server.ThoraServer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class ThoraServerCodec extends ThoraCodec {
	
	private final ThoraServer server;
	
	protected ThoraServer server() {
		return server;
	}
	
	public ThoraServerCodec(ThoraServer server, Logger logger) {
		super(logger);
		this.server = server;
	}
	
	@Override
	protected ClientSession getSession(ChannelHandlerContext ctx) {
		return ClientSession.get(ctx);
	}
	
	@Override
	protected void populate() {
		
		addDecoder(new LoginRequestDecoder());
		addDecoder(new ChatMessageDecoder());
		
		
		addEncoder(new LoginResponseEncoder());
		addEncoder(new ChatMessageEncoder());
		addEncoder(new WorldDefMessageEncoder());
		addEncoder(new TileMessageEncoder());
		addEncoder(new StateChangeMessageEncoder());
		
	}
	
	public class LoginRequestDecoder extends MessageDecoder<LoginRequestMessage> {

		public LoginRequestDecoder() {
			super(ThoraCodec.OPCODE_SERVER_LOGIN_REQUEST);
		}

		@Override
		public LoginRequestMessage decode(ChannelHandlerContext ctx, ByteBuf encBuf) throws IOException {
			ClientSession session = getSession(ctx);
			if(!encBuf.isReadable()) {
				return null;
			}
			ByteBuf buf = ctx.alloc().buffer();
			try {
				EncodingUtils.decryptOther(buf, encBuf, session.getCryptoCreds().getAssymetric().getDecryptCipher());
				long sessionKey = buf.readLong();
				long timeStamp = buf.readLong();
				String username = EncodingUtils.readByteString(buf);
				String password = EncodingUtils.readByteString(buf);
				return new LoginRequestMessage(username, password, sessionKey, timeStamp);
			} catch (IllegalBlockSizeException | BadPaddingException e) {
				throw new IOException(e);
			} finally {
				buf.release();
			}
		}
		
	}
	
	public class LoginResponseEncoder extends EncryptedPayloadMessageEncoder<LoginResponseMessage> {
		LoginResponseEncoder() {
			super(ThoraCodec.OPCODE_CLIENT_LOGIN_RESPONSE);
		}
		@Override
		public void encodePlain(ChannelHandlerContext ctx, LoginResponseMessage packet, ByteBuf buf) {
			buf.writeBoolean(packet.isAccepted());
			EncodingUtils.writeString(packet.getReason(), buf);
		}
	}
	
	public class ChatMessageEncoder extends EncryptedPayloadMessageEncoder<ChatMessage> {
		protected ChatMessageEncoder() {
			super(OPCODE_CLIENT_CHAT_MESSAGE);
		}

		@Override
		public void encodePlain(ChannelHandlerContext ctx, ChatMessage packet, ByteBuf buf) {
			EncodingUtils.writeString(packet.message, buf);
		}
	}
	
	public class ChatMessageDecoder extends EncryptedPayloadMessageDecoder<ChatMessage> {

		public ChatMessageDecoder() {
			super(OPCODE_SERVER_CHAT_MESSAGE);
		}

		@Override
		protected ChatMessage decodePlain(ChannelHandlerContext ctx, ByteBuf buf) {
			String text = EncodingUtils.readString(buf);
			return new ChatMessage(text);
		}
		
	}
	
	public class WorldDefMessageEncoder extends MessageEncoder<WorldDefinitionMessage> {
		protected WorldDefMessageEncoder() {
			super(OPCODE_CLIENT_WORLD_INFORM);
		}

		@Override
		public void encode(ChannelHandlerContext ctx, WorldDefinitionMessage msg, ByteBuf buf) {
			final World world = msg.world();
			EncodingUtils.writeString(world.getName(), buf);
		}
		
	}
	
	public class TileMessageEncoder extends MessageEncoder<BasicTileMessage> {
		protected TileMessageEncoder() {
			super(OPCODE_CLIENT_TILE_INFORM);
		}

		@Override
		public void encode(ChannelHandlerContext ctx, BasicTileMessage msg, ByteBuf buf) {
			ClientSession session = ClientSession.get(ctx);
			
			write2DLocation(msg.bottomLeft, buf);
			
			buf.writeBoolean(msg.isGroup());
			
			if(msg.isGroup()) {
				EncodingUtils.encode2DObjectArrayNoIndex(buf, msg.tiles, ThoraServerCodec::encodeTileData);
			} else {
				encodeTileData(msg.data, buf);
			}
			
		}
		
	}
	
	public class StateChangeMessageEncoder extends MessageEncoder<StateChangeMessage> {
		protected StateChangeMessageEncoder() {
			super(OPCODE_CLIENT_STATE_CHANGE);
		}

		@Override
		public void encode(ChannelHandlerContext ctx, StateChangeMessage msg, ByteBuf buf) {
			buf.writeInt(msg.stateID);
		}
		
	}
	
	private static final void encodeTileData(TileData data, ByteBuf buf) {
		buf.writeByte(data.material().ordinal());
	}
	
}
