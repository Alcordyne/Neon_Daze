package tage.nodeControllers;

import tage.*;
import org.joml.*;
import org.joml.Math;

public class BatController extends NodeController {
	private GameObject avatar;
	private float swingAngle = 0.0f;  // current angle in radians
	private float swingSpeed = .0001f;  // radians per second
	private float swingAmplitude = (float) Math.toRadians(90);  // max swing range
	private float timeAccum = 0.0f;   // used to track time for sine wave

	public BatController(GameObject avatar) {
		this.avatar = avatar;
	}

	@Override
	public void apply(GameObject bat) {
		float elapsedSec = getElapsedTime();  // from NodeController base class
		timeAccum += elapsedSec;

		// Oscillate back and forth using sine wave
		swingAngle = (float) Math.sin(timeAccum * swingSpeed) * swingAmplitude;

		// Calculate bat swing rotation
		Matrix4f swingRotation = new Matrix4f().rotationY(swingAngle);

		// Attach bat to avatar with offset
		Matrix4f avatarRotation = new Matrix4f()
			.mul(avatar.getLocalRotation())
			.mul(swingRotation);

		bat.setLocalRotation(avatarRotation);
	}
}
