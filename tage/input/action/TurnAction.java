package tage.input.action;

import tage.GameObject;
import net.java.games.input.Event;
import org.joml.*;

import ND.Client.MyGame;
import ND.Client.ProtocolClient;

public class TurnAction extends AbstractInputAction {
    private MyGame game;
    private ProtocolClient protClient;
    private float turnAmount = 4.0f;

    public TurnAction(MyGame game, ProtocolClient protClient) {
        this.game = game;
        this.protClient = protClient;
    }

    @Override
    public void performAction(float time, Event evt) {
        // Determine the direction based on the key pressed
        String key = evt.getComponent().getName();

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
    }
}