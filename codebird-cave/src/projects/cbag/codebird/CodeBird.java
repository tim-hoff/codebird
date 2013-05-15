package projects.cbag.codebird;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.FPSLogger;

public class CodeBird extends Game
{

	public static final String VERSION = "0.0.1.20 Incubation";
	public static final String LOG = "Code Bird";
	public static final boolean DEBUG = true;
	FPSLogger log;
	
	static GameController controller;
	
	public CodeBird()
	{
		super();
		controller = new GameController(this);
	}

	@Override
	public void create()
	{
		log = new FPSLogger();
		Audio.playMusic(true);
		controller.load_main_menu();
	}

	@Override
	public void dispose()
	{
		super.dispose();
		Audio.dispose();
	}

	@Override
	public void render()
	{
		super.render();
		log.log();
	}

	@Override
	public void resize(int width, int height)
	{
		super.resize(width, height);
	}

	@Override
	public void pause()
	{
		super.pause();
	}

	@Override
	public void resume()
	{
		super.resume();
	}
}
