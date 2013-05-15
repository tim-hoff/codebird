package projects.cbag.codebird;


import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class npc1 {
	static float WIDTH;
	static float HEIGHT;
	static float ACCEL = 20f;
	static float JUMP_VEL = 40f;
	static float GRAV = 20.0f;
	static float MAX_VEL = 8f;
	static float DAMP = 0.87f;

	enum State {
		Stand, Walk, Jump
	}

	Vector2 pos = new Vector2();
	Vector2 posL = new Vector2();
	Vector2 vel = new Vector2();
	State state = State.Stand;
	public Rectangle bounds = new Rectangle();
	static boolean grounded = false;
	boolean facesRight = true;
	public static float stateTime = 0;
}
