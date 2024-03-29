package com.thora.client.state;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.util.Dimension;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.ashley.signals.Listener;
import com.badlogic.ashley.signals.Signal;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.thora.client.FlamesOfThoraClient;
import com.thora.client.graphics.MultiTextureComponent;
import com.thora.client.graphics.TextureComponent;
import com.thora.client.graphics.TransformComponent;
import com.thora.client.input.InputHandler.KeyBinding;
import com.thora.client.input.InputHandler.KeyRecord;
import com.thora.client.input.Key;
import com.thora.client.screen.ChatFormatter;
import com.thora.client.system.MoveSystem;
import com.thora.client.system.MoveValidationSystem;
import com.thora.client.world.OldWorldRendererSystem;
import com.thora.core.HasLogger;
import com.thora.core.entity.EntityType;
import com.thora.core.entity.PlayerComponent;
import com.thora.core.entity.TypeComponent;
import com.thora.core.net.message.ChatMessage;
import com.thora.core.net.message.EntityMoveRequestMessage;
import com.thora.core.world.Locatable;
import com.thora.core.world.Location;
import com.thora.core.world.LocationComponent;
import com.thora.core.world.MovableComponent;
import com.thora.core.world.MoveEvent;
import com.thora.core.world.WeakVectorLocation;

public class PlayingState extends GameState implements HasLogger {
	
	private static final Logger logger =  LogManager.getLogger(PlayingState.class);
	
	public static final double WALK_SPEED_TPS = 10f;
	public static final long WALK_TILE_DURATION = (long) (1000 / WALK_SPEED_TPS);
	
	public static final double GRID_TOGGLE_SPEED_TPS = 7.5f;
	public static final long GRID_TOGGLE_LIMIT_DURATION = (long) (8 / GRID_TOGGLE_SPEED_TPS);
	
	private static final Key KEY_ESCAPE = new Key(Keys.ESCAPE);
	private static final Key KEY_UP = new Key(Keys.UP);
	private static final Key KEY_DOWN = new Key(Keys.DOWN);
	private static final Key KEY_LEFT = new Key(Keys.LEFT);
	private static final Key KEY_RIGHT = new Key(Keys.RIGHT);
	private static final Key KEY_G = new Key(Keys.G);
	
	private OldWorldRendererSystem worldRenderer;
	
	private float delta;
	private float lastGridToggleTime;
	
	public static final Matrix4 NATIVE_MATRIX = new Matrix4();
	
	private OrthographicCamera worldCamera;
	private SpriteBatch worldBatch;
	private SpriteBatch hudBatch;
	private SpriteBatch entityBatch;
	private ShapeRenderer shapeRend;
	
	public BitmapFont font;
	Texture playerImg;
	Texture playerImgBack;
	
	TextureRegion playerImgRegion;
	TextureRegion playerImgBackRegion;
	
	private Entity player;
	private long lastWalkTime;
	
	private ChatFormatter chatFormatter = ChatFormatter.DEFAULT;
	
	/**
	 * A cached window size should be handled in Client instances.
	 */
	private Dimension appSize;
	
	public Skin skin = new Skin(Gdx.files.internal("assets/skin/uiskin.json"));
	
	protected InputMultiplexer inputMultiplex;
	protected Stage uiStage;
	
	protected Table chatTable;
	protected ScrollPane chatLineScroll;
	protected Table chatLines;
	protected TextField chatbar;
	
	
	private Signal<Dimension> resizeSignal = new Signal<>();
	
	float viewportScale = 20f;
	float scrollScaleChange = .035f;
	
	//	private final InputHandler in = new InputHandler();
	//	private final InputListener inputListener = new InputListener(in) {
	//		
	//		@Override
	//		public boolean scrolled(final int amount) {
	//			logger().trace(() -> "Mouse scroll: " + amount);
	//			if(amount != 0) {
	//				if(amount > 0) {
	//					for(int i=amount; i>0; --i) {
	//						scaleCamera(1f - scrollScaleChange);
	//					}
	//				} else {
	//					for(int i=amount; i<0; ++i) {
	//						scaleCamera(1f + scrollScaleChange);
	//					}
	//				}
	//				return true;
	//			}
	//			
	//			return false;
	//		}
	//		
	//	};
	
	public void scaleCamera(float scale) {
		viewportScale *= scale;
		this.worldCamera.setToOrtho(false, g().getWidth()/viewportScale, g().getHeight()/viewportScale);
		LocationComponent lc = LocationComponent.MAPPER.get(player);
		this.worldCamera.position.set(lc.getX() + .5f, lc.getY() + .5f, 0);
	}
	
	public PlayingState(FlamesOfThoraClient client, String name, int id) {
		super(client, name, id);
	}
	
	@Override
	public Logger logger() {
		return logger;
	}
	
	protected void handleNewChatRequest(final String text) {
		client().sendChatMessage(text);
	}
	
	public void handleNewChatMessage(final ChatMessage message) {
		
		boolean onBottom = chatLineScroll.getScrollY() == chatLineScroll.getMaxY();
		
		ChatEntry entry = new ChatEntry(this, message, skin);
		chatLines.row();
		chatLines.add(entry);
		entry.setWidth(entry.getParent().getWidth());
		chatLines.pack();
		if(onBottom) {
			chatLineScroll.setScrollY(chatLineScroll.getMaxY());
		}
		chatLineScroll.invalidate();
	}
	
	public static String markupColor(final Color color) {
		return "[#" + color.toString() + "]";
	}
	
	public static final StringBuilder markupColor(final StringBuilder b, final Color color) {
		return b.append("[#").append(color.toString()).append("]");
	}
	
	private static final DateTimeFormatter instantFormatter = DateTimeFormatter.ISO_LOCAL_TIME
			.withZone(ZoneId.systemDefault());
	
	private static class ChatEntry extends Label {
		
		final PlayingState state;
		private final ChatMessage message;
		
		public ChatEntry(final PlayingState state, final ChatMessage message, final Skin skin) {
			super(message.content, skin);
			this.state = state;
			this.message = message;
			this.setText(ChatMessage.formatEscapes(toLineString()));
		}
		
		public final ChatMessage getMessage() {
			return message;
		}
		
		public String toLineString() {
			return state.chatFormatter.format(getMessage());
		}
		
	}
	
	@Override
	public void onCreate() {
		this.appSize = new Dimension(g().getWidth(), g().getHeight());
		
		uiStage = new Stage(new ScreenViewport());
		
		Color bckColor = Color.DARK_GRAY;
		bckColor.a = .75f;
		
		chatTable = new Table();
		chatTable.setX(0, Align.bottomLeft);
		chatTable.setHeight(250f);
		chatTable.setFillParent(false);
		chatTable.setWidth(uiStage.getViewport().getScreenWidth());
		chatTable.setBackground(skin.newDrawable("white", bckColor));
		
		chatTable.defaults().align(Align.bottomLeft);
		chatTable.defaults().prefWidth(uiStage.getViewport().getScreenWidth());
		
		chatLines = new Table();
		chatLines.defaults().align(Align.bottomLeft);
		chatLines.defaults().prefWidth(chatTable.getWidth());
		chatLines.setHeight(220f);
		chatLines.setX(0, Align.bottomLeft);
		chatLines.align(Align.bottom);
		chatLines.defaults().fillX();
		
		chatTable.setWidth(uiStage.getViewport().getScreenWidth());
		
		
		chatLineScroll = new ScrollPane(chatLines, skin);
		chatLineScroll.getStyle().background = null;
		
		chatLineScroll.setFadeScrollBars(false);
		chatLineScroll.setOverscroll(false, false);
		chatLineScroll.setScrollingDisabled(true, false);
		chatLineScroll.setForceScroll(false, false);
		
		//chatTable.add(chatLines).height(180f);
		chatTable.add(chatLineScroll).height(200f);
		
		chatLines.setDebug(true, true);
		//chatLines.pack();
		
		//chatLineScroll.pack();
		
		
		chatbar = new TextField("", skin);
		bckColor.a = .45f;
		chatbar.getStyle().disabledBackground = skin.newDrawable("white", bckColor);
		chatbar.setStyle(chatbar.getStyle());
		chatbar.setX(0, Align.bottomLeft);
		//chatbar.pack();
		
		chatbar.addListener(new EventListener() {
			
			@Override
			public boolean handle(Event event) {
				if(event instanceof InputEvent) {
					final InputEvent e = (InputEvent) event;
					if(e.getType() == Type.keyTyped) {
						char c = e.getCharacter();
						if(c == '\r') {
							final String text = chatbar.getText();
							if(!text.isEmpty()) {
								handleNewChatRequest(text);
							}
							chatbar.setText("");
							chatbar.setVisible(false);
							uiStage.unfocus(chatbar);
						}
					}
				}
				return false;
			}
			
		});
		
		chatTable.row();
		chatTable.add(chatbar);
		
		uiStage.addActor(chatTable);
		
		
		chatTable.pack();
		
		this.inputMultiplex = new InputMultiplexer(uiStage, worldInutProcessor);
		Gdx.input.setInputProcessor(inputMultiplex);
		
		logger().trace("Created Playing State!");
	}
	
	protected void toggleGridView() {
		logger().debug("Last Resized at: {}", lastGridToggleTime);
		logger().debug("Toggling Grid");
		worldRenderer.toggleBorders();
		lastGridToggleTime = delta;
	}
	
	private Matrix4 uiMatrix = new Matrix4();
	private static final Color COLOR_OFF_WHITE = new Color(1f, 1f, 1f, .5f);
	
	private final Vector2 v = new Vector2();
	private final Vector2 lastWalkV = new Vector2();
	
	//Various tasks that should be completed on the render portion of the game loop.
	@Override
	public void render(float dt) {
		
		Gdx.gl.glClearColor( 0, 0, 0, 1 );
		Gdx.gl.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT );
		
		//Updates the entity system. 
		engine().update(dt);
		
		uiStage.draw();
		
		float width = g().getWidth();
		float height = g().getHeight();
		
		//worldCamera.update();
		uiMatrix.set(worldCamera.combined);
		uiMatrix.setToOrtho2D(0, 0, width, height);
		
		//batch.setTransformMatrix(hudCamera.combined);
		hudBatch.setProjectionMatrix(uiMatrix);
		hudBatch.begin();
		
		Location loc = player.getComponent(LocationComponent.class).getLocation();
		
		/*
		 * FPS COUNTER
		 */
		font.setColor(Color.RED);
		String msg = String.format("FPS: %s\t(%s,%s)\n%s", g().getFramesPerSecond(), Gdx.input.getX(), Gdx.input.getY(), loc);
		font.draw(hudBatch, msg, 0, height);
		
		
		hudBatch.end();
		
		//		shapeRend.setProjectionMatrix(uiMatrix);
		//		shapeRend.begin(ShapeRenderer.ShapeType.Line);
		//		Gdx.gl.glEnable(GL11.GL_BLEND);
		//		shapeRend.setColor(COLOR_OFF_WHITE);
		//		shapeRend.line(width/2, 0, width/2, height);
		//		shapeRend.line(0, height/2, width, height/2);
		//		Gdx.gl.glLineWidth(1f);
		//		shapeRend.end();
		
		//update(dt);
	}
	
	//Various tasks that should be completed on the update portion of the game loop.
	@Override
	public void update(float dt) {
		
		//Update the Delta.
		updateLocalDelta(dt);
		
		//Handle Key Events for Keyboard Keys registered in this state.
		handleInput();
		
		engine().update(dt);
		
		uiStage.act(dt);
		
		//Update the camera.
		//worldCamera.update();
		
	}
	
	@Override
	public void onPause() {
		
	}
	
	@Override
	public void onResume() {
		
	}
	
	@Override
	public void onResize(int width, int height) {
		logger().debug("ON_RESIZE: [{},{}] -> [{},{}]", appSize.getWidth(), appSize.getHeight(), width, height);
		worldCamera.update();
		appSize.setSize(width, height);
		
		resizeSignal.dispatch(appSize);
		//worldCamera.setToOrtho(false, g().getWidth()/viewportScale, g().getHeight()/viewportScale);
		uiStage.getViewport().update(width, height, true);
		chatbar.setWidth(width);
		chatTable.setWidth(width);
		worldCamera.update();
		//		hudBatch.dispose();
		//		worldBatch.dispose();
		//		font.dispose();
		//		hudBatch = new SpriteBatch();
		//		worldBatch = new SpriteBatch();
		//		font = new BitmapFont();
	}
	
	@Override
	public void enter() {
		hudBatch = new SpriteBatch();
		worldBatch = new SpriteBatch();
		shapeRend = new ShapeRenderer();
		font = new BitmapFont();
		
		//Player Images
		playerImg = new Texture("assets/player.png");
		playerImgBack = new Texture("assets/playerbackdetails.png");
		playerImgRegion = new TextureRegion(playerImg);
		playerImgBackRegion = new TextureRegion(playerImgBack);
		
		//		Gdx.input.setInputProcessor(inputListener);
		
		//		in.bindKey(EXIT_BINDING, Keys.ESCAPE);
		//		in.bindKey(SHOW_GRID_BIDING, Keys.G);
		
		//inputHandler.RegisterKey(KEY_ESCAPE);
		//		in.RegisterKey(KEY_UP);
		//		in.RegisterKey(KEY_DOWN);
		//		in.RegisterKey(KEY_LEFT);
		//		in.RegisterKey(KEY_RIGHT);
		//		in.RegisterKey(KEY_G);
		
		
		Location spawn = new WeakVectorLocation<>(client().world(), 0, 0);
		player = createPlayerEntity(engine(), spawn);
		engine().addEntity(player);
		
		
		worldCamera = new OrthographicCamera(g().getWidth()/viewportScale, g().getHeight()/viewportScale);
		worldCamera.position.set(spawn.getX(), spawn.getY(), 0);
		
		
		worldRenderer = new OldWorldRendererSystem(client(), worldBatch, client().world(), worldCamera, player.getComponent(LocationComponent.class),
				resizeSignal, 100);
		
		engine().addSystem(worldRenderer);
		
		
		
		engine().addSystem(new MoveValidationSystem(10));
		
		engine().addSystem(new MoveSystem(20));
		
		Gdx.input.setInputProcessor(inputMultiplex);
		
		uiStage.addCaptureListener(new EventListener() {
			@Override
			public boolean handle(Event event) {
				if(event instanceof InputEvent) {
					InputEvent e = (InputEvent) event;
					if(Type.touchDown == e.getType()) {
						if(!(event.getTarget() instanceof TextField)) {
							uiStage.setKeyboardFocus(null);
						}
						if(!(event.getTarget() instanceof ScrollPane)) {
							uiStage.setScrollFocus(null);
						}
					}
				}
				
				return false;
			}
		});
		
		client().newMessageSignal.add(new Listener<ChatMessage>() {
			@Override
			public void receive(Signal<ChatMessage> signal, ChatMessage message) {
				handleNewChatMessage(message);
			}
		});
		
		for(ChatMessage message: client().chatMessages) {
			handleNewChatMessage(message);
		}
		
	}
	
	private Entity createPlayerEntity(PooledEngine engine, Locatable loc) {
		return createPlayerEntity(engine, loc.getLocation().getX(), loc.getLocation().getY());
	}
	
	private Entity createPlayerEntity(PooledEngine engine, int x, int y) {
		Entity entity = engine.createEntity();
		
		PlayerComponent player = engine.createComponent(PlayerComponent.class);
		
		TypeComponent type = engine.createComponent(TypeComponent.class).set(EntityType.HUMAN_MALE);
		LocationComponent location = engine.createComponent(LocationComponent.class).setLocation(new WeakVectorLocation<>(client().world(), x, y));
		MovableComponent movable = engine.createComponent(MovableComponent.class);
		
		movable.signal.add(new Listener<MoveEvent>() {
			@Override
			public void receive(Signal<MoveEvent> signal, MoveEvent event) {
				final Location oldLoc = location.getLocation().clone();
				location.getLocation().shift(event.dx(), event.dy());
				
				worldCamera.position.add(event.dx(), event.dy(), 0);
				worldCamera.update();
				
				EntityMoveRequestMessage packet = new EntityMoveRequestMessage(null, oldLoc, location.getLocation().clone());
				client().network().session().writeAndFlush(packet);
				
			}
		});
		
		TextureComponent fronttexture = engine.createComponent(TextureComponent.class).set(playerImgRegion);
		TextureComponent backTexture = engine.createComponent(TextureComponent.class).set(playerImgBackRegion);
		
		MultiTextureComponent textures = new MultiTextureComponent();
		
		textures.addTextureComponent(fronttexture, 0);
		textures.addTextureComponent(backTexture, 1);	
		
		
		TransformComponent transform = engine.createComponent(TransformComponent.class);
		
		entity.add(player)
		.add(type)
		.add(location)
		.add(movable)
		.add(fronttexture)
		.add(textures)
		.add(transform);
		
		return entity;
	}
	
	@Override
	public void exit() {
		//Gdx.input.setInputProcessor(null);
		engine().removeSystem(worldRenderer);
		engine().removeAllEntities();
		worldBatch.dispose();
		hudBatch.dispose();
		shapeRend.dispose();
		font.dispose();
	}
	
	public SpriteBatch getEntityBatch() {
		return entityBatch;
	}
	
	public void setEntityBatch(SpriteBatch entityBatch) {
		this.entityBatch = entityBatch;
	}
	
	private KeyBinding EXIT_BINDING = new KeyBinding() {
		@Override
		public boolean onPress(KeyRecord key) {
			Gdx.app.exit();
			return true;
		}
	};
	
	private KeyBinding SHOW_GRID_BIDING = new KeyBinding() {
		@Override
		public boolean onRelease(KeyRecord key) {
			logger().debug("Last Resized at: {}", lastGridToggleTime);
			logger().debug("Toggling Grid");
			worldRenderer.toggleBorders();
			lastGridToggleTime = delta;
			return true;
		}
	};
	
	private final InputProcessor worldInutProcessor = new InputAdapter() {
		
		@Override
		public boolean keyTyped(final char character) {
			boolean handled = true;
			
			switch(character) {
			
			case '\r':
				chatbar.setVisible(true);
				uiStage.setKeyboardFocus(chatbar);
				break;
				
			case '/':
				chatbar.setVisible(true);
				if(uiStage.setKeyboardFocus(chatbar)) {
					chatbar.appendText("/");
				}
				break;
				
			default:
				handled = false;
				break;
				
			}
			
			return handled;
		}
		
		@Override
		public boolean keyDown(final int keycode) {
			boolean handled = true;
			
			switch(keycode) {
			
			case Keys.UP:
				walk(0, 1);
				break;
			case Keys.DOWN:
				walk(0, -1);
				break;
			case Keys.LEFT:
				walk(-1, 0);
				break;
			case Keys.RIGHT:
				walk(1, 0);
				break;
				
			default:
				handled = false;
				break;
				
			}
			
			return handled;
		}
		
		@Override
		public boolean keyUp(final int keycode) {
			boolean handled = true;
			
			switch(keycode) {
			
			case Keys.UP:
				walk(0, -1);
				break;
			case Keys.DOWN:
				walk(0, 1);
				break;
			case Keys.LEFT:
				walk(1, 0);
				break;
			case Keys.RIGHT:
				walk(-1, 0);
				break;
				
			case Keys.CONTROL_LEFT:
				uiStage.dispose();
				onCreate();
				break;
				
			case Keys.ESCAPE:
				client().dispose();
				break;
				
			default:
				handled = false;
				break;
				
			}
			
			return handled;
		}
		
		@Override
		public boolean mouseMoved(final int screenX, final int screenY) {
			// TODO Auto-generated method stub
			return super.mouseMoved(screenX, screenY);
		}
		
		@Override
		public boolean scrolled(final int amount) {
			logger().trace("Mouse scroll: {}", amount);
			if(amount != 0) {
				if(amount > 0) {
					for(int i=amount; i>0; --i) {
						scaleCamera(1f - scrollScaleChange);
					}
				} else {
					for(int i=amount; i<0; ++i) {
						scaleCamera(1f + scrollScaleChange);
					}
				}
				return true;
			}
			
			return false;
		}
		
	};
	
	private void handleInput() {
		
		//TODO Instead of polling input every frame, have a State specific InputProcesser implement input logic.
		
		
		if(Gdx.input.isKeyJustPressed(Keys.G)) {
			toggleGridView();
		}
		
		
		Location loc = player.getComponent(LocationComponent.class).getLocation();
		
		long time = System.currentTimeMillis();
		if(time > lastWalkTime + WALK_TILE_DURATION) {
			
			//			if(in.isKeyDown(Keys.UP)) {
			//				walk(0, 1);
			//			}
			//			if(in.isKeyDown(Keys.DOWN)) {
			//				walk(0, -1);
			//			}
			//			if(in.isKeyDown(Keys.LEFT)) {
			//				walk(-1, 0);
			//			}
			//			if(in.isKeyDown(Keys.RIGHT)) {
			//				walk(1, 0);
			//			}
			
			//			if(KEY_UP.ifPressed()) {
			//				walk(0, 1);
			//			}
			//			
			//			if(KEY_DOWN.ifPressed()) {
			//				walk(0, -1);
			//			}
			//			
			//			if(KEY_LEFT.ifPressed()) {
			//				walk(-1, 0);
			//			}
			//			
			//			if(KEY_RIGHT.ifPressed()) {
			//				walk(1, 0);
			//			}
			
			if(!v.isZero()) {
				final LocationComponent locComp = player.getComponent(LocationComponent.class);
				final Location oldLoc = locComp.getLocation().clone();
				
				final float yDir = Math.signum(v.y);
				if(yDir != Math.signum(lastWalkV.y)) {
					if(yDir > 0) {
						player.getComponent(MultiTextureComponent.class).setActiveComponent(1);
					} else if(yDir < 0) {
						player.getComponent(MultiTextureComponent.class).setActiveComponent(0);
					}
				}
				lastWalkV.set(v);
				
				locComp.setLocation(locComp.getLocation().shift((int)v.x, (int)v.y));
				
				final Location newLoc = locComp.getLocation().clone();
				client().network().session().writeAndFlush(new EntityMoveRequestMessage(null, oldLoc, newLoc));
				
				//loc.shift((int)v.x, (int)v.y);
				//worldCamera.position.add(v.x, v.y, 0);
				//v.setZero();
				lastWalkTime = time;
				//worldCamera.update();
			}
		}
		
	}
	
	protected void walk(int dx, int dy) {
		v.add(dx, dy);
	}
	
	/* Updates the local delta time. 
	 *(The time since the playing state was created and the first update was ran)
	 */
	private void updateLocalDelta(float dt) {
		//delta += Gdx.app.getGraphics().getDeltaTime();
		delta = dt;
	}
	
	@Override
	public void setName(String name) {
		
		//Sets the state name to Playing State(Hard Coded for utility purposes)
		
		this.setName("Playing State");
		
	}
	
	
}
