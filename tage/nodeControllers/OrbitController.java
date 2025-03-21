package tage.nodeControllers;

import org.joml.Math;
import tage.*;
import org.joml.*;

/**
 * An OrbitController makes the object orbit around a target object in a circular path.
 */
public class OrbitController extends NodeController {

    private GameObject target;  // The target object to orbit around
    private float radius = 10.0f;
    private float orbitSpeed = 1.0f;
    private float angle = 0.0f; // Current angle of the orbit
    private Engine engine;

    /**
     * Creates an OrbitController with the given target, orbit radius, and speed.
     * @param target The object to orbit around.
     * @param radius The radius of the orbit.
     * @param speed The speed of the orbit (how fast the object moves along the orbit).
     */
    public OrbitController(GameObject target, float radius, float speed) {
        super();
        this.target = target;
        this.radius = radius;
        this.orbitSpeed = speed;
    }

    /**
     * This is called automatically by the RenderSystem (via SceneGraph) once per frame
     * during display(). It updates the position of the object to create the orbit effect.
     */
    @Override
    public void apply(GameObject go) {
        if (target == null) {
            return;  // If there is no target, exit
        }

        // Get the current time (elapsed time for smooth rotation)
        float elapsedTime = super.getElapsedTime();

        // Update the angle based on elapsed time and orbit speed
        angle += orbitSpeed * elapsedTime;

        // Calculate the new position using trigonometric functions
        float x = (float) Math.sin(Math.toRadians(angle)) * radius;
        float z = (float) Math.cos(Math.toRadians(angle)) * radius;

        // Create a translation matrix for the new position
        Matrix4f translationMatrix = new Matrix4f().translation(x, 0.0f, z);

        // Set the new position of the object
        go.setLocalTranslation(translationMatrix);
    }
}