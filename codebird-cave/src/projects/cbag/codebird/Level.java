package projects.cbag.codebird;

import projects.cbag.codebird.bird.State;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public abstract class Level implements Screen
{
	private TiledMap map;
	private OrthogonalTiledMapRenderer renderer;
	private OrthographicCamera camera;
	private Texture birdTexture;
	private Animation stand, walk, jump;
	private bird bird;
	public Enemy enemy;
	float maxPos;
	float minPos = 0;
	private Pool<Rectangle> rectPool = new Pool<Rectangle>()
	{
		@Override
		protected Rectangle newObject()
		{
			return new Rectangle();
		}
	};
	private Array<Rectangle> tiles = new Array<Rectangle>();
	private Array<Rectangle> exits = new Array<Rectangle>();
	private static final float GRAVITY = -1.5f;

	abstract String get_level();

	abstract public void loadNext();

	abstract public void resetScreen();

	@Override
	public void render(float delta)
	{
		Gdx.gl.glClearColor(0.7f, 0.7f, 1.0f, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		float deltaTime = Gdx.graphics.getDeltaTime();
		updateBird(deltaTime);
		// camera.position.y = bird.pos.y;
		if (bird.pos.x <= maxPos - 15f)
		{
			camera.position.x = bird.posL.x;
			camera.update();
		} else
		{
			bird.posL.x = maxPos - 15f;
		}

		renderer.setView(camera);
		renderer.render();
		renderBird(deltaTime);
	}

	@Override
	public void resize(int width, int height)
	{

	}

	@Override
	public void show()
	{
		birdTexture = new Texture(CodeBird.controller.get_avatar_path());

		TextureRegion[] regions = TextureRegion.split(birdTexture, 16, 16)[0];
		stand = new Animation(0, regions[0]);
		jump = new Animation(0, regions[3]);
		walk = new Animation(0.15f, regions[2], regions[1], regions[4]);
		walk.setPlayMode(Animation.LOOP_PINGPONG);
		projects.cbag.codebird.bird.WIDTH = 1 / 16f * regions[0]
				.getRegionWidth();  
		projects.cbag.codebird.bird.HEIGHT = 1 / 16f * regions[0]
				.getRegionHeight();

		map = new TmxMapLoader().load("data/tiled/code-bird/" + get_level()	+ ".tmx");
		renderer = new OrthogonalTiledMapRenderer(map, 1 / 16f);

		camera = new OrthographicCamera();
		camera.setToOrtho(false, 30, 20);
		camera.update();

		bird = new bird();
		bird.pos.set(20, 20);
		bird.posL.set(20, 20);
		minPos = (bird.posL.x - 15f);
		maxPos = 212;

		enemy = new Enemy();
		enemy.pos.set(20, 20);
		enemy.posL.set(20, 20);
		
		renderEnemies();
	}

	@Override
	public void hide()
	{
	}

	@Override
	public void pause()
	{
	}

	@Override
	public void resume()
	{
	}

	@Override
	public void dispose()
	{
	}

	private void updateBird(float deltaTime)
	{
		// TODO Auto-generated method stub
		if (deltaTime == 0)
			return;
		bird.stateTime += deltaTime;
		if ((Gdx.input.isKeyPressed(Keys.ESCAPE)))
		{
			resetScreen();
		}
		if ((Gdx.input.isKeyPressed(Keys.P)))
		{
			bird.pos.set(180, 20);
			bird.posL.set(180, 20);
		}
		if ((Gdx.input.isKeyPressed(Keys.SPACE) || isTouched(0.75f, 1))
				&& bird.grounded)
		{
			bird.vel.y += projects.cbag.codebird.bird.FLY_VEL;
			bird.state = State.Jump;
			bird.grounded = false;
		}

		if (Gdx.input.isKeyPressed(Keys.LEFT) || Gdx.input.isKeyPressed(Keys.A)
				|| isTouched(0, 0.25f))
		{
			bird.vel.x = -projects.cbag.codebird.bird.MAX_VEL;
			if (bird.grounded)
				bird.state = State.Walk;
			bird.facesRight = false;

		}

		if (Gdx.input.isKeyPressed(Keys.RIGHT)
				|| Gdx.input.isKeyPressed(Keys.D) || isTouched(0.25f, 0.5f))
		{
			bird.vel.x = projects.cbag.codebird.bird.MAX_VEL;
			if (bird.grounded)
				bird.state = State.Walk;
			bird.facesRight = true;
		}
		bird.vel.add(0, GRAVITY);
		if (Math.abs(bird.vel.x) > projects.cbag.codebird.bird.MAX_VEL)
		{
			bird.vel.x = Math.signum(bird.vel.x)
					* projects.cbag.codebird.bird.MAX_VEL;
		}
		if (Math.abs(bird.vel.x) < 1)
		{
			bird.vel.x = 0;
			if (bird.grounded)
				bird.state = State.Stand;
		}
		bird.vel.scl(deltaTime);

		Rectangle birdRect = rectPool.obtain();
		birdRect.set(bird.pos.x, bird.pos.y, projects.cbag.codebird.bird.WIDTH,
				projects.cbag.codebird.bird.HEIGHT);

		int startX, startY, endX, endY;

		if (bird.vel.x > 0)
		{
			startX = endX = (int) (bird.pos.x
					+ projects.cbag.codebird.bird.WIDTH + bird.vel.x);

		} else
		{
			startX = endX = (int) (bird.pos.x + bird.vel.x);
		}
		if (bird.pos.x < minPos)
		{
			bird.pos.x = minPos;
		}
		if (bird.pos.x >= (maxPos - 1))
		{
			bird.pos.x = maxPos - 1;
		}

		startY = (int) (bird.pos.y);
		endY = (int) (bird.pos.y + projects.cbag.codebird.bird.HEIGHT);

		getTiles(startX, startY, endX, endY, tiles);
		getExits(startX, startY, endX, endY, exits);

		birdRect.x += bird.vel.x;
		if (bird.pos.x > bird.posL.x)
		{
			bird.posL.x = bird.pos.x;

		}
		if (bird.posL.x < (maxPos))
		{
			minPos = (bird.posL.x - 15f);
		}

		for (Rectangle tile : tiles)
		{
			if (birdRect.overlaps(tile))
			{
				bird.vel.x = 0;
				break;
			}
		}
		birdRect.x = bird.pos.x;

		if (bird.vel.y > 0)
		{
			startY = endY = (int) (bird.pos.y
					+ projects.cbag.codebird.bird.HEIGHT + bird.vel.y);
		} else
		{
			startY = endY = (int) (bird.pos.y + bird.vel.y);
		}
		startX = (int) (bird.pos.x);
		endX = (int) (bird.pos.x + projects.cbag.codebird.bird.WIDTH);

		getTiles(startX, startY, endX, endY, tiles);
		getExits(startX, startY, endX, endY, exits);
		birdRect.y += bird.vel.y;
		for (Rectangle tile : tiles)
		{
			// Hitting Block from under kills it.
			if (birdRect.overlaps(tile))
			{
				if (bird.vel.y > 0)
				{
					bird.pos.y = tile.y - projects.cbag.codebird.bird.HEIGHT;
					TiledMapTileLayer layerB = (TiledMapTileLayer) map
							.getLayers().get(2);

					layerB.setCell((int) tile.x, (int) tile.y, null);

				} else
				{
					bird.pos.y = tile.y + tile.height;
					bird.grounded = true;
				}
				bird.vel.y = 0;
				break;
			}
		}
		for (Rectangle exit : exits)
		{
			if (birdRect.overlaps(exit))
			{
				loadNext();
			}
		}
		TiledMapTileLayer door = (TiledMapTileLayer) map.getLayers().get(4);
		rectPool.free(birdRect);
		bird.pos.add(bird.vel);
		bird.vel.scl(1 / deltaTime);
		bird.vel.x *= projects.cbag.codebird.bird.DAMP;
		//
	}

	private boolean isTouched(float startX, float endX)
	{

		for (int i = 0; i < 2; i++)
		{
			float x = Gdx.input.getX() / (float) Gdx.graphics.getWidth();
			if (Gdx.input.isTouched(i) && (x >= startX && x <= endX))
			{
				return true;
			}
		}
		return false;
	}

	private void getTiles(int startX, int startY, int endX, int endY,
			Array<Rectangle> tiles)
	{
		TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get(1);
		TiledMapTileLayer layerB = (TiledMapTileLayer) map.getLayers().get(2);
		rectPool.freeAll(tiles);
		tiles.clear();
		for (int y = startY; y <= endY; y++)
		{
			for (int x = startX; x <= endX; x++)
			{
				Cell cell = layer.getCell(x, y);
				if (cell != null)
				{
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					tiles.add(rect);
				}
				Cell cellB = layerB.getCell(x, y);
				if (cellB != null)
				{
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					tiles.add(rect);
				}
			}
		}
	}

	public void getExits(int startX, int startY, int endX, int endY,
			Array<Rectangle> exits)
	{
		TiledMapTileLayer door = (TiledMapTileLayer) map.getLayers().get(4);
		rectPool.freeAll(exits);
		exits.clear();
		for (int y = startY; y <= endY; y++)
		{
			for (int x = startX; x <= endX; x++)
			{
				Cell cell = door.getCell(x, y);
				if (cell != null)
				{
					Rectangle rect = rectPool.obtain();
					rect.set(x, y, 1, 1);
					exits.add(rect);
				}
			}
		}
	}

	private void renderBird(float deltaTime)
	{
		TextureRegion frame = null;
		switch (bird.state)
		{
		case Stand:
			frame = stand.getKeyFrame(bird.stateTime);
			break;
		case Walk:
			frame = walk.getKeyFrame(bird.stateTime);
			break;
		case Jump:
			frame = jump.getKeyFrame(bird.stateTime);
			break;
		}
		SpriteBatch batch = renderer.getSpriteBatch();
		batch.begin();
		if (bird.facesRight)
		{
			batch.draw(frame, bird.pos.x, bird.pos.y,
					projects.cbag.codebird.bird.WIDTH,
					projects.cbag.codebird.bird.HEIGHT);
		} else
		{
			batch.draw(frame, bird.pos.x + projects.cbag.codebird.bird.WIDTH,
					bird.pos.y, -projects.cbag.codebird.bird.WIDTH,
					projects.cbag.codebird.bird.HEIGHT);
		}
		batch.end();
	}

	private void renderEnemies()
	{
		Texture frame = new Texture(CodeBird.controller.get_avatar_path());
		SpriteBatch batch = renderer.getSpriteBatch();
		batch.begin();
		batch.draw(frame, enemy.pos.x + projects.cbag.codebird.bird.WIDTH,
				enemy.pos.y, -projects.cbag.codebird.bird.WIDTH,
				projects.cbag.codebird.bird.HEIGHT);
		batch.end();
	}

}
