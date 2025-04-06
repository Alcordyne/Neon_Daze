package ND.Server;
import java.io.IOException;
import tage.networking.IGameConnection.ProtocolType;

public class NetworkingServer 
{
	private GameServerUDP thisUDPServer;

	public NetworkingServer(int serverPort, String protocol) 
	{	try 
		{	if(protocol.toUpperCase().compareTo("UDP") == 0)
			{	thisUDPServer = new GameServerUDP(serverPort);
				System.out.println("UDP");
			}
		} 
		catch (IOException e) 
		{	e.printStackTrace();
		}
	}

	public static void main(String[] args) 
	{	if(args.length > 1)
		{	NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
		}
	}

}
