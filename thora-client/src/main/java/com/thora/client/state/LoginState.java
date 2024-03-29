package com.thora.client.state;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.thora.client.FlamesOfThoraClient;
import com.thora.client.input.InputHandler;
import com.thora.client.input.InputListener;
import com.thora.core.FlamesOfThora;
import com.thora.core.net.LoginTransaction;
import com.thora.core.net.netty.EncodingUtils;

import io.netty.util.concurrent.Future;

public class LoginState extends GameState {

	//Some Constants about the Login Window
	private static final int MINIMUMLOGINWINDOWWIDTH = 300;
	private static final int MINIMUMLOGINWINDOWHEIGHT = 250;
	
	protected String defaultAddress;
	
	//Input
	InputHandler inputHandler = new InputHandler();
	InputListener inputListener = new InputListener(inputHandler);

	//Stage for UI Objects
	private Stage uiStage;

	//Skin for resources
	private Skin skin = new Skin(Gdx.files.internal("assets/skin/uiskin.json"));

	//Table for UI 
	Table loginscreenuiTable;
	
	//Table for the background
	private Table loginscreenbackgroundContainer;
	
	protected String usernameEntry;
	protected String passwordEntry;
	
	//Constructor
	public LoginState(FlamesOfThoraClient client, String name, int id, String defaultAddress) {
		super(client, name, id);
		this.defaultAddress = defaultAddress;
		this.addressField.setText(defaultAddress);
	}
	
	public LoginState(FlamesOfThoraClient client, String name, int id) {
		this(client, name, id, FlamesOfThora.defaultAddress);
	}
	
	//Debug Logger
	private static final Logger logger =  LogManager.getLogger(MenuState.class);

	@Override
	public final Logger logger() {
		return logger;
	}

	@Override
	protected void update(float dt) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void render(float dt) {

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		//Render the UI
		uiStage.act(dt);
		uiStage.draw();

	}
	
	final Label responseLabel = new Label("", skin);
	
	//User Name Text Field
	final TextField addressField = new TextField("", skin);
	
	//User Name Text Field
	final TextField usernameField = new TextField("", skin);

	//Password text Field
	final TextField passwordField = new TextField("", skin);
	
	private InetSocketAddress getAddress() {
		return EncodingUtils.parseSocketAddress(addressField.getText());
	}
	
	protected void setResponseText(String response) {
		this.responseLabel.setText(response);
	}
	
	private TextFieldStyle goodAddressStyle;
	private TextFieldStyle badAddressStyle;
	
	@Override
	public void onCreate() {

		try {
			
			loginscreenbackgroundContainer = new Table();
			loginscreenbackgroundContainer.setPosition((float) (Gdx.graphics.getWidth() * 0.5 - MINIMUMLOGINWINDOWWIDTH * 0.5), (float) (Gdx.graphics.getHeight() * 0.5 - MINIMUMLOGINWINDOWHEIGHT * 0.5));
			loginscreenbackgroundContainer.setWidth(MINIMUMLOGINWINDOWWIDTH);
			loginscreenbackgroundContainer.setHeight(MINIMUMLOGINWINDOWHEIGHT);
			
			loginscreenuiTable = new Table();
			loginscreenuiTable.setPosition((float) (Gdx.graphics.getWidth() * 0.5 - MINIMUMLOGINWINDOWWIDTH * 0.5), (float) (Gdx.graphics.getHeight() * 0.5 - MINIMUMLOGINWINDOWHEIGHT * 0.5));
			loginscreenuiTable.setWidth(MINIMUMLOGINWINDOWWIDTH);
			loginscreenuiTable.setHeight(MINIMUMLOGINWINDOWHEIGHT);

			// Create the UI Stage
			uiStage = new Stage(new ScreenViewport());
			Gdx.input.setInputProcessor(uiStage);

			//Create the background
			Image loginscreenBackground = new Image(new Texture("assets/LoginScreenBackground.png"));
			skin.add("default", loginscreenBackground);
			loginscreenbackgroundContainer.add(loginscreenBackground).expand();

			// Generate a 1x1 white texture and store it in the skin named "white".
			Pixmap pixmap = new Pixmap(1, 1, Format.RGBA8888);
			pixmap.setColor(Color.WHITE);
			pixmap.fill();

			skin.add("white", new Texture(pixmap));
			skin.add("default", new BitmapFont());

			// Configure a TextButtonStyle and name it "default". Skin resources are stored by type, so this doesn't overwrite the font.
			TextButtonStyle textButtonStyle = new TextButtonStyle();
			textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
			textButtonStyle.down = skin.newDrawable("white", Color.DARK_GRAY);
			textButtonStyle.checked = skin.newDrawable("white", Color.BLUE);
			textButtonStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY);
			textButtonStyle.font = skin.getFont("default");
			skin.add("default", textButtonStyle);
			
			goodAddressStyle = addressField.getStyle();
			
			badAddressStyle = new TextFieldStyle(goodAddressStyle);
			badAddressStyle.fontColor.set(Color.RED);
	
	//======================Create the UI========================
			
			final Label addressLabel = new Label("Address: ", skin);
			
			addressField.addListener(new EventListener() {
				@Override
				public boolean handle(Event event) {
					if(event instanceof InputEvent) {
						if(getAddress() == null) {
							addressField.setStyle(badAddressStyle);
						} else {
							addressField.setStyle(goodAddressStyle);
						}
					}
					return false;
				}
			});
			
			if(getAddress() == null) {
				addressField.setStyle(badAddressStyle);
			} else {
				addressField.setStyle(goodAddressStyle);
			}
			
			//Username Label
			final Label usernameLabel = new Label("Username: ", skin);
			usernameLabel.setColor(Color.WHITE);

			//Password Label
			final Label passwordLabel = new Label("Password: ", skin);
			
			passwordField.setPasswordCharacter('*');
			passwordField.setPasswordMode(true);
			
			
			//Show Password CheckBox
			final CheckBox showpasswordCheckBox = new CheckBox("Show Password", skin);
			
			//Login Button
			final TextButton loginbutton = new TextButton("Login", skin);

			//Exit button
			final TextButton exitbutton = new TextButton("Exit", skin);
			
			responseLabel.setWidth(loginscreenuiTable.getWidth());
			responseLabel.setWrap(true);
			
			loginscreenuiTable.add(responseLabel);
			loginscreenuiTable.row();
			
			loginscreenuiTable.add(addressLabel);
			loginscreenuiTable.add(addressField).pad(5);
			loginscreenuiTable.row();
			
			loginscreenuiTable.add(usernameLabel).pad(5);
			loginscreenuiTable.add(usernameField);
			loginscreenuiTable.row();
			
			loginscreenuiTable.add(passwordLabel).pad(5);
			loginscreenuiTable.add(passwordField);
			loginscreenuiTable.row();
			
			loginscreenuiTable.add();
			loginscreenuiTable.add(showpasswordCheckBox);
			loginscreenuiTable.row();
			
			loginscreenuiTable.add(loginbutton);
			loginscreenuiTable.add(exitbutton);

			uiStage.addActor(loginscreenbackgroundContainer);
			uiStage.addActor(loginscreenuiTable);
			
	//===================Event Handlers========================

			showpasswordCheckBox.addListener(new ChangeListener() {
				@Override
				public void changed (ChangeEvent event, Actor actor) {
					logger().trace("showPasswordCheckBox event fired = {}    on {}", event, actor);
					passwordField.setPasswordMode(!passwordField.isPasswordMode());
				}
			});
			
			loginbutton.addListener(new ChangeListener() {
				@Override
				public void changed (ChangeEvent event, Actor actor) {
					
					usernameEntry = usernameField.getText();
					passwordEntry = passwordField.getText();
					
					logger().debug("Attempting to log in as {}", usernameEntry);
					
					//NettyNetworkManager net = new NettyNetworkManager(1, client().getServerIdentity());
					
					final InetSocketAddress address = getAddress();
					
					setResponseText("Connecting to " + address);
					
					client().network().connectAndLogin(address, usernameField.getText(), passwordField.getText())
					.addListener((Future<LoginTransaction> f) -> {
						if(f.isSuccess()) {
							LoginTransaction result = f.get();
							setResponseText(result.response.getReason());
							if(!result.response.isAccepted()) {
								
							}
							//this.setFinished(true);
						} else {
							setResponseText("Failed due to " + f.cause());
							logger().atWarn().withThrowable(f.cause()).log("Could not login due to exception!");
							//Gdx.app.exit();
						}
					});
					
					//exit();
				}
			});

			//Add an event to the exit button
			exitbutton.addListener(new ChangeListener() {
				@Override
				public void changed (ChangeEvent event, Actor actor) {
					System.out.println("Exiting! : " + exitbutton.isChecked());
					loginbutton.setText("...");
					Gdx.app.exit();
				}
			});

			//Success
			logger().trace("Created Login State!");

		} catch(Exception e) {

			//Failure
			logger().trace("Failed to properly create Login State!");

		}
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onResize(int width, int height) {

		//Update the UI Viewport
		uiStage.getViewport().update(width, height, true);
		loginscreenuiTable.setPosition((float) (width * 0.5 - MINIMUMLOGINWINDOWWIDTH * 0.5), (float) (height * 0.5 - MINIMUMLOGINWINDOWHEIGHT * 0.5));
		loginscreenuiTable.setWidth(MINIMUMLOGINWINDOWWIDTH);
		loginscreenuiTable.setHeight(MINIMUMLOGINWINDOWHEIGHT);
		
		loginscreenbackgroundContainer.setPosition((float) (Gdx.graphics.getWidth() * 0.5 - MINIMUMLOGINWINDOWWIDTH * 0.5), (float) (Gdx.graphics.getHeight() * 0.5 - MINIMUMLOGINWINDOWHEIGHT * 0.5));
		loginscreenbackgroundContainer.setWidth(MINIMUMLOGINWINDOWWIDTH);
		loginscreenbackgroundContainer.setHeight(MINIMUMLOGINWINDOWHEIGHT);
	}

	@Override
	public void enter() {

		//Entered Login State. This should only happen initially or upon logout.
		Gdx.input.setInputProcessor(uiStage);

	}

	@Override
	public void exit() {

		//Delete the UI Stage

		skin.dispose();
		this.setFinished(true);
	}

}
