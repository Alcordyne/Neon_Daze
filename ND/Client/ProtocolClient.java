package ND.Client;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;
import tage.networking.client.GameConnectionClient;
import tage.shapes.AnimatedShape;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;
	
	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game) throws IOException 
	{	super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}
	
	public UUID getID() { return id; }
	
	@Override
	protected void processPacket(Object message)
	{	String strMessage = (String)message;
		System.out.println("message received -->" + strMessage);
		String[] messageTokens = strMessage.split(",");
		
		// Game specific protocol to handle the message
		if(messageTokens.length > 0)
		{
			// Handle JOIN message
			// Format: (join,success) or (join,failure)
			if(messageTokens[0].compareTo("join") == 0)
			{	if(messageTokens[1].compareTo("success") == 0)
				{	System.out.println("join success confirmed");
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition());
				}
				if(messageTokens[1].compareTo("failure") == 0)
				{	System.out.println("join failure confirmed");
					game.setIsConnected(false);
			}	}
			
			// Handle BYE message
			// Format: (bye,remoteId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	// remove ghost avatar with id = remoteId
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}
			
			// Handle CREATE message
			// Format: (create,remoteId,x,y,z)
			// AND
			// Handle DETAILS_FOR message
			// Format: (dsfr,remoteId,x,y,z)
			if (messageTokens[0].compareTo("create") == 0 || (messageTokens[0].compareTo("dsfr") == 0))
			{	// create a new ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));

				try
				{	ghostManager.createGhostAvatar(ghostID, ghostPosition);
				}	catch (IOException e)
				{	System.out.println("error creating ghost avatar");
				}
			}
			
			// Handle WANTS_DETAILS message
			// Format: (wsds,remoteId)
			if (messageTokens[0].compareTo("wsds") == 0)
			{
				// Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition());
			}
			
			// Handle MOVE message
			// Format: (move,remoteId,x,y,z)
			if (messageTokens[0].compareTo("move") == 0)
			{
				// move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				ghostManager.updateGhostAvatar(ghostID, ghostPosition);
			}
			// Handle TURN message
			// Format: (turn,remoteId,yaw)
			if (messageTokens[0].compareTo("turn") == 0)
			{
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				// Parse out the yaw rotation as a float
				float yaw = Float.parseFloat(messageTokens[2]);

				// Update the ghost avatar's rotation
				ghostManager.updateGhostAvatarRotation(ghostID, yaw);
			}	
			// Format: (anim,remoteId,animType)
			if (messageTokens[0].compareTo("anim") == 0) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
				String animType = messageTokens[2];

				GhostAvatar ghostAvatar = ghostManager.getGhostAvatar(ghostID);
				if (ghostAvatar != null) {
					AnimatedShape ghostShape = (AnimatedShape) ghostAvatar.getShape();
					ghostShape.playAnimation(animType, 1.0f,animType.equals("SWING") ? AnimatedShape.EndType.STOP : AnimatedShape.EndType.LOOP, 0);
				}	
				if ("SWING".equals(animType)) {
					game.swingSound.stop();
					game.swingSound.setLocation(ghostAvatar.getWorldLocation());
					game.setEarParameters();
					game.swingSound.play();
				}
				}
			if (messageTokens[0].equals("knock")) {
				UUID ghostID = UUID.fromString(messageTokens[1]);
					float vx = Float.parseFloat(messageTokens[2]);
					float vy = Float.parseFloat(messageTokens[3]);
					float vz = Float.parseFloat(messageTokens[4]);
					// apply to our avatar’s physics object:
					game.caps2P.setLinearVelocity(new float[]{ vx, vy, vz });
			}
		}	
	}
	
	// The initial message from the game client requesting to join the 
	// server. localId is a unique identifier for the client. Recommend 
	// a random UUID.
	// Message Format: (join,localId)
	
	public void sendJoinMessage()
	{	try 
		{	sendPacket(new String("join," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the client is leaving the server. 
	// Message Format: (bye,localId)

	public void sendByeMessage()
	{	try 
		{	sendPacket(new String("bye," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the client�s Avatar�s position. The server 
	// takes this message and forwards it to all other clients registered 
	// with the server.
	// Message Format: (create,localId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessage(Vector3f position)
	{	try 
		{	String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the local avatar's position. The server then 
	// forwards this message to the client with the ID value matching remoteId. 
	// This message is generated in response to receiving a WANTS_DETAILS message 
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID remoteId, Vector3f position)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the local avatar has changed position.  
	// Message Format: (move,localId,x,y,z) where x, y, and z represent the position.

	public void sendMoveMessage(Vector3f position)
	{	try 
		{	String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}

	// Informs the server that the local avatar has changed orientation (yaw).
	// Message Format: (turn,localId,yaw)
	public void sendTurnMessage(float yaw)
	{
		try 
		{
			String message = new String("turn," + id.toString());
			message += "," + yaw;
			sendPacket(message);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	public void sendAnimationMessage(String animType) {
		try {
			String message = new String("anim," + id.toString() + "," + animType);
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void sendKnockMessage(float vx, float vy, float vz){
		try {
			String message = new String("knock," + id.toString() + ","+ vx + "," + vy + "," + vz);
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
