package tage.input.action;

import ND.MyGame;
import tage.GameObject;
import net.java.games.input.Event;
import org.joml.*;

public class FwdAction extends AbstractInputAction
{ private MyGame game;    private GameObject av,dol;
    private Vector3f oldPosition, newPosition;
    private Vector4f fwdDirection;
    public FwdAction(MyGame g)
    { game = g;
    }
    @Override
    public void performAction(float time, Event e)
    {

            dol = game.getAvatar();
            oldPosition = dol.getWorldLocation();
            fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
            fwdDirection.mul(dol.getWorldRotation());
            fwdDirection.mul(0.1f);
            newPosition = oldPosition.add(fwdDirection.x(),
                    fwdDirection.y(), fwdDirection.z());
            dol.setLocalLocation(newPosition);

    } }