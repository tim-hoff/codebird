package projects.cbag.codebird;

import projects.cbag.codebird.GameController.Avatar;
import projects.cbag.codebird.GameController._Level;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Align;

public class Choice implements Screen {
	Stage stage;
	BitmapFont black;
	BitmapFont white;
	TextureAtlas atlas;
	Skin skin;
	SpriteBatch batch;

	TextButton button;
	TextButton button1;


	Label label;
	Label label1;
	Label label2;

	public void startGame(){
		CodeBird.controller.load_level(_Level.level1);
	}
	
	@Override
	public void render(float delta) {

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		stage.act(delta);

		batch.begin();
		stage.draw();
		batch.end();
		if (Gdx.input.isKeyPressed(Keys.NUM_1)){
			CodeBird.controller.avatar = Avatar.Mockingjay;
			startGame();
		}
		if (Gdx.input.isKeyPressed(Keys.NUM_2)){
			CodeBird.controller.avatar = Avatar.Raven;
			startGame();
		}
	}

	@Override
	public void resize(int width, int height) {
		if (stage == null)
			stage = new Stage(width, height, true);
		stage.clear();

		Gdx.input.setInputProcessor(stage);

		// setting the text button styles
		TextButtonStyle style = new TextButtonStyle();
		style.up = skin.getDrawable("buttonnormal");
		style.down = skin.getDrawable("buttonpressed");
		style.font = black;

		button = new TextButton("Mockingjay", style);
		button.setSize(200, 100);
		button.setPosition(Gdx.graphics.getWidth() / 2 - button.getWidth() / 2,
				Gdx.graphics.getHeight() / 2 - button.getHeight() / 2);
		button.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				return true;
			}

			public void touchUp(InputEvent event, float x, float y,
					int pointer, int button) {
				CodeBird.controller.avatar = Avatar.Mockingjay;
				startGame();
			}
		});

		button1 = new TextButton("Raven", style);
		button1.setSize(200, 100);
		button1.setPosition(button.getX() + button1.getWidth(), button.getY());
		button1.addListener(new InputListener() {
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				return true;
			}

			public void touchUp(InputEvent event, float x, float y,
					int pointer, int button) {
				CodeBird.controller.avatar = Avatar.Raven;
				startGame();
			}
		});

		// label work (top lable)
		LabelStyle ls = new LabelStyle(white, Color.WHITE);
		label = new Label("Pick your player", ls);
		label.setX(0);
		//label.setY(Gdx.graphics.getHeight() / 2 + 100);
		label.setY(Gdx.graphics.getHeight() / 2 + 150);
		label.setWidth(width);
		label.setAlignment(Align.center);
		// lets add another one (under button)

		// add stuff to the stage
		stage.addActor(button);
		stage.addActor(button1);
		stage.addActor(label);

	}

	@Override
	public void show() {
		batch = new SpriteBatch();
		atlas = new TextureAtlas("data/button.pack");
		skin = new Skin();
		skin.addRegions(atlas);
		white = new BitmapFont(Gdx.files.internal("data/whitefont.fnt"), false);
		black = new BitmapFont(Gdx.files.internal("data/font.fnt"), false);
	}

	@Override
	public void hide() {
		dispose();

	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {
		batch.dispose();
		skin.dispose();
		atlas.dispose();
		white.dispose();
		black.dispose();
		stage.dispose();
	}

}
