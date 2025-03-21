package ND;

import tage.*;
import tage.input.action.BckAction;
import tage.input.action.FwdAction;
import tage.shapes.*;
import tage.input.*;

import java.awt.event.*;
import java.lang.Math;

import org.joml.*;
import tage.nodeControllers.*;

/*Things to do: Stage design basic temple in middle pyramid floating in sky,
deathplane, jump controller, swing controller, custom neon skybox, environmental hazards,
hazard controller, character moves, custom models

*/
public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;

	private int horizons;

	private NodeController rc,ocs;

	public boolean axis = true;
	private boolean gameOver = false;
	private double lastFrameTime, currFrameTime, elapsTime;

	private float panX, panY = 0.0f;
	private float zoom = 1.0f;

	private String counterStr = "";

	private InputManager im;
	private GameObject avatar,terr,x,y,z;
	private ObjShape avatarS,terrS,linxS,linyS,linzS;
	private TextureImage avatartx, hmap,ground;

	private Light glight;

	public MyGame() { super(); }

	public static void main(String[] args)
	{

		MyGame game = new MyGame();
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void createViewports()
	{   (engine.getRenderSystem()).addViewport("LEFT",0,0,1f,1f);
		(engine.getRenderSystem()).addViewport("RIGHT",.75f,0,.25f,.25f);
		Viewport leftVp = (engine.getRenderSystem()).getViewport("LEFT");
		Viewport rightVp = (engine.getRenderSystem()).getViewport("RIGHT");
		Camera leftCamera = leftVp.getCamera();
		Camera rightCamera = rightVp.getCamera();
		rightVp.setHasBorder(true);
		rightVp.setBorderWidth(4);
		rightVp.setBorderColor(0.0f, 1.0f, 0.0f);
		leftCamera.setLocation(new Vector3f(-2,0,2));
		leftCamera.setU(new Vector3f(1,0,0));
		leftCamera.setV(new Vector3f(0,1,0));
		leftCamera.setN(new Vector3f(0,0,-1));
		rightCamera.setLocation(new Vector3f(0,2,0));
		rightCamera.setU(new Vector3f(1,0,0));
		rightCamera.setV(new Vector3f(0,0,-1));
		rightCamera.setN(new Vector3f(0,-1,0));
	}

	@Override
	public void loadShapes()
	{	avatarS = new ImportedModel("dolphinHighPoly.obj");
		terrS = new TerrainPlane(400);
		linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
	}

	@Override
	public void loadTextures()
	{
		avatartx = new TextureImage("Dolphin_HighPolyUV.png");
		hmap = new TextureImage("heightmap.jpg");
		ground = new TextureImage("ground.jpg");

	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale;

		// add X,Y,-Z axes
		if(axis) {
			x = new GameObject(GameObject.root(), linxS);
			y = new GameObject(GameObject.root(), linyS);
			z = new GameObject(GameObject.root(), linzS);
			(x.getRenderStates()).setColor(new Vector3f(1f, 0f, 0f));
			(y.getRenderStates()).setColor(new Vector3f(0f, 1f, 0f));
			(z.getRenderStates()).setColor(new Vector3f(0f, 0f, 1f));
		}



		// build avatar in the center of the window
		avatar = new GameObject(GameObject.root(), avatarS, avatartx);
		initialTranslation = (new Matrix4f()).translation(0f,0f,0f);
		avatar.setLocalTranslation(initialTranslation);
		Matrix4f initialRotation = (new Matrix4f()).rotationY(
				(float) Math.toRadians(135.0f));
		avatar.setLocalRotation(initialRotation);

		// build terrain object
		terr = new GameObject(GameObject.root(), terrS, ground);
		initialTranslation = (new Matrix4f()).translation(0f,0f,0f);
		terr.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(15.0f, 1.0f, 15.0f);
		terr.setLocalScale(initialScale);
		terr.setHeightMap(hmap);
        // set tiling for terrain texture
		terr.getRenderStates().setTiling(1);
		terr.getRenderStates().setTileFactor(1);

	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);

		glight = new Light();
		glight.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(glight);

	}
	@Override
	public void loadSkyBoxes()
	{ 	horizons = (engine.getSceneGraph()).loadCubeMap("horizons");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(horizons);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void initializeGame()
	{	lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);


		// ------------ set up node controller --------------
		rc = new RotationController(engine, new Vector3f(0,1,0), 0.01f);
		(engine.getSceneGraph()).addNodeController(rc);
		rc.toggle();


		ocs = new OrbitController(avatar,1.0f,.05f);
		(engine.getSceneGraph()).addNodeController(ocs);
		ocs.toggle();


		// ----------------- initialize camera ----------------
		im = engine.getInputManager();
		String gpName = im.getFirstGamepadName();
		Camera c = (engine.getRenderSystem()).getViewport("LEFT").getCamera();


        // ----------------- OTHER INPUTS SECTION -----------------------------
		FwdAction fwdAction = new FwdAction(this);
		BckAction bckAction = new BckAction(this);

			im.associateActionWithAllKeyboards(
					net.java.games.input.Component.Identifier.Key.W, fwdAction,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateActionWithAllKeyboards(
					net.java.games.input.Component.Identifier.Key.S, bckAction,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.A, (time, e) -> {

						avatar.globalYaw(.02f); // Rotate dolphin left

				}, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.D, (time, e) -> {

						avatar.globalYaw(-0.02f); // Rotate dolphin right

				}, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// Zooming in and out HUD 2
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.UP, (time, e) -> {
					zoom -= 0.1f; // Zoom in
					if (zoom < 0.25f) zoom = 0.25f; // Minimum zoom level
				},
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.DOWN, (time, e) -> {
					zoom += 0.1f; // Zoom out
					if (zoom > 2.0f) zoom = 2.0f; // Maximum zoom level
				},
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

		// Panning left and right HUD 2
		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.LEFT, (time, e) -> {
					panX -= 0.1f; // Pan left
				},
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

		im.associateActionWithAllKeyboards(
				net.java.games.input.Component.Identifier.Key.RIGHT, (time, e) -> {
					panX += 0.1f; // Pan right
				},
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

		im.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.X, (time, e) -> {
					float xValue = e.getValue();
					if (xValue < 0) {
						// Negative X: yaw left

							avatar.globalYaw(0.03f);

					} else if (xValue > 0) {
						// Positive X: yaw right

							avatar.globalYaw(-0.03f);

					} {

					}
				}, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
				net.java.games.input.Component.Identifier.Axis.Y, (time, e) -> {
					// Perform action based on Y-axis (negative or positive)
					float yValue = e.getValue();
					if (yValue < 0) {
						// Negative Y: Move backward
						bckAction.performAction(time, e);
					} else if (yValue > 0) {
						// Positive Y: Move forward
						fwdAction.performAction(time, e);
					}
				}, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

	}

	@Override
	public void update() {

		// HUD 1
		String dispStr1 = counterStr;
		Vector3f hud1Color = new Vector3f(1, 0, 0);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);


		if (!gameOver) {
			elapsTime += (currFrameTime - lastFrameTime) / 1000.0;

			im.update((float)elapsTime);
			avatarMove();
			CameraOverhead();





		}
	}

	public GameObject getAvatar() { return avatar; }

	public void avatarMove(){
		Vector3f loc, fwd, up, right;
		// adjusts avatar height according to terrain
		loc = avatar.getWorldLocation();
		float height = terr.getHeight(loc.x(), loc.z());
		avatar.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));

		Camera cam = (engine.getRenderSystem().getViewport("LEFT").getCamera());
		loc = avatar.getWorldLocation();
		fwd = avatar.getWorldForwardVector();
		up = avatar.getWorldUpVector();
		right = avatar.getWorldRightVector();

		fwd.normalize();
		up.normalize();
		right.normalize();

		cam.setU(right);
		cam.setV(up);
		cam.setN(fwd);
		Vector3f cameraOffset = new Vector3f(fwd).mul(-2.5f).add(up.mul(1.3f));
		Vector3f cameraPosition = new Vector3f(loc).add(cameraOffset);
		cam.setLocation(cameraPosition);
	}
	// overhead camera for viewport 2
	public void CameraOverhead(){
		Camera rightCamera = engine.getRenderSystem().getViewport("RIGHT").getCamera();
		Vector3f avatarPos = avatar.getWorldLocation();

		Vector3f cameraOffset = new Vector3f(panX, 10 * zoom, panY);
		Vector3f cameraPosition = new Vector3f(avatarPos).add(cameraOffset);
		rightCamera.setLocation(cameraPosition);
		// Make the camera look at the avatar
		rightCamera.lookAt(new Vector3f(avatarPos.x + panX, 0, avatarPos.z + panY));
	}
	private void createAxis() {
		x = new GameObject(GameObject.root(), linxS);
		y = new GameObject(GameObject.root(), linyS);
		z = new GameObject(GameObject.root(), linzS);
		(x.getRenderStates()).setColor(new Vector3f(1f, 0f, 0f));
		(y.getRenderStates()).setColor(new Vector3f(0f, 1f, 0f));
		(z.getRenderStates()).setColor(new Vector3f(0f, 0f, 1f));
	}

	private void hideAxis() {
		if (x != null) {
			x.getParent().removeChild(x);
			x = null;
		}
		if (y != null) {
			y.getParent().removeChild(y);
			y = null;
		}
		if (z != null) {
			z.getParent().removeChild(z);
			z = null;
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		Matrix4f initialTranslation, initialScale;
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_SPACE: // Disarm
			{
				}
				break;


			case KeyEvent.VK_Z: // Toggle axis
				axis = !axis;
				if (axis) {
					createAxis(); // Create axis lines
				} else {
					hideAxis(); // Hide axis lines
				}
				break;

		}
		super.keyPressed(e);
	}


}

