package tage.input.action;

import tage.GameObject;
import net.java.games.input.Event;
import org.joml.*;

import ND.Client.MyGame;
import ND.Client.ProtocolClient;

public class BckAction extends AbstractInputAction
{ private MyGame game;
    private GameObject av,dol;
    private Vector3f oldPosition, newPosition;
    private Vector4f bckDirection;
    private ProtocolClient protClient;
    private float speed = 8.0f;

    public BckAction(MyGame g, ProtocolClient p)
    { 
        game = g;
        protClient = p;
    }
    @Override
    public void performAction(float time, Event e)
    {
        // Move dolphin backward when riding
        dol = game.getAvatar();
        oldPosition = dol.getWorldLocation();
        bckDirection = new Vector4f(0f, 0f, -1f, 1f); // Negative Z-axis for backward direction
        bckDirection.mul(dol.getWorldRotation());
        bckDirection.mul(speed*time);
        newPosition = oldPosition.add(bckDirection.x(), bckDirection.y(), bckDirection.z());
        dol.setLocalLocation(newPosition);
        if(protClient != null)
            protClient.sendMoveMessage(dol.getWorldLocation());

    }
}