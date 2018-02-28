# Reliable-UDP-library
A Reliable-capable UDP Library for multiplayer games (or other)

### Compile and run
#### Requirements
This project requires those to be installed on your system:
- Eclipse IDE
- Java 8+

#### Examples:
```java
public class Server
{
    public static RUDPServer serverInstance;
    public static final int SERVER_PORT = 56448;
    public static void main(String[] args)
    {
        RUDPServer server = null;
        try {
		serverInstance = new RUDPServer(SERVER_PORT);
		serverInstance.setClientPacketHandler(ServerPHandler.class);
		serverInstance.start();
	}
	catch(SocketException e) {
		System.out.println("Port " + SERVER_PORT + " is occupied. Server couldn't be initialized.");
		System.exit(-1);
	}
	
	for(RUDPClient c : serverInstance.getConnectedUsers()) {
		c.sendPacket(new byte[]{0x00}); //send data to every client
	}
	
	serverInstance.stop();
}
```

```java
public class Client
{
    public static final InetAddress SERVER_HOST = NetUtils.getInternetAdress("localhost");
    public static final int SERVER_PORT = 56448;
    public static void main(String[] args)
    {
        try {
		client = new RUDPClient(SERVER_HOST, SERVER_PORT);
		client.setPacketHandler(ClientPHandler.instance);
		client.connect();
	}
	catch(SocketException e) {
		System.out.println("Cannot allow port for the client. Client can't be launched.");
		System.exit(-1);
	}
	catch(UnknownHostException e) {
		System.out.println("Couldn't connect to " + SERVER_HOST + ":" + SERVER_PORT + ".");
		System.exit(-1);
	}
	catch(SocketTimeoutException e) {}
	catch(IOException e) {}
	
	client.sendPacket(new byte[]{0x00}); //Send packet to the server
    }
}
```

## Getting support
If you have any question or you found a problem, you can [open an issue](https://github.com/Slaynash/Reliable-UDP-library/issues) on the github repository, or contact Slaynash#2879 on [our french Discord](https://discord.gg/n9fUUaR) Guild in the #general channel.
