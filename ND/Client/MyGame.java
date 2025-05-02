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

	public boolean axis = true;
	private boolean gameOver,npcR = false;
	private double lastFrameTime, currFrameTime, elapsTime;

	private int horizons;
	private float panX, panY = 0.0f;
	private float zoom = 1.0f;
	private String counterStr = "";

	private GameObject avatar,npc,terr, bat,hammer, x,y,z;
	private ObjShape terrS,linxS,linyS,linzS, batS,hammerS;
	private TextureImage avatartx,npctx, ghostT, hmap, ground, wood,hammerTx;

	private IAudioManager audioMgr;
	private Sound swingSound;

	private  AnimatedShape avatarS, ghostS,npcS;

	public static MyGame game;

	public MyGame(String serverAddress, int serverPort, String protocol)
	{	super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("UDP") == 0)
			this.serverProtocol = ProtocolType.UDP;
	}
	public static void main(String[] args)
	{	game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
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
	{	avatarS = new AnimatedShape("panda.rkm", "panda.rks");
		avatarS.loadAnimation("RUN", "panda.rka");
		npcS = new AnimatedShape("panda.rkm", "panda.rks");
		npcS.loadAnimation("RUN", "panda.rka");
		ghostS = new AnimatedShape("panda.rkm", "panda.rks");
		ghostS.loadAnimation("RUN", "panda.rka");
		batS = new ImportedModel("bat.obj");
		hammerS = new ImportedModel("hammer.obj");
		terrS = new TerrainPlane(400);
		linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
	}

	@Override
	public void loadTextures()
	{
		avatartx = new TextureImage("pandatx.jpg");
		npctx = new TextureImage("redPandaTx.png");
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
		avatar = new GameObject(GameObject.root(), avatarS, avatartx);
		initialTranslation = (new Matrix4f()).translation(0f,0f,0f);
		initialScale = (new Matrix4f()).scaling(1f, 1f, 1f);
		avatar.setLocalScale(initialScale);
		avatar.setLocalTranslation(initialTranslation);
		Matrix4f initialRotation = (new Matrix4f()).rotationX((float) Math.toRadians(0.0f));
		avatar.setLocalRotation(initialRotation);

		// build npc in the view of the window
		npc = new GameObject(GameObject.root(), npcS, npctx);
		initialTranslation = (new Matrix4f()).translation(8f,.5f,9f);
		initialScale = (new Matrix4f()).scaling(1f, 1f, 1f);
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
		initialTranslation = (new Matrix4f()).translation(-.25f,0.32f,.2f);
		bat.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(.05f, .05f, .05f);
		bat.setLocalScale(initialScale);

		//build hammer
		hammer = new GameObject(npc, hammerS, hammerTx);
		initialTranslation = (new Matrix4f()).translation(-.15f,.1f,0f);
		hammer.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(.1f, .1f, .1f);
		hammer.setLocalScale(initialScale);

		Matrix4f rotationY = new Matrix4f().rotationY((float) Math.toRadians(-90.0f));
		Matrix4f rotationX = new Matrix4f().rotationX((float) Math.toRadians(90.0f));

		initialRotation = new Matrix4f().mul(rotationY).mul(rotationX);
		bat.setLocalRotation(initialRotation);

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
		AudioResource resource1;
		audioMgr = engine.getAudioManager();
		resource1 = audioMgr.createAudioResource("swing_mono.wav", AudioResourceType.AUDIO_SAMPLE);
		swingSound = new Sound(resource1, SoundType.SOUND_EFFECT, 100, false);
		swingSound.initialize(audioMgr);
		swingSound.setMaxDistance(10.0f);
		swingSound.setMinDistance(0.5f);
		swingSound.setRollOff(5.0f);
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

		setupNetworking();

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


		// ----------------- initialize camera ----------------
		im = engine.getInputManager();
		String gpName = im.getFirstGamepadName();
		Camera c = (engine.getRenderSystem()).getViewport("LEFT").getCamera();

		// --- initialize physics system ---
		float[] gravity = {0f, 0f, 10f};
		physicsEngine = (engine.getSceneGraph()).getPhysicsEngine();
		physicsEngine.setGravity(gravity);
		// --- create physics world ---
		float mass = .5f;
		float up[] = {0, 0, 1};
		float radius = 0.5f;
		float height = 1.0f;
		double[] tempTransform;

// Get the NPC's current position
		Matrix4f translation = new Matrix4f(npc.getLocalTranslation());

// Apply rotation to orient capsule upright
		Matrix4f rotationY = new Matrix4f().rotationY((float) Math.toRadians(270.0f));
		Matrix4f rotationX = new Matrix4f().rotationX((float) Math.toRadians(90.0f));
		Matrix4f rotation = new Matrix4f().mul(rotationY).mul(rotationX);

// Combine rotation with translation
		Matrix4f transform = new Matrix4f().mul(translation).mul(rotation);

// Convert matrix to double array for physics engine
		tempTransform = toDoubleArray(transform.get(vals));

// Create physics capsule
		caps1P = (engine.getSceneGraph()).addPhysicsCapsuleX(
				mass, tempTransform, radius, height);
		//caps1P.setBounciness(1f);

// Attach capsule to NPC
		npc.setPhysicsObject(caps1P);

// Store avatar transform for later use
		translation = new Matrix4f(avatar.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));

		translation = new Matrix4f(terr.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		planeP = (engine.getSceneGraph()).addPhysicsStaticPlane(
				tempTransform, up, 0.0f);
		planeP.setBounciness(1.0f);
		terr.setPhysicsObject(planeP);
		engine.enableGraphicsWorldRender();
		engine.enablePhysicsWorldRender();

        // ----------------- OTHER INPUTS SECTION -----------------------------
		FwdAction fwdAction = new FwdAction(this, protClient);
		BckAction bckAction = new BckAction(this, protClient);
		TurnAction turnAction = new TurnAction(this, protClient);

			im.associateActionWithAllKeyboards(
					Key.W, fwdAction,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
					protClient.sendMoveMessage(avatar.getWorldLocation());
			im.associateActionWithAllKeyboards(
					Key.S, bckAction,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
					protClient.sendMoveMessage(avatar.getWorldLocation());
			im.associateActionWithAllKeyboards(
					Key.A, turnAction,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			im.associateActionWithAllKeyboards(
					Key.D, turnAction,
					InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// Zooming in and out HUD 2
		im.associateActionWithAllKeyboards(
				Key.UP, (time, e) -> {
					zoom -= 0.1f; // Zoom in
					if (zoom < 0.25f) zoom = 0.25f; // Minimum zoom level
				},
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

		im.associateActionWithAllKeyboards(
				Key.DOWN, (time, e) -> {
					zoom += 0.1f; // Zoom out
					if (zoom > 2.0f) zoom = 2.0f; // Maximum zoom level
				},
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

		// Panning left and right HUD 2
		im.associateActionWithAllKeyboards(
				Key.LEFT, (time, e) -> {
					panX -= 0.1f; // Pan left
				},
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

		im.associateActionWithAllKeyboards(
				Key.RIGHT, (time, e) -> {
					panX += 0.1f; // Pan right
				},
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN
		);

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

		long startTime = 0,elapsedTime,prevTime = 0,amt = 0;
		double totalTime = System.currentTimeMillis() - startTime;
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();



		// HUD 1
		String dispStr1 = counterStr;
		Vector3f hud1Color = new Vector3f(1, 0, 0);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);

		avatarS.updateAnimation();
		setEarParameters(); 


		if (!gameOver) {
			long currentTime = System.currentTimeMillis();
			double deltaTime = (currentTime - lastFrameTime) / 1000.0; // seconds
			lastFrameTime = currentTime;
			
			im.update((float)deltaTime);
			avatarMove();
			CameraOverhead();
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
			{ if (go.getPhysicsObject() != null)
			{ // set translation
				mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
				mat2.set(3,0,mat.m30());
				mat2.set(3,1,mat.m31());
				mat2.set(3,2,mat.m32());
				go.setLocalTranslation(mat2);
// set rotation
				mat.getRotation(aa);
				mat3.rotation(aa);
				go.setLocalRotation(mat3);
			} } }

		if (npcR) {
			Vector3f npcPos = npc.getWorldLocation();
			Vector3f avatarPos = avatar.getWorldLocation();
			float distance = npcPos.distance(avatarPos);
			npcS.playAnimation("RUN", 1.0f, AnimatedShape.EndType.LOOP, 0);
			if (distance > 0.15f && distance <= 6.0f) {
				Vector3f direction = new Vector3f(avatarPos).sub(npcPos).normalize();
				float speed = 0.08f;
				direction.mul(speed);
				Vector3f newPos = new Vector3f(npcPos).add(direction);
				npc.setLocalLocation(newPos);
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

		// Update the bat's position and orientation relative to the avatar
		Matrix4f handOffset = new Matrix4f().translation(-0.25f, 0.32f, .2f);  // Adjust to avatar's hand position
		Matrix4f relativeRotation = new Matrix4f().rotationY((float) Math.toRadians(-90.0f));  // Initial bat orientation

		// Combine the transformations: avatar rotation -> hand offset -> bat rotation
		Matrix4f finalTransform = new Matrix4f().mul(avatar.getLocalRotation()).mul(handOffset).mul(relativeRotation);
		bat.setLocalTranslation(finalTransform);
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
		Matrix4f initialTranslation, initialScale;
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
				System.out.println("starting physics");
				running = !running;
				break;

			case KeyEvent.VK_Q:
				if (caps1P != null) {
					Vector3f npcPos = npc.getWorldLocation();
					Vector3f avatarPos = avatar.getWorldLocation();

					Vector3f knockbackDir = new Vector3f();
					knockbackDir.sub(npcPos, avatarPos);  // Direction from avatar to NPC

					if (knockbackDir.length() > 0) {
						knockbackDir.normalize();
						float knockbackStrength = 10.0f;
						knockbackDir.x *= knockbackStrength;
						knockbackDir.y *= knockbackStrength;
						knockbackDir.z *= knockbackStrength;

						float[] knockbackVelocity = new float[] {
								knockbackDir.x, knockbackDir.y, knockbackDir.z
						};
						caps1P.setLinearVelocity(knockbackVelocity);
						System.out.println("Knockback velocity set on ghost.");
					}
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

			case KeyEvent.VK_W:
				avatarS.playAnimation("RUN", 1.0f, AnimatedShape.EndType.LOOP, 0);
				break;

		}
		super.keyPressed(e);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		super.keyReleased(e);
		if (e.getKeyCode() == KeyEvent.VK_W) {
			avatarS.stopAnimation();
		}
	}


	// ---------- NETWORKING SECTION ----------------
	public AnimatedShape getAvatarShape() { return avatarS; }
	public AnimatedShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghostT; }
	public GhostManager getGhostManager() { return gm; }
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