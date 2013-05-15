package projects.cbag.codebird;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

public class Audio {
	private Audio() {
	}

	public static Music song = Gdx.audio.newMusic(Gdx.files
			.internal("data/determination.mp3"));

	public static void playMusic(boolean looping) {
		song.setLooping(looping);
		song.play();
	}

	public static void stopMusic() {
		song.stop();
	}

	public static void dispose() {
		song.dispose();
	}
}
