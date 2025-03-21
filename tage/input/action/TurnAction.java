package tage.input.action;

import ND.MyGame;
import net.java.games.input.Event;
import org.joml.Vector3f;
import tage.Camera;
import tage.VariableFrameRateGame;

public class TurnAction extends AbstractInputAction {
    private MyGame game;
    private Camera camera;
    private Vector3f rightVector, upVector, fwdVector;

    public TurnAction(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        float keyValue = e.getValue();

        // Deadzone check
        if (keyValue > -0.2 && keyValue < 0.2) return;

        // Get the camera from the viewport
        camera = VariableFrameRateGame.getEngine().getRenderSystem().getViewport("MAIN").getCamera();

        // Get camera direction vectors
        rightVector = new Vector3f(camera.getU());
        upVector = new Vector3f(camera.getV());
        fwdVector = new Vector3f(camera.getN());

        // Determine rotation direction based on the key press (left or right)
        float rotationAngle = 0.01f; // The rotation speed (can be adjusted)
        if (keyValue < 0) { // Left turn (A key)
            rotationAngle = -rotationAngle;
        }

        rightVector.rotateAxis(rotationAngle, upVector.x(), upVector.y(), upVector.z());
        fwdVector.rotateAxis(rotationAngle, upVector.x(), upVector.y(), upVector.z());

        camera.setU(rightVector);
        camera.setN(fwdVector);
    }
}

