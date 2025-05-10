package tage.input.action;

import tage.GameObject;
import net.java.games.input.Event;
import org.joml.*;

import ND.Client.MyGame;
import ND.Client.ProtocolClient;

public class FwdAction extends AbstractInputAction
{   private MyGame game;    
    private GameObject av,dol;
    private Vector3f oldPosition, newPosition;
    private Vector4f fwdDirection;
    private ProtocolClient protClient;
    private float speed = 6.0f;
    
	public FwdAction(MyGame g, ProtocolClient p)
	{	
        game = g;
		protClient = p;
    }

    @Override
    public void performAction(float time, Event e)
    {
            dol = game.getAvatar();
            oldPosition = dol.getWorldLocation();
            fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
            fwdDirection.mul(dol.getWorldRotation());
            fwdDirection.mul(speed*time);
            newPosition = oldPosition.add(fwdDirection.x(),
            fwdDirection.y(), fwdDirection.z());
            dol.setLocalLocation(newPosition);
            if(protClient != null)
                protClient.sendMoveMessage(dol.getWorldLocation());
    } }