package tage.input.action;

import org.joml.Math;
import tage.GameObject;
import net.java.games.input.Event;
import org.joml.*;

import ND.Client.MyGame;
import ND.Client.ProtocolClient;

public class TurnAction extends AbstractInputAction {
    private MyGame game;
    private ProtocolClient protClient;
    private float turnAmount = 4.0f;
    private float gamepadTurnMultiplier = 1f;

    public TurnAction(MyGame game, ProtocolClient protClient) {
        this.game = game;
        this.protClient = protClient;
    }

    @Override
    public void performAction(float time, Event evt) {
        float turnSpeed = turnAmount * time;
        String key = evt.getComponent().getName();

        // Handle analog gamepad input
        if (key.equals("A")) {
            // Turn left
            game.getAvatar().globalYaw(turnAmount*time);
            if(protClient != null)
                protClient.sendTurnMessage(turnAmount*time);
        } else if (key.equals("D")) {
            // Turn right
            game.getAvatar().globalYaw(-turnAmount*time);
            if(protClient != null)
                protClient.sendTurnMessage(-turnAmount*time);
        }
        else {
            if (key.equals("A")) {
                // Turn left
                game.getAvatar().globalYaw(turnSpeed);
                if (protClient != null) {
                    protClient.sendTurnMessage(turnSpeed);
                }
            } else if (key.equals("D")) {
                // Turn right
                game.getAvatar().globalYaw(-turnSpeed);
                if (protClient != null) {
                    protClient.sendTurnMessage(-turnSpeed);
                }
            }
        }
    }
}