package projects.cbag.codebird;

public class GameController
{
	CodeBird codeBird;
	Avatar avatar;

	public GameController(CodeBird _codeBird)
	{
		codeBird = _codeBird;
		avatar = Avatar.Raven;
	}
	
	public void load_level(_Level l)
	{
		switch (l)
		{
		case level1:
			codeBird.setScreen(new Level1());
			break;
		case level2:
			codeBird.setScreen(new Level2());
			break;
		case level3:
			codeBird.setScreen(new Level3());
			break;
		default:
			break;
		}
	}
	
	public void load_main_menu()
	{
		codeBird.setScreen(new MainMenu());
	}
	
	public void load_game_menu()
	{
		codeBird.setScreen(new Choice());
	}
	
	public String get_avatar_path()
	{
		if (avatar == Avatar.Mockingjay)
			return "data/tiled/avat/redbird.png";
		else if (avatar == Avatar.Raven)
			return "data/tiled/avat/bird.png";
		return "data/redbird.png";
	}
	
	enum _Level {
		level1, level2, level3
	}
	
	enum Avatar
	{
		Raven, Vulture, Magpie, Falcon, Mockingjay
	}
}
