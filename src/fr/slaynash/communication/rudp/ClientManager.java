package fr.slaynash.communication.rudp;

public abstract class ClientManager {
	RUDPClient rudp;
	
	public ClientManager(RUDPClient rudpClient) {
		this.rudp = rudpClient;
	}
	
	public abstract void initializeClient();
	public abstract void disconnected(String reason);
	public abstract void disconnect(String reason);
	public abstract void handlePacket(byte[] data);
	public abstract void handleReliablePacket(byte[] data, long sendNS);
}
