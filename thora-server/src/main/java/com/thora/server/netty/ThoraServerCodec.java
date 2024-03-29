package com.thora.server.netty;

import java.io.IOException;
import java.time.Instant;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.logging.log4j.Logger;

import com.thora.core.net.message.BasicTileMessage;
import com.thora.core.net.message.CameraEntityMessage;
import com.thora.core.net.message.CameraPointMessage;
import com.thora.core.net.message.ChatMessage;
import com.thora.core.net.message.EntityMessage;
import com.thora.core.net.message.EntityMoveRequestMessage;
import com.thora.core.net.message.LoginRequestMessage;
import com.thora.core.net.message.LoginResponseMessage;
import com.thora.core.net.message.StateChangeMessage;
import com.thora.core.net.message.WorldDefinitionMessage;
import com.thora.core.net.netty.EncodingUtils;
import com.thora.core.net.netty.ThoraCodec;
import com.thora.core.world.Location;
import com.thora.core.world.TileData;
import com.thora.core.world.World;
import com.thora.core.world.WorldEntity;
import com.thora.server.ServerPlayer;
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
	protected ClientSession getSession(final ChannelHandlerContext ctx) {
		return ClientSession.get(ctx);
	}
	
	@Override
	protected void populate() {
		
		addDecoder(new LoginRequestDecoder());
		addDecoder(new ChatMessageDecoder());
		addDecoder(new EntityMoveRequestMessageDecoder());
		
		
		addEncoder(new LoginResponseEncoder());
		addEncoder(new ChatMessageEncoder());
		addEncoder(new ByteChatMessageEncoder());
		addEncoder(new WorldDefMessageEncoder());
		addEncoder(new TileMessageEncoder());
		addEncoder(new EntityMessageEncoder());
		addEncoder(new StateChangeMessageEncoder());
		addEncoder(new CameraPointMessageEncoder());
		addEncoder(new CameraEntityMessageEncoder());
		
	}
	
	public class LoginRequestDecoder extends MessageDecoder<LoginRequestMessage> {

		public LoginRequestDecoder() {
			super(ThoraCodec.OPCODE_SERVER_LOGIN_REQUEST);
		}

		@Override
		public LoginRequestMessage decode(final ChannelHandlerContext ctx, final ByteBuf encBuf) throws IOException {
			final ClientSession session = getSession(ctx);
			if(!encBuf.isReadable()) {
				return null;
			}
			try {
				EncodingUtils.decryptSameByteArrayBuf(encBuf, encBuf.readableBytes(), session.getCryptoCreds().getAssymetric().getDecryptCipher());
				final long sessionKey = encBuf.readLong();
				final long timeStamp = encBuf.readLong();
				final String username = EncodingUtils.readByteString(encBuf);
				final String password = EncodingUtils.readByteString(encBuf);
				return new LoginRequestMessage(username, password, sessionKey, timeStamp);
			} catch (IllegalBlockSizeException | BadPaddingException e) {
				throw new IOException(e);
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
			ThoraCodec.writeInstantUTC(packet.getTime(), buf);
			EncodingUtils.writeNullableVarString(packet.getSenderName(), buf);
			EncodingUtils.writeString(packet.getContent(), buf);
		}
	}
	
	public class ByteChatMessageEncoder extends EncryptedPayloadMessageEncoder<ByteChatMessage> {
		protected ByteChatMessageEncoder() {
			super(OPCODE_CLIENT_CHAT_MESSAGE);
		}
		@Override
		public void encodePlain(final ChannelHandlerContext ctx, final ByteChatMessage message, final ByteBuf buf) {
			final ByteBuf messageBuf = message.messageBuf;
			try {
				buf.writeBytes(messageBuf, 0, messageBuf.writerIndex());
			} finally {
				messageBuf.release();
			}
		}
	}
	
	public class ChatMessageDecoder extends EncryptedPayloadMessageDecoder<ChatMessage> {
		public ChatMessageDecoder() {
			super(OPCODE_SERVER_CHAT_MESSAGE);
		}
		@Override
		protected ChatMessage decodePlain(final ChannelHandlerContext ctx, final ByteBuf buf) {
			final ClientSession session = ClientSession.get(ctx);
			
			final Instant serverTime = Instant.now();
			final Instant clientTime = ThoraCodec.readInstantUTC(buf);
			final String content = EncodingUtils.readString(buf);
			
			return new ChatMessage(serverTime, session.getPlayer(), content);
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
	
	public final class EntityMessageEncoder extends MessageEncoder<EntityMessage> {
		protected EntityMessageEncoder() {
			super(OPCODE_CLIENT_ENTITY_INFORM);
		}
		
		public static final int HEADER_BIT_CREATE = 0x01;
		public static final int HEADER_BIT_UPDATE = 0x02;
		public static final int HEADER_BIT_DESTROY = 0x04;
		
		@Override
		public final void encode(ChannelHandlerContext ctx, EntityMessage msg, ByteBuf buf) {
			
			final int headerIndex = buf.writerIndex();
			byte header = 0;
			buf.writeByte(0);
			
			if(!msg.getCreate().isEmpty()) {
				header |= HEADER_BIT_CREATE;
				EncodingUtils.encodeIntMap(msg.getCreate(), this::encodeEntityCreate, buf);
			}
			
			if(!msg.getUpdate().isEmpty()) {
				header |= HEADER_BIT_UPDATE;
				EncodingUtils.encodeIntMap(msg.getUpdate(), this::encodeEntityUpdate, buf);
			}
			
			if(!msg.getDestroy().isEmpty()) {
				header |= HEADER_BIT_DESTROY;
				EncodingUtils.encodeCollection(msg.getDestroy(), EncodingUtils::writePosVarInt, buf);
			}
			
			buf.setByte(headerIndex, header);
			
		}
		
		private final void encodeEntityCreate(final int id, final WorldEntity e, final ByteBuf buf) {
			encodeEntityLocHeader(e, buf);
			buf.writeByte(e.getEntityType().ordinal());
			EncodingUtils.writeNullableVarString(e.getName(), buf);
		}
		
		private final void encodeEntityUpdate(final int id, final WorldEntity e, final ByteBuf buf) {
			encodeEntityLocHeader(e, buf);
		}
		
	}
	
	private void encodeEntityLocHeader(final WorldEntity e, final ByteBuf buf) {
		EncodingUtils.writePosVarInt(e.getID(), buf);
		ThoraCodec.write2DLocation(e, buf);
	}
	
	public class EntityMoveRequestMessageDecoder extends MessageDecoder<EntityMoveRequestMessage> {

		public EntityMoveRequestMessageDecoder() {
			super(OPCODE_CLIENT_MOVE_REQUSET);
		}

		@Override
		public EntityMoveRequestMessage decode(ChannelHandlerContext ctx, ByteBuf buf) throws IOException {
			ClientSession session = ClientSession.get(ctx);
			ServerPlayer player = session.getPlayer();
			
			final Location from = ThoraCodec.read2DLocation(player.world(), buf);
			final Location to = ThoraCodec.read2DLocation(player.world(), buf);
			
			return new EntityMoveRequestMessage(player.getEntity(), from, to);
		}
		
	}
	
	public class CameraPointMessageEncoder extends MessageEncoder<CameraPointMessage> {
		protected CameraPointMessageEncoder() {
			super(OPCODE_CLIENT_CAMERA_CHANGE);
		}
		@Override
		public void encode(ChannelHandlerContext ctx, CameraPointMessage msg, ByteBuf buf) {
			buf.writeByte(0);
			buf.writeDouble(msg.getScale());
			ThoraCodec.write2DLocation(msg.getFocus(), buf);
		}
	}
	
	public class CameraEntityMessageEncoder extends MessageEncoder<CameraEntityMessage> {
		protected CameraEntityMessageEncoder() {
			super(OPCODE_CLIENT_CAMERA_CHANGE);
		}
		@Override
		public void encode(ChannelHandlerContext ctx, CameraEntityMessage msg, ByteBuf buf) {
			buf.writeByte(1);
			buf.writeDouble(msg.getScale());
			ThoraCodec.writeEntityReference(msg.getFocus(), buf);
			buf.writeBoolean(msg.canControl());
		}
	}
	
	private static final void encodeTileData(TileData data, ByteBuf buf) {
		buf.writeByte(data.material().ordinal());
	}
	
}
