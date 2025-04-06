package ND.Client;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;

public class GhostManager
{
	private MyGame game;
	private Vector<GhostAvatar> ghostAvatars = new Vector<GhostAvatar>();

	public GhostManager(VariableFrameRateGame vfrg)
	{	game = (MyGame)vfrg;
	}
	
	public void createGhostAvatar(UUID id, Vector3f position) throws IOException
	{	System.out.println("adding ghost with ID --> " + id);
		ObjShape s = game.getGhostShape();
		TextureImage t = game.getGhostTexture();
		GhostAvatar newAvatar = new GhostAvatar(id, s, t, position);
		Matrix4f initialScale = (new Matrix4f()).scaling(1);
		newAvatar.setLocalScale(initialScale);
		ghostAvatars.add(newAvatar);
	}
	
	public void removeGhostAvatar(UUID id)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if(ghostAvatar != null)
		{	game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			ghostAvatars.remove(ghostAvatar);
		}
		else
		{	System.out.println("tried to remove, but unable to find ghost in list");
		}
	}

	private GhostAvatar findAvatar(UUID id)
	{	GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while(it.hasNext())
		{	ghostAvatar = it.next();
			if(ghostAvatar.getID().compareTo(id) == 0)
			{	return ghostAvatar;
			}
		}		
		return null;
	}
	
	public void updateGhostAvatar(UUID id, Vector3f position)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{	ghostAvatar.setPosition(position);
		}
		else
		{	System.out.println("tried to update ghost avatar position, but unable to find ghost in list");
		}
	}
	public void updateGhostAvatarRotation(UUID id, float yaw)
	{
		GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{
			// Retrieve the current rotation from the ghostAvatar
			Matrix4f currentRotation = ghostAvatar.getLocalRotation();
			
			// Create the incremental rotation based on yaw
			Matrix4f incrementalRotation = new Matrix4f().rotateY(yaw);
			
			// Accumulate the rotation: current * incremental
			currentRotation.mul(incrementalRotation);
			
			// Set the updated rotation back to the ghost avatar
			ghostAvatar.setLocalRotation(currentRotation);
		}
		else
		{
			System.out.println("tried to update ghost avatar rotation, but unable to find ghost in list");
		}
	}
}
