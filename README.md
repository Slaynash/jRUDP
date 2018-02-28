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
    public static final int SERVER_PORT = 56448;
    public static void main(String[] args)
    {
        RUDPServer server = null;
        try{
            server = new RUDPServer(SERVER_PORT);
        }catch(Exception e){
            System.err.println("Unable to start server at port "+SERVER_PORT+" :");
            e.printStackTrace();
            System.exit(1);
        }
        
        server.setClientPacketHandler(ClientHandler.class);
		server.start();
        
        //some code
        
        server.stop();
    }
}
```

```java
public class Client
{
    public static final int SERVER_PORT = 56448;
    public static void main(String[] args)
    {
        InetAddress SERVER_INETADDRESS = InetAddress.getByName("127.0.0.1");
        RUDPClient client = null;
        try{
            client = new RUDPClient(SERVER_INETADDRESS, SERVER_PORT);
        }catch(Exception e){
            System.err.println("Unable to start server at port "+SERVER_PORT+" :");
            e.printStackTrace();
            System.exit(1);
        }
        
        client.setClientPacketHandler(Client.class);
		client.connect();
        
        //some code
        
        client.disconnect("Disconnected by user");
    }
}
```

## Getting support
If you have any question or you found a problem, you can [open an issue](https://github.com/Slaynash/Reliable-UDP-library/issues) on the github repository, or contact Slaynash#2879 on [our french Discord](https://discord.gg/n9fUUaR) Guild in the #general channel.
