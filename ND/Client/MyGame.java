package ND.Client;

import tage.*;
import tage.audio.*;
import tage.input.action.AbstractInputAction;
import tage.input.action.BckAction;
import tage.input.action.FwdAction;
import tage.input.action.TurnAction;
import tage.shapes.*;
import tage.input.*;

import java.awt.event.*;
import java.lang.Math;

import java.io.*;
import java.util.*;
import java.util.UUID;
import java.net.InetAddress;

import java.net.UnknownHostException;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.networking.IGameConnection.ProtocolType;

import org.joml.*;

import tage.nodeControllers.*;

import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.JBullet.*;

/*Things to do: Stage design basic temple in middle pyramid floating in sky,
deathplane, jump controller, swing controller, custom neon skybox, environmental hazards,
hazard controller, character moves, custom models

*/
public class MyGame extends VariableFrameRateGame
{
	
	public static MyGame game;
	private static Engine engine;
	private InputManager im;
	private NodeController rc,ocs;
	private Light glight;

	private PhysicsEngine physicsEngine;
	private PhysicsObject caps1P, caps2P, planeP;

	private boolean running = false;
	private float vals[] = new float[16];

	private GhostManager gm;
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;

	public boolean axis, physicsLines;
	private boolean gameOver,npcR = false;
	private double lastFrameTime, currFrameTime, elapsTime;

	private int horizons;
	private float panX, panY = 0.0f;
	private float zoom = 1.0f;
	private String counterStr = "";

	private GameObject avatar,npc,terr, bat,hammer, x,y,z;
	private ObjShape terrS,linxS,linyS,linzS, batS,hammerS;
	private TextureImage whitePandatx, redPandatx, ghostT, hmap, ground, wood,hammerTx;

	private IAudioManager audioMgr;
	private Sound swingSound;

	private  AnimatedShape avatarS, ghostS,npcS;

	private float cRadius;
	private float cHeight;

	//Animation timing
	private boolean wHeld      = false;
	private boolean isSwinging = false;
	private long    swingEnd   = 0L;
	private static final long SWING_MS = 600; 

	//Menu
	private enum GameState { MENU, PLAYING, AVATARSELECT }
	private GameState gameState    = GameState.MENU;
	private int       menuSelection = 0;  

	//batswing
	private long    batSwingStart   = 0L;
	private Matrix4f batRestRotation;
	private final float swingAmplitude = (float)Math.toRadians(180f);  // 45° arc
	private Vector3f batRestOffset;

	public MyGame(String serverAddress, int serverPort, String protocol)
	{	super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("UDP") == 0)
			this.serverProtocol = ProtocolType.UDP;
	}
	public static void main(String[] args)
	{	
		game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void createViewports()
	{   (engine.getRenderSystem()).addViewport("LEFT",0,0,1f,1f);

		Viewport leftVp = (engine.getRenderSystem()).getViewport("LEFT");

		Camera leftCamera = leftVp.getCamera();

		leftCamera.setLocation(new Vector3f(-2,0,2));
		leftCamera.setU(new Vector3f(1,0,0));
		leftCamera.setV(new Vector3f(0,1,0));
		leftCamera.setN(new Vector3f(0,0,-1));

	}

	@Override
	public void loadShapes()
	{	avatarS = new AnimatedShape("panda2.rkm", "panda2.rks");
		avatarS.loadAnimation("RUN", "pandaRun.rka");
		avatarS.loadAnimation("SWING", "pandaSwing.rka");
		avatarS.loadAnimation("IDLE", "pandaIdle.rka");

		npcS = new AnimatedShape("panda2.rkm", "panda2.rks");
		npcS.loadAnimation("RUN", "pandaRun.rka");

		ghostS = new AnimatedShape("panda2.rkm", "panda2.rks");
		ghostS.loadAnimation("RUN", "pandaRun.rka");
		ghostS.loadAnimation("SWING", "pandaSwing.rka");
		ghostS.loadAnimation("IDLE", "pandaIdle.rka");

		batS = new ImportedModel("bat.obj");
		hammerS = new ImportedModel("hammer.obj");
		terrS = new TerrainPlane(400);
		linxS = new Line(new Vector3f(0f,0.5f,0f), new Vector3f(5f,0.5f,0f));
		linyS = new Line(new Vector3f(0f,0.5f,0f), new Vector3f(0f,5.5f,0f));
		linzS = new Line(new Vector3f(0f,0.5f,0f), new Vector3f(0f,0.5f,-5f));
	}

	@Override
	public void loadTextures()
	{
		whitePandatx = new TextureImage("pandatx.jpg");
		redPandatx = new TextureImage("redPandaTx.png");
		ghostT = new TextureImage("redPandaTx.png");
		hmap = new TextureImage("heightmap.jpg");
		ground = new TextureImage("ground.jpg");
		wood = new TextureImage("wood.png");
		hammerTx = new TextureImage("hammerTx.jpg");
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
		avatar = new GameObject(GameObject.root(), avatarS, whitePandatx);
		initialTranslation = (new Matrix4f()).translation(0f,0f,0f);
		initialScale = (new Matrix4f()).scaling(.6f, .6f, .6f);
		avatar.setLocalScale(initialScale);
		avatar.setLocalTranslation(initialTranslation);
		Matrix4f initialRotation = (new Matrix4f()).rotationX((float) Math.toRadians(0.0f));
		avatar.setLocalRotation(initialRotation);

		// build npc in the view of the window
		npc = new GameObject(GameObject.root(), npcS, redPandatx);
		initialTranslation = (new Matrix4f()).translation(8f,.5f,9f);
		initialScale = (new Matrix4f()).scaling(.6f, .6f, .6f);
		Matrix4f NrotationY = new Matrix4f().rotationY((float) Math.toRadians(210f));
		Matrix4f NrotationX = new Matrix4f().rotationX((float) Math.toRadians(0f));

		initialRotation = new Matrix4f().mul(NrotationY).mul(NrotationX);

		npc.setLocalScale(initialScale);
		npc.setLocalTranslation(initialTranslation);
		npc.setLocalRotation(initialRotation);

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

		//build bat
		bat = new GameObject(avatar, batS, wood);
		initialScale = (new Matrix4f()).scaling(.05f, .05f, .05f);
		bat.setLocalScale(initialScale);
		Matrix4f rotationX = new Matrix4f().rotationX((float) Math.toRadians(-90.0f));
		Matrix4f rotationY = new Matrix4f().rotationY((float) Math.toRadians(-90.0f));
		Matrix4f rotationZ = new Matrix4f().rotationZ((float) Math.toRadians(180.0f));
		initialRotation = new Matrix4f().mul(rotationY).mul(rotationX).mul(rotationZ);
		bat.setLocalRotation(initialRotation);
		batRestRotation = new Matrix4f(bat.getLocalRotation());
		batRestOffset = new Vector3f(-0.25f, 0.935f, -0.18f);

		//build hammer
		hammer = new GameObject(npc, hammerS, hammerTx);
		initialTranslation = (new Matrix4f()).translation(-.15f,.1f,0f);
		hammer.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(.1f, .1f, .1f);
		hammer.setLocalScale(initialScale);
		hammer.setLocalRotation(initialRotation);

	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);

		glight = new Light();
		glight.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(glight);

	}

	@Override
	public void loadSounds()
	{ 
		AudioResource swingsfx,bgm;
		audioMgr = engine.getAudioManager();

		swingsfx = audioMgr.createAudioResource("swing_mono.wav", AudioResourceType.AUDIO_SAMPLE);
		swingSound = new Sound(swingsfx, SoundType.SOUND_EFFECT, 100, false);
		swingSound.initialize(audioMgr);
		swingSound.setMaxDistance(10.0f);
		swingSound.setMinDistance(0.5f);
		swingSound.setRollOff(5.0f);

		bgm = audioMgr.createAudioResource("bgm.wav", AudioResourceType.AUDIO_STREAM);
		Sound backgroundMusic = new Sound(bgm, SoundType.SOUND_MUSIC, 2, true); // `true` for looping
		backgroundMusic.initialize(audioMgr);
		backgroundMusic.play();
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

		swingSound.setLocation(avatar.getWorldLocation());
		setEarParameters();
		swingSound.play();

		// ------------ set up node controller --------------
		rc = new RotationController(engine, new Vector3f(0,1,0), 0.01f);
		(engine.getSceneGraph()).addNodeController(rc);
		rc.toggle();


		ocs = new OrbitController(avatar,1.0f,.05f);
		(engine.getSceneGraph()).addNodeController(ocs);
		ocs.toggle();

		//BatController batc = new BatController(bat);
		//(engine.getSceneGraph()).addNodeController(batc);
		//batc.addTarget(bat);
		//batc.enable();
		

		// ----------------- initialize camera ----------------
		im = engine.getInputManager();
		String gpName = im.getFirstGamepadName();
		Camera c = (engine.getRenderSystem()).getViewport("LEFT").getCamera();

		// --- initialize physics system ---
		physicsEngine = engine.getSceneGraph().getPhysicsEngine();
		physicsEngine.setGravity(new float[]{0f, -10f, 0f});

		float mass    = 0.5f;
		cRadius       = 0.25f;
		cHeight       = 0.0001f;

		Vector3f npcPos = npc.getWorldLocation();

		float capsuleYOffset = -(cHeight / 2f - cRadius);
		Matrix4f capsuleXform = new Matrix4f().translation(npcPos.x, npcPos.y + capsuleYOffset, npcPos.z);



		double[] tempTransform = toDoubleArray(capsuleXform.get(vals));
		caps1P = engine.getSceneGraph().addPhysicsCapsule(mass, tempTransform, cRadius, cHeight);


		//lock rotation
		npc.setPhysicsObject(caps1P);
		JBulletPhysicsObject jbo = (JBulletPhysicsObject)caps1P;
		jbo.getRigidBody().setAngularFactor(0f);

		// --- now create the static floor plane underneath ---
		float[] up = {0f, 1f, 0f};
		Matrix4f boxXform = new Matrix4f().translation(0f, 0f, 0f);
		double[] boxTrans = toDoubleArray(boxXform.get(vals));
		float[] boxSize = {30f, 1f, 30f};

		PhysicsObject boxP = engine.getSceneGraph()
				.addPhysicsBox(0f, boxTrans, boxSize); // 0f for no mass, assuming this is a static object


		boxP.setBounciness(0f); // Set this to 0 for no bounciness or adjust as needed


		terr.setPhysicsObject(boxP);

		engine.enablePhysicsWorldRender();
	}

	private void setupInputs(){
		// ----------------- OTHER INPUTS SECTION -----------------------------
		FwdAction fwdAction = new FwdAction(this, protClient);
		BckAction bckAction = new BckAction(this, protClient);
		TurnAction turnAction = new TurnAction(this, protClient);

			im.associateActionWithAllKeyboards(
					Key.W, fwdAction,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateActionWithAllKeyboards(
					Key.S, bckAction,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateActionWithAllKeyboards(
					Key.A, turnAction,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateActionWithAllKeyboards(
					Key.D, turnAction,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);


		im.associateActionWithAllGamepads(
				Axis.X, (time, e) -> {
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
				Axis.Y, (time, e) -> {
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

		// MENU STATE: draw two lines of HUD and skip the rest
		if (gameState == GameState.MENU) {
			Camera cam = (engine.getRenderSystem().getViewport("LEFT").getCamera());
			Vector3f camStart = new Vector3f(0,1000f,0);
			cam.setLocation(camStart);
			// draw the two options, highlighting the selected one
			Vector3f black = new Vector3f(0,0,0);
			Vector3f blue = new Vector3f(0,0,1);
			if (menuSelection == 0) {
				engine.getHUDmanager().setHUD1("> Single Player", blue,  100, 300);
				engine.getHUDmanager().setHUD2("  Multiplayer",  black,  100, 260);
			} else {
				engine.getHUDmanager().setHUD1("  Single Player", black,  100, 300);
				engine.getHUDmanager().setHUD2("> Multiplayer",   blue, 100, 260);
			}
			return;
		}
		if (gameState == GameState.AVATARSELECT) {
			// draw the two options, highlighting the selected one
			Vector3f white = new Vector3f(1,1,1);
			Vector3f red = new Vector3f(1,0,0);
			Vector3f black = new Vector3f(0,0,0);
			if (menuSelection == 0) {
				engine.getHUDmanager().setHUD1("> White Panda", white,  100, 300);
				engine.getHUDmanager().setHUD2("  Red Panda",  black,  100, 260);
			} else {
				engine.getHUDmanager().setHUD1("  White Panda", black,  100, 300);
				engine.getHUDmanager().setHUD2("> Red Panda",   red, 100, 260);
			}
			return;
		}

		long startTime = 0,elapsedTime,prevTime = 0,amt = 0;
		double totalTime = System.currentTimeMillis() - startTime;
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();



		// HUD 1
		String dispStr1 = counterStr;
		Vector3f hud1Color = new Vector3f(1, 0, 0);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);

		avatarS.updateAnimation();
		ghostS.updateAnimation();
		setEarParameters(); 

		long now = System.currentTimeMillis();
		if (isSwinging) {
			// 1) compute normalized t in [0,1]
			float t = (now - batSwingStart) / (float)SWING_MS;
			if (t > 1f) t = 1f;

			// 2) sine easing: 0→1→0 over π
			float angle = swingAmplitude * (float)Math.sin((float)Math.PI * t);

			// 3) apply downward rotation about X relative to rest
			Matrix4f swingRot = new Matrix4f(batRestRotation)
									.rotateY(-angle);
			bat.setLocalRotation(swingRot);

			// 4) once both animation & bat have reached end, reset
			if (now >= swingEnd) {
				isSwinging = false;
				bat.setLocalRotation(batRestRotation);

				// panda back to RUN if W still down
				if (wHeld) {
					avatarS.playAnimation("RUN", 1.0f, AnimatedShape.EndType.LOOP, 0);
					if (protClient != null)
						protClient.sendAnimationMessage("RUN");
				}
			}
		}
		if (!gameOver) {
			long currentTime = System.currentTimeMillis();
			double deltaTime = (currentTime - lastFrameTime) / 1000.0; // seconds
			lastFrameTime = currentTime;
			
			im.update((float)deltaTime);
			avatarMove();
			processNetworking((float)deltaTime);
		}

		if (running)
		{ AxisAngle4f aa = new AxisAngle4f();
			Matrix4f mat = new Matrix4f();
			Matrix4f mat2 = new Matrix4f().identity();
			Matrix4f mat3 = new Matrix4f().identity();
			checkForCollisions();
			physicsEngine.update((float)elapsedTime);
			for (GameObject go:engine.getSceneGraph().getGameObjects())
			{ 
				if (go.getPhysicsObject() != null)
				{ 
					// set translation
					mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
					mat2.set(3,0,mat.m30());
					mat2.set(3,1,mat.m31());
					mat2.set(3,2,mat.m32());
					go.setLocalTranslation(mat2);

					// set rotation
					mat.getRotation(aa);
					mat3.rotation(aa);
					go.setLocalRotation(mat3);
				} 
			} 
		}

		if (npcR) {
			Vector3f npcPos = npc.getWorldLocation();
			Vector3f avatarPos = avatar.getWorldLocation();
			float distance = npcPos.distance(avatarPos);
			npcS.playAnimation("RUN", 1.0f, AnimatedShape.EndType.LOOP, 0);
			if (distance > 0.15f && distance <= 15.0f) {
				Vector3f direction = new Vector3f(avatarPos).sub(npcPos).normalize();
				float speed = 0.02f;
				direction.mul(speed);
				Vector3f newPos = new Vector3f(npcPos).add(direction);
				npc.setLocalLocation(newPos);
				npc.lookAt(avatar);

				//move capsule
				//Vector3f worldPos = npc.getWorldLocation();
				//float yOffset = (cHeight / 2f) + cRadius;
				//Matrix4f physicsMat = new Matrix4f().translation(worldPos.x, worldPos.y + yOffset, worldPos.z);
				//physicsMat.get(vals);
				//caps1P.setTransform(toDoubleArray(vals));
			}
		}
	}

	private void checkForCollisions()
	{ com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;
		dynamicsWorld =
				((JBulletPhysicsEngine)physicsEngine).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();
		for (int i=0; i<manifoldCount; i++)
		{ manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 =
					(com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
			object2 =
					(com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 =
					JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 =
					JBulletPhysicsObject.getJBulletPhysicsObject(object2);
			for (int j = 0; j < manifold.getNumContacts(); j++)
			{ contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0.0f)
				{ System.out.println("---- hit between " + obj1 + " and " + obj2);
					break;
				} } } }
	// ------------------ UTILITY FUNCTIONS used by physics
	private float[] toFloatArray(double[] arr)
	{ if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++)
		{ ret[i] = (float)arr[i];
		}
		return ret;
	}
	private double[] toDoubleArray(float[] arr)
	{ if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++)
		{ ret[i] = (double)arr[i];
		}
		return ret;
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

		// bat translations
		Vector3f handOffset = new Vector3f(-0.25f, 0.935f, -0.18f);
		Matrix4f avatarRot = avatar.getLocalRotation();
		avatarRot.transformDirection(handOffset);
		Matrix4f t = new Matrix4f().translation(handOffset);
		bat.setLocalTranslation(t);
	}


	public void setEarParameters()
	{ 
    Camera cam = engine.getRenderSystem().getViewport("LEFT").getCamera();
    audioMgr.getEar().setLocation(cam.getLocation());
    audioMgr.getEar().setOrientation(cam.getN(), new Vector3f(0,1,0));
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
		if (gameState == GameState.MENU || gameState == GameState.AVATARSELECT) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_UP:
				case KeyEvent.VK_W:
					menuSelection = (menuSelection + 1) % 2;
					break;
				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_S:
					menuSelection = (menuSelection + 1) % 2;
					break;
				case KeyEvent.VK_ENTER:
					Vector3f white = new Vector3f(1,1,1);
					
					if (menuSelection == 0) {
						//single player
						if (gameState == GameState.MENU){
							gameState = GameState.AVATARSELECT;
							setupInputs();
						}
						//white panda
						else{
							gameState = GameState.PLAYING;
							engine.getHUDmanager().setHUD1("", white, 1, 1);
							engine.getHUDmanager().setHUD2("", white, 1, 1);
							avatar.setTextureImage(whitePandatx);
						}
					} 
					else {
						//multiplayer
						if (gameState == GameState.MENU){
							gameState = GameState.AVATARSELECT;
							npc.setLocalScale((new Matrix4f()).scaling(0f, 0f, 0f));
							setupNetworking();
							setupInputs();
						}
						//red panda
						else{
							gameState = GameState.PLAYING;
							engine.getHUDmanager().setHUD1("", white, 1, 1);
							engine.getHUDmanager().setHUD2("", white, 1, 1);
							avatar.setTextureImage(redPandatx);
						}
					}
				default:
			}
		}
		else{
			switch (e.getKeyCode())
			{
				case KeyEvent.VK_SPACE: 
					for (GhostAvatar ghost : game.getGhostManager().getAllGhostAvatars()) {
					swingSound.stop();
					swingSound.setLocation( ghost.getWorldLocation() );
					setEarParameters();
					swingSound.play();
					}
					break;
				case KeyEvent.VK_C: // Toggle npc
					npcR = !npcR;
					break;

				case KeyEvent.VK_P: // Toggle physics
					running = !running;
					break;

				case KeyEvent.VK_Q:
					swingSound.stop();
					swingSound.setLocation( avatar.getWorldLocation() );
					setEarParameters();
					swingSound.play();

					avatarS.playAnimation("SWING", 1.0f, AnimatedShape.EndType.STOP, 0);
					if(protClient != null)
						protClient.sendAnimationMessage("SWING");
					isSwinging = true;
					batSwingStart  = System.currentTimeMillis();
					swingEnd       = batSwingStart + SWING_MS;

					if (caps1P != null) {
						// compute knockback direction
						Vector3f npcPos    = npc.getWorldLocation();
						Vector3f avatarPos = avatar.getWorldLocation();
						Vector3f kbDir = new Vector3f(npcPos).sub(avatarPos).normalize();
						float horizontalStrength = 5.0f;
						float upwardStrength     = 5.0f;   

						// build final velocity
						float vx = kbDir.x * horizontalStrength;
						float vz = kbDir.z * horizontalStrength;
						float vy = upwardStrength;

						caps1P.setLinearVelocity(new float[]{ vx, vy, vz });
					}
					break;

				case KeyEvent.VK_Z: // Toggle axis and physics lines
					axis = !axis;
					physicsLines = !physicsLines;
					if (axis) {
						createAxis(); 
					} else {
						hideAxis(); 
					}
					if(physicsLines){
						engine.enablePhysicsWorldRender();
					} else{
						engine.disablePhysicsWorldRender();
					}
					break;

				case KeyEvent.VK_W:
					wHeld = true;
					avatarS.playAnimation("RUN", 1.0f, AnimatedShape.EndType.LOOP, 0);
					if(protClient != null)
						protClient.sendAnimationMessage("RUN");
					break;

			}
		}
		super.keyPressed(e);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		super.keyReleased(e);
		if (e.getKeyCode() == KeyEvent.VK_W) {
			wHeld = false;
			avatarS.stopAnimation();
			avatarS.playAnimation("IDLE", 1.0f, AnimatedShape.EndType.LOOP, 0);
			if(protClient != null)
				protClient.sendAnimationMessage("IDLE");
		}
	}


	// ---------- NETWORKING SECTION ----------------
	public AnimatedShape getAvatarShape() { return avatarS; }
	public AnimatedShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghostT; }
	public GhostManager getGhostManager() { return gm; }
	public ProtocolClient getProtClient() { return protClient;}
	public static Engine getEngine() { return engine; }
	
	private void setupNetworking()
	{	isClientConnected = false;	
		try 
		{	protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} 	catch (UnknownHostException e) 
		{	e.printStackTrace();
		}	catch (IOException e) 
		{	e.printStackTrace();
		}
		if (protClient == null)
		{	System.out.println("missing protocol host");
		}
		else
		{	// Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}
	
	protected void processNetworking(float elapsTime)
	{	// Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
	}

	public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }

	public void setIsConnected(boolean value) { this.isClientConnected = value; }
	
	private class SendCloseConnectionPacketAction extends AbstractInputAction
	{	@Override
		public void performAction(float time, Event evt)
		{	if(protClient != null && isClientConnected == true)
			{	protClient.sendByeMessage();
			}
		}
	}
}