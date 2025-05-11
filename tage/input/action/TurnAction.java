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
    private float gamepadTurnMultiplier = .5f;

    public TurnAction(MyGame game, ProtocolClient protClient) {
        this.game = game;
        this.protClient = protClient;
    }

    @Override
    public void performAction(float time, Event evt) {
        float turnSpeed = turnAmount * time;

        // Handle analog gamepad input
        if (Math.abs(evt.getValue()) > 0) {
            // Scale turn speed by gamepad tilt amount
            turnSpeed *= Math.abs(evt.getValue()) * gamepadTurnMultiplier;

            if (evt.getValue() < 0) {
                // Left stick left: rotate left
                game.getAvatar().globalYaw(turnSpeed);
                if (protClient != null) {
                    protClient.sendTurnMessage(turnSpeed);
                }
            } else if (evt.getValue() > 0) {
                // Left stick right: rotate right
                game.getAvatar().globalYaw(-turnSpeed);
                if (protClient != null) {
                    protClient.sendTurnMessage(-turnSpeed);
                }
            }
        }
        // Handle keyboard input
        else {
            String key = evt.getComponent().getName();
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
    public void setTurnAmount(float amount) {
        turnAmount = amount;
    }

    public void setGamepadMultiplier(float multiplier) {
        gamepadTurnMultiplier = multiplier;
    }
}