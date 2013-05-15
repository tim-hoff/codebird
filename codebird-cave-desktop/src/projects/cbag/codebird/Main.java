package projects.cbag.codebird;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class Main {
	public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "codebird-cave";
		cfg.useGL20 = true;
		//cfg.width = 480;
		//cfg.height = 320;
		cfg.width = 1080;
		cfg.height = 720;
		
		new LwjglApplication(new CodeBird(), cfg);
	}
}
