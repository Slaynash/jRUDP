package fr.slaynash.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import fr.slaynash.communication.handlers.PacketHandler;
import fr.slaynash.communication.rudp.Packet;
import fr.slaynash.communication.rudp.RUDPClient;
import fr.slaynash.communication.rudp.RUDPServer;
import fr.slaynash.communication.utils.NetUtils;

public class CommunicationTest {
	private static RUDPServer server;
	private static RUDPClient client;
	
	public static class ServerPHandler extends PacketHandler {

		public ServerPHandler(RUDPClient rudpClient) {
			super(rudpClient);
		}

		@Override
		public void initializeClient() {}

		@Override
		public void onDisconnected(String reason) {}

		@Override
		public void handlePacket(byte[] data) {}

		@Override
		public void receiveReliablePacket(byte[] data) {}
		
	}
	
	public static class ClientPHandler extends fr.slaynash.communication.handlers.ClientPHandler {
		public static final ClientPHandler instance = new ClientPHandler();

		public ClientPHandler() {
			super(null);
		}

		@Override
		public void onDisconnected(String reason) {}
		
		@Override
		public void initializeClient() {}
		
		@Override
		public void receiveReliablePacket(byte[] data) {
			super.receiveReliablePacket(data);
		}
		
		@Override
		public void handlePacket(byte[] data) {
			System.out.println("Non-reliable: " + NetUtils.asHexString(data));					
		}
		
		@Override
		public void handleReliablePacketOrdered(Packet packet) {
			super.handleReliablePacketOrdered(packet);
		}
	}
	
	public static void test() {
		initServer();
		
		try { Thread.sleep(2000); } catch(InterruptedException e) {}
		
		initClient();
		
		server.getConnectedUsers().get(0).sendPacket(new byte[]{1});
		for(int i=0; i<100; i++) {
			server.getConnectedUsers().get(0).sendReliablePacket(new byte[]{1});
		}
	}

	private static void initServer() {
		try {
			server = new RUDPServer(1111);
			server.setClientPacketHandler(ServerPHandler.class);
			server.start();
		}
		catch(SocketException e) {
			System.out.println("Port 1111 is occupied. Server couldn't be initialized.");
			System.exit(-1);
		}
	}
	
	private static void initClient() {
		try {
			client = new RUDPClient(InetAddress.getByName("localhost"), 1111);
			client.setPacketHandler(ClientPHandler.instance);
			client.connect();
		}
		catch(SocketException e) {
			System.out.println("Cannot allow port for the client. Client can't be launched.");
			System.exit(-1);
		}
		catch(UnknownHostException e) {
			System.out.println("Couldn't connect to localhost.");
			System.exit(-1);
		}
		catch(SocketTimeoutException e) {}
		catch(IOException e) {}
	}

	public static void main(String[] args) {
		test();
	}
}
