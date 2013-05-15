package projects.cbag.codebird;

import projects.cbag.codebird.GameController._Level;

public class Level1 extends Level
{

	@Override
	public void loadNext()
	{
		CodeBird.controller.load_level(_Level.level2);
	}

	@Override
	public void resetScreen()
	{
		CodeBird.controller.load_level(_Level.level1);
	}

	@Override
	String get_level()
	{
		return "level1";
	}
}
