package tage.input.action;

import ND.MyGame;
import tage.GameObject;
import net.java.games.input.Event;
import org.joml.*;

public class BckAction extends AbstractInputAction
{ private MyGame game;
    private GameObject av,dol;
    private Vector3f oldPosition, newPosition;
    private Vector4f bckDirection;
    public BckAction(MyGame g)
    { game = g;
    }
    @Override
    public void performAction(float time, Event e)
    {
        // Move dolphin backward when riding
        dol = game.getAvatar();
        oldPosition = dol.getWorldLocation();
        bckDirection = new Vector4f(0f, 0f, -1f, 1f); // Negative Z-axis for backward direction
        bckDirection.mul(dol.getWorldRotation());
        bckDirection.mul(0.02f);
        newPosition = oldPosition.add(bckDirection.x(), bckDirection.y(), bckDirection.z());
        dol.setLocalLocation(newPosition);

    }
}