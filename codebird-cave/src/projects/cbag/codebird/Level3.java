package projects.cbag.codebird;

import projects.cbag.codebird.GameController._Level;

public class Level3 extends Level
{
	@Override
	public void loadNext()
	{
		CodeBird.controller.load_level(_Level.level3);
	}

	@Override
	public void resetScreen()
	{
		CodeBird.controller.load_level(_Level.level3);
	}
	
	@Override
	String get_level()
	{
		return _Level.level2.toString();
	}
}
