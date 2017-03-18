package fr.slaynash.communication.rudp;

public abstract class ClientManager {
	protected RUDPClient rudp;
	
	public ClientManager(RUDPClient rudpClient) {
		this.rudp = rudpClient;
	}
	
	public abstract void initializeClient();
	public abstract void onDisconnected(String reason);
	public void disconnect(String reason){
		rudp.disconnect(reason);
	}
	public abstract void handlePacket(byte[] data);
	public abstract void handleReliablePacket(byte[] data, long sendNS);
}
