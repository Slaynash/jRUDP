package fr.slaynash.test;

import fr.slaynash.communication.handlers.OrderedPacketHandler;
import fr.slaynash.communication.handlers.PacketHandlerAdapter;
import fr.slaynash.communication.rudp.Packet;
import fr.slaynash.communication.rudp.RUDPClient;
import fr.slaynash.communication.rudp.RUDPServer;
import fr.slaynash.communication.utils.NetUtils;

public class LocalServClientTest {
	private static RUDPServer server;
	private static RUDPClient client;
	
	public static class ServerPHandler extends PacketHandlerAdapter {

	}
	
	public static class ClientPHandler extends OrderedPacketHandler {

		@Override
		public void onPacketReceived(byte[] data) {
			System.out.println(NetUtils.asHexString(data));
		}
		
		@Override
		public void onExpectedPacketReceived(Packet packet) {
			System.out.println(packet);
		}
		
		@Override
		public void onDisconnectedByRemote(String reason) {
			super.onDisconnectedByRemote(reason);
			System.out.println("DC reason: " + reason);
		}
	}
	
	public static void main(String[] args) throws Exception {
		server = new RUDPServer(1111);
		server.setPacketHandler(ServerPHandler.class);
		server.start();
		
		client = new RUDPClient(NetUtils.getInternetAdress("localhost"), 1111);
		client.setPacketHandler(ClientPHandler.class);
		client.connect();
		
		server.getConnectedClients().get(0).sendPacket(new byte[]{0});
		
		client.disconnect();
		client.connect();
		
		server.getConnectedClients().get(0).sendPacket(new byte[]{0});
		server.getConnectedClients().get(0).sendReliablePacket(new byte[]{0});
		
		client.disconnect();
		client.connect();
		
		client.disconnect();
		server.stop();
	}
}
