# Reliable-UDP-library
A Reliable Java UDP Library for multiplayer games and more

Specials thanks
---
Special Thanks to [iGoodie](https://github.com/iGoodie) for the work he had done and the help he gave me on this project

Compile and run
---
#### Requirements
To use this library, you only need to have java 8 or newer. No additional libraries required !

Examples:
---
```java
public class Server
{
    public static RUDPServer serverInstance;
    public static final int SERVER_PORT = 56448;
    public static void main(String[] args)
    {
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
If you have any question or you found a problem, you can [open an issue](https://github.com/Slaynash/Reliable-UDP-library/issues) on the Github repository, send me an email at [slaynash@survival-machines.fr](mailto:slaynash@survival-machines.fr), or contact me on Discord (Slaynash#2879).
