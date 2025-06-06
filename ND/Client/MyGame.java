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

	private Light greenSpotlight; // For NPC death
	private Light redGlobalLight; // For player death

	private List<Light> gameLights = new ArrayList<>();
	private Light orangeFlashLight; // For successful hit
	private long orangeFlashEndTime = 0;
	private static final long orangeFlash = 500;


	private PhysicsEngine physicsEngine;
	public PhysicsObject caps1P, caps2P, caps3P, planeP;

	private boolean running = true;
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
	private long gameStartTime;

	private static final float attackRange = 2.0f;

	// AI variables
	private static final float AI_ATTACK_RANGE = 2.5f;
	private static final float AI_DASH_RANGE = 5.0f;
	private static final long AI_DASH_COOLDOWN = 5000; // 5 seconds
	private static final float AI_MOVE_SPEED = 0.03f;
	private long lastAIActionTime = 0;
	private long lastAIDashTime = 0;
	private boolean aiIsSwinging = false;
	private long aiSwingEndTime = 0;
	private boolean aiIsDashing = false;
	private long aiDashEndTime = 0;

	private int horizons;
	private float panX, panY = 0.0f;
	private float zoom = 1.0f;
	private String counterStr = "";

	private GameObject avatar,npc,terr, bat,hammer, x,y,z;
	private ObjShape terrS,linxS,linyS,linzS, batS,hammerS;
	private TextureImage whitePandatx, redPandatx, ghostT, hmap, ground, wood,hammerTx;

	private IAudioManager audioMgr;
	public Sound swingSound;

	private  AnimatedShape avatarS, ghostS,npcS;

	private float cRadius;
	private float cHeight;

	private static final float DEADZONE = .7f;

	//Animation timing
	private boolean wHeld      = false;
	private boolean isSwinging = false;
	private long    swingEnd   = 0L;
	private static final long SWING_MS = 600;

	private boolean isSwingOnCooldown = false;
	private long lastSwingTime = 0;
	private static final long SWING_COOLDOWN_MS = 750;

	//Menu
	private enum GameState { MENU, PLAYING, AVATARSELECT }
	private GameState gameState    = GameState.MENU;
	private int       menuSelection = 0;

	//batswing
	private long batSwingStart   = 0L;
	private Matrix4f batRestRotation;
	private final float swingAmplitude = (float)Math.toRadians(180f);  // 45° arc
	private Vector3f batRestOffset;

	//dash
	private boolean isDashing = false;
	private long dashStartTime = 0;
	private long dashCooldownEnd = 0;
	private static final long DASH_DURATION = 300;
	private static final long DASH_COOLDOWN = 3000; // 3 seconds
	private static final float DASH_SPEED = 0.3f;

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

		// Green spotlight
		greenSpotlight = new Light();
		greenSpotlight.setType(Light.LightType.SPOTLIGHT);
		greenSpotlight.setLocation(new Vector3f(0f, 5f, 0f));
		greenSpotlight.setDirection(new Vector3f(0f, -1f, 0f));
		greenSpotlight.setAmbient(0f, 1f, 0f); // Green ambient
		greenSpotlight.setDiffuse(0f, 1f, 0f); // Green diffuse
		greenSpotlight.setSpecular(0f, 1f, 0f); // Green specular
		greenSpotlight.setCutoffAngle(45f);
		greenSpotlight.disable(); // Start disabled
		(engine.getSceneGraph()).addLight(greenSpotlight);

		// Red global light
		redGlobalLight = new Light();
		redGlobalLight.setType(Light.LightType.POSITIONAL);
		redGlobalLight.setAmbient(1f, 0f, 0f); // Red ambient
		redGlobalLight.setDiffuse(1f, 0f, 0f); // Red diffuse
		redGlobalLight.setSpecular(1f, 0f, 0f); // Red specular
		redGlobalLight.disable(); // Start disabled
		(engine.getSceneGraph()).addLight(redGlobalLight);

		// Orange flash light
		orangeFlashLight = new Light();
		orangeFlashLight.setType(Light.LightType.POSITIONAL);
		orangeFlashLight.setLocation(new Vector3f(0f, 0f, 0f));
		orangeFlashLight.setAmbient(1f, 0.5f, 0f); // Orange color
		orangeFlashLight.setDiffuse(1f, 0.5f, 0f);
		orangeFlashLight.setSpecular(1f, 0.5f, 0f);
		orangeFlashLight.disable();
		(engine.getSceneGraph()).addLight(orangeFlashLight);

	}

	public void resetPlayerLights() {
		redGlobalLight.disable();
		glight.enable();
	}

	public void resetNPCLights() {
		greenSpotlight.disable();
	}

	private void triggerOrangeFlash(Vector3f position) {
		// Position the light slightly above the hit location
		Vector3f lightPos = new Vector3f(position).add(0f, 2f, 0f);
		orangeFlashLight.setLocation(lightPos);

		// Enable and set duration
		orangeFlashLight.enable();
		orangeFlashEndTime = System.currentTimeMillis() + orangeFlash;
	}

	@Override
	public void loadSounds()
	{
		AudioResource swingsfx,bgm;
		audioMgr = engine.getAudioManager();

		swingsfx = audioMgr.createAudioResource("swing_mono.wav", AudioResourceType.AUDIO_SAMPLE);
		swingSound = new Sound(swingsfx, SoundType.SOUND_EFFECT, 500, false);
		swingSound.initialize(audioMgr);
		swingSound.setMaxDistance(10.0f);
		swingSound.setMinDistance(0.5f);
		swingSound.setRollOff(5.0f);

		bgm = audioMgr.createAudioResource("bgm.wav", AudioResourceType.AUDIO_STREAM);
		Sound backgroundMusic = new Sound(bgm, SoundType.SOUND_MUSIC, 1, true); // `true` for looping
		backgroundMusic.initialize(audioMgr);
		backgroundMusic.play();
	}
	@Override
	public void loadSkyBoxes()
	{ 	horizons = (engine.getSceneGraph()).loadCubeMap("horizons");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(horizons);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	private void handleDash(float elapsedTime) {
		if (isDashing) {
			long currentTime = System.currentTimeMillis();
			if (currentTime - dashStartTime < DASH_DURATION) {
				// Apply dash movement
				Vector3f dashDirection = avatar.getWorldForwardVector();
				Vector3f currentPos = avatar.getWorldLocation();
				Vector3f newPos = new Vector3f(
						currentPos.x + dashDirection.x * DASH_SPEED,
						currentPos.y,
						currentPos.z + dashDirection.z * DASH_SPEED
				);
				avatar.setLocalLocation(newPos);
			} else {
				isDashing = false;
			}
		}
	}

	private void handleSwing() {
		long currentTime = System.currentTimeMillis();

		// Check cooldown
		if (isSwingOnCooldown) {
			if (currentTime - lastSwingTime >= SWING_COOLDOWN_MS) {
				isSwingOnCooldown = false;
			} else {
				return; // Still on cooldown
			}
		}

		// Execute swing
		swingSound.stop();
		swingSound.setLocation(avatar.getWorldLocation());
		setEarParameters();
		swingSound.play();

		avatarS.playAnimation("SWING", 1.0f, AnimatedShape.EndType.STOP, 0);
		if (protClient != null) {
			protClient.sendAnimationMessage("SWING");
		}

		isSwinging = true;
		batSwingStart = currentTime;
		swingEnd = currentTime + SWING_MS;
		lastSwingTime = currentTime;
		isSwingOnCooldown = true;

		// Hit detection
		if (caps1P != null) {
			Vector3f npcPos = npc.getWorldLocation();
			Vector3f avatarPos = avatar.getWorldLocation();
			float distance = npcPos.distance(avatarPos);

			if (distance <= attackRange) {
				triggerOrangeFlash(avatar.getWorldLocation());
				// Apply knockback
				Vector3f kbDir = new Vector3f(npcPos).sub(avatarPos).normalize();
				float[] knockForce = {
						kbDir.x * 8.0f, // horizontal strength
						5.0f,           // upward strength
						kbDir.z * 8.0f  // horizontal strength
				};
				caps1P.setLinearVelocity(knockForce);
			}
		}
		if (caps2P != null) {
			// compute knockback direction
			for (GhostAvatar ghost : game.getGhostManager().getAllGhostAvatars()) {
				Vector3f ghostPos    = ghost.getWorldLocation();
				Vector3f avatarPos = avatar.getWorldLocation();
				Vector3f kbDir = new Vector3f(ghostPos).sub(avatarPos).normalize();
				float horizontalStrength = 8.0f;
				float upwardStrength     = 5.0f;  
				// build final velocity
				float vx = kbDir.x * horizontalStrength;
				float vz = kbDir.z * horizontalStrength;
				float vy = upwardStrength;
				caps1P.setLinearVelocity(new float[]{ vx, vy, vz });
				if(protClient != null)
					protClient.sendKnockMessage(vx, vy, vz);
			}
		}
	}

	private void updateAIBehavior(float elapsedTime) {
		if (gameState != GameState.PLAYING || !npcR) return;

		long currentTime = System.currentTimeMillis();
		Vector3f npcPos = npc.getWorldLocation();
		Vector3f avatarPos = avatar.getWorldLocation();
		float distance = npcPos.distance(avatarPos);

		// Handle current actions first
		if (aiIsSwinging) {
			if (currentTime >= aiSwingEndTime) {
				aiIsSwinging = false;
				npcS.playAnimation("RUN", 1.0f, AnimatedShape.EndType.LOOP, 0);
			} else {
				// Continue swinging animation
				return;
			}
		}

		if (aiIsDashing) {
			if (currentTime >= aiDashEndTime) {
				aiIsDashing = false;
			} else {
				// Apply dash movement
				Vector3f dashDirection = npc.getWorldForwardVector();
				Vector3f newPos = new Vector3f(
						npcPos.x + dashDirection.x * DASH_SPEED * 1.5f,
						npcPos.y,
						npcPos.z + dashDirection.z * DASH_SPEED * 1.5f
				);
				npc.setLocalLocation(newPos);
				return;
			}
		}

		// Behavior tree decision making
		if (distance <= AI_ATTACK_RANGE) {
			// Attack if in range
			performAISwing();
		}
		else if (distance <= AI_DASH_RANGE && (currentTime - lastAIDashTime) > AI_DASH_COOLDOWN) {
			// Dash if in dash range and cooldown is over
			performAIDash();
		}
		else {
			// Move toward player
			moveTowardPlayer();
		}

		// Update NPC rotation to face player
		npc.lookAt(avatar);
	}

	private void moveTowardPlayer() {
		Vector3f npcPos = npc.getWorldLocation();
		Vector3f avatarPos = avatar.getWorldLocation();

		// Only move if not too close (to prevent jitter)
		if (npcPos.distance(avatarPos) > 1.5f) {
			Vector3f direction = new Vector3f(avatarPos).sub(npcPos).normalize();
			direction.mul(AI_MOVE_SPEED);
			Vector3f newPos = new Vector3f(npcPos).add(direction);

			// Adjust height based on terrain
			float height = terr.getHeight(newPos.x, newPos.z);
			newPos.y = height;

			npc.setLocalLocation(newPos);
			npcS.playAnimation("RUN", 1.0f, AnimatedShape.EndType.LOOP, 0);
		} else {
			npcS.playAnimation("IDLE", 1.0f, AnimatedShape.EndType.LOOP, 0);
		}

		// Update physics object
		if (caps1P != null) {
			Matrix4f updatedXform = new Matrix4f().translation(
					npcPos.x, npcPos.y, npcPos.z);
			caps1P.setTransform(toDoubleArray(updatedXform.get(vals)));
		}
	}

	private void performAISwing() {
		long currentTime = System.currentTimeMillis();

		// Play swing animation
		npcS.playAnimation("SWING", 1.0f, AnimatedShape.EndType.STOP, 0);
		aiIsSwinging = true;
		aiSwingEndTime = currentTime + SWING_MS;

		// Play swing sound
		swingSound.stop();
		swingSound.setLocation(npc.getWorldLocation());
		setEarParameters();
		swingSound.play();

		// Check hit detection
		Vector3f npcPos = npc.getWorldLocation();
		Vector3f avatarPos = avatar.getWorldLocation();
		float distance = npcPos.distance(avatarPos);

		if (distance <= AI_ATTACK_RANGE) {
			triggerOrangeFlash(npcPos);

			// Apply knockback to player
			if (caps2P != null) {
				Vector3f kbDir = new Vector3f(avatarPos).sub(npcPos).normalize();
				float[] knockForce = {
						kbDir.x * 8.0f, // horizontal strength
						5.0f,           // upward strength
						kbDir.z * 8.0f  // horizontal strength
				};
				caps2P.setLinearVelocity(knockForce);
			}
		}
	}

	private void performAIDash() {
		long currentTime = System.currentTimeMillis();
		aiIsDashing = true;
		aiDashEndTime = currentTime + DASH_DURATION;
		lastAIDashTime = currentTime;

		// Play dash sound
		swingSound.stop();
		swingSound.setLocation(npc.getWorldLocation());
		setEarParameters();
		swingSound.play();

		// Apply initial dash impulse if using physics
		if (caps1P != null) {
			Vector3f dashDirection = npc.getWorldForwardVector();
			float dashPower = 12f;
			float[] impulse = {
					dashDirection.x * dashPower,
					0,
					dashDirection.z * dashPower
			};
			caps1P.setLinearVelocity(impulse);
		}
	}

	@Override
	public void initializeGame()
	{
		gameStartTime = System.currentTimeMillis();
		lastFrameTime = System.currentTimeMillis();
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
		cRadius       = 0.5f;
		cHeight       = 1.0f;

		Vector3f npcPos = npc.getWorldLocation();

		Matrix4f capsuleXform = new Matrix4f().translation(npcPos.x, npcPos.y, npcPos.z);
		double[] tempTransform = toDoubleArray(capsuleXform.get(vals));
		caps1P = engine.getSceneGraph().addPhysicsCapsule(mass, tempTransform, cRadius, cHeight);


		//lock rotation
		npc.setPhysicsObject(caps1P);
		((JBulletPhysicsObject)caps1P).getRigidBody().setAngularFactor(0f);

		//Player physics obj
		Vector3f avPos  = avatar.getWorldLocation();
		// offset up by half your capsule’s height so it sits on the floor
		Matrix4f avXform = new Matrix4f().translation(avPos.x, avPos.y, avPos.z);
		caps2P = engine.getSceneGraph().addPhysicsCapsule(mass, toDoubleArray(avXform.get(vals)), cRadius, cHeight);

		avatar.setPhysicsObject(caps2P);
		((JBulletPhysicsObject)caps2P).getRigidBody().setAngularFactor(0f);

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

	private void setupInputs() {
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
				Component.Identifier.Axis.X,
				turnAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(
				Component.Identifier.Axis.Y, (time, e) -> {
					float yValue = e.getValue();
					if (Math.abs(yValue) > DEADZONE) {
						if (yValue < 0) {
							// Left stick up: move forward
							fwdAction.performAction(time, e);
						} else if (yValue > 0) {
							// Left stick down: move backward
							bckAction.performAction(time, e);
						}
					}
				}, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		// Gamepad Buttons
		im.associateActionWithAllGamepads(

				Component.Identifier.Button._0, (time, e) -> { // A Button
					if (e.getValue() == 1.0f) { // Button pressed
						handleGamepadDash();
					}
				}, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		im.associateActionWithAllGamepads(
				Component.Identifier.Button._2, (time, e) -> { // X Button
					if (e.getValue() == 1.0f) { // Button pressed
						handleSwing();
					}
				}, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		im.associateActionWithAllGamepads(
				Component.Identifier.Button._7, (time, e) -> { // Start Button
					if (e.getValue() == 1.0f && gameState == GameState.PLAYING) {
						npcR = !npcR; // Toggle NPC behavior
						System.out.println("NPC behavior: " + (npcR ? "Enabled" : "Disabled"));
					}
				}, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

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

		//better time
		long now = System.currentTimeMillis();
		long elapsedMs = now - gameStartTime;

		// convert to seconds/minutes
		long totalSeconds = elapsedMs / 1000;
		long minutes = totalSeconds / 60;
		long seconds = totalSeconds % 60;

		// format as MM:SS
		String dispStr1 = String.format("%02d:%02d", minutes, seconds);
		Vector3f hud1Color = new Vector3f(0, 0, 1);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 25, 25);

		// Handle dash
		handleDash((float)elapsedTime);

		avatarS.updateAnimation();
		ghostS.updateAnimation();
		setEarParameters();

		// Check for death plane (-2f on Y axis)
		Vector3f avatarL = avatar.getWorldLocation();
		Vector3f npcL = npc.getWorldLocation();

		// Player death check
		if (avatarL.y < -2f) {
			if (!redGlobalLight.isEnabled()) {
				redGlobalLight.enable();
				glight.disable();
				// You could add respawn logic here
			}
		} else if (redGlobalLight.isEnabled()) {
			redGlobalLight.disable();
			glight.enable();
		}

	// NPC death check
		if (npcL.y < -2f) {
			if (!greenSpotlight.isEnabled()) {
				greenSpotlight.enable();
				// Position spotlight above player
				Vector3f avatarPos = avatar.getWorldLocation();
				greenSpotlight.setLocation(new Vector3f(avatarPos.x, avatarPos.y + 5f, avatarPos.z));
				// You could add NPC respawn logic here
			}
		} else if (greenSpotlight.isEnabled()) {
			greenSpotlight.disable();
		}

	// Handle orange flash timeout
		if (orangeFlashLight.isEnabled() && System.currentTimeMillis() > orangeFlashEndTime) {
			orangeFlashLight.disable();
		}

		if (isSwinging) {
			now = System.currentTimeMillis();
			float t = (now - batSwingStart) / (float)SWING_MS;
			if (t > 1f) t = 1f;

			float angle = swingAmplitude * (float)Math.sin((float)Math.PI * t);
			Matrix4f swingRot = new Matrix4f(batRestRotation).rotateY(-angle);
			bat.setLocalRotation(swingRot);

			if (now >= swingEnd) {
				isSwinging = false;
				bat.setLocalRotation(batRestRotation);
				if (wHeld) {
					avatarS.playAnimation("RUN", 1.0f, AnimatedShape.EndType.LOOP, 0);
					if (protClient != null) {
						protClient.sendAnimationMessage("RUN");
					}
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
		System.out.println(avatar.getWorldLocation());

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
					//mat.getRotation(aa);
					//mat3.rotation(aa);
					//go.setLocalRotation(mat3);
				}
			}
			if (protClient != null) {
    			protClient.sendMoveMessage(avatar.getWorldLocation());
}
		}

		if (npcR) {
			updateAIBehavior((float)elapsedTime);
		}
	}

	private void handleGamepadDash() {
		long currentTime = System.currentTimeMillis();

		// Check cooldown
		if (currentTime < dashCooldownEnd) {
			return; // Still on cooldown
		}

		// Start dash
		isDashing = true;
		dashStartTime = currentTime;
		dashCooldownEnd = currentTime + DASH_COOLDOWN;

		// Play sound
		swingSound.stop();
		swingSound.setLocation(avatar.getWorldLocation());
		setEarParameters();
		swingSound.play();

		// Apply initial dash impulse
		Vector3f dashDirection = avatar.getWorldForwardVector();
		float dashPower = 15f; // Adjust this value as needed

		// If using physics
		if (caps2P != null) {
			float[] impulse = {
					dashDirection.x * dashPower,
					0, // No vertical boost
					dashDirection.z * dashPower
			};
			caps2P.setLinearVelocity(impulse);
		} else {
			// Non-physics movement
			Vector3f newPos = avatar.getWorldLocation();
			newPos.add(dashDirection.mul(DASH_SPEED));
			avatar.setLocalLocation(newPos);
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
		// adjusts npc height according to terrain
		loc = npc.getWorldLocation();
		if ((loc.x > -20  && loc.x < 20) && (loc.z > -20 && loc.z < 20)){
			float height = terr.getHeight(loc.x(), loc.z());
			npc.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));
		}

		//avatar height
		loc = avatar.getWorldLocation();
		if ((loc.x > -20  && loc.x < 20) && (loc.z > -20 && loc.z < 20)){
			float height = terr.getHeight(loc.x(), loc.z());
			avatar.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));
		}

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

		Vector3f newAvLoc = avatar.getWorldLocation();
		Matrix4f updatedXform = new Matrix4f().translation(
			newAvLoc.x, newAvLoc.y, newAvLoc.z);
		caps2P.setTransform(toDoubleArray(updatedXform.get(vals)));

		newAvLoc = npc.getWorldLocation();
		updatedXform = new Matrix4f().translation(
			newAvLoc.x, newAvLoc.y, newAvLoc.z);
		caps1P.setTransform(toDoubleArray(updatedXform.get(vals)));

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
				case KeyEvent.VK_X:
					for (GhostAvatar ghost : game.getGhostManager().getAllGhostAvatars()) {
					swingSound.stop();
					swingSound.setLocation( ghost.getWorldLocation() );
					setEarParameters();
					swingSound.play();
					}
					break;

				case KeyEvent.VK_SPACE:
					long currentTime = System.currentTimeMillis();
					if (currentTime > dashCooldownEnd && !isDashing) {
						// Start dash
						isDashing = true;
						dashStartTime = currentTime;
						dashCooldownEnd = currentTime + DASH_COOLDOWN;

						// Play dash sound if you have one
						swingSound.stop();
						swingSound.setLocation(avatar.getWorldLocation());
						setEarParameters();
						swingSound.play();
					}
					break;
				case KeyEvent.VK_C: // Toggle npc
					npcR = !npcR;
					if (!npcR) {
						// When turning off, reset to idle animation
						npcS.playAnimation("IDLE", 1.0f, AnimatedShape.EndType.LOOP, 0);
					}
					break;

				case KeyEvent.VK_P: // Toggle physics
					running = !running;
					break;

				case KeyEvent.VK_L:
					handleSwing();
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