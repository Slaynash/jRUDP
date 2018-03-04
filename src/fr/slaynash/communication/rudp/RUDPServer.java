package fr.slaynash.communication.rudp;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.slaynash.communication.RUDPConstants;
import fr.slaynash.communication.enums.ConnectionState;
import fr.slaynash.communication.handlers.PacketHandler;
import fr.slaynash.communication.utils.NetUtils;

public class RUDPServer {// receive buffer is bigger (4096B) and client packet is dynamic (<4096B (reliable) / ~21B or ~45B (avoidable))
	//Packet format:
	//
	//data:							 type:	 	size:
	//packet type					[byte]		  1
	//sequence id					[short]	 	  2
	//payload						[byte[]]	<4094
	
	private int port;
	private DatagramSocket datagramSocket;
	
	private Thread serverThread;
	private Thread clientDropHandlerThread;
	
	private boolean running = false;
	private boolean stopping = false;
	private List<RUDPClient> clients = new ArrayList<RUDPClient>();
	
	private Class<? extends PacketHandler> clientManager;
	
	/* Constructor */
	public RUDPServer(int port) throws SocketException{
		this.port = port;
		datagramSocket = new DatagramSocket(port);
		
		startServerThread();
		
		initClientDropHandler();
	}
	
	/* Getter And Setters */
	public int getPort(){
		return port;
	}

	public boolean isRunning() {
		return running;
	}

	public List<RUDPClient> getConnectedClients(){
		synchronized (clients) {
			return new ArrayList<RUDPClient>(clients);
		}
	}
	
	public RUDPClient getClient(String host, int port) {
		return getClient(NetUtils.getInternetAdress(host), port);
	}

	public RUDPClient getClient(InetAddress address, int port) {
		for(RUDPClient c : clients) {
			if(c.address.equals(address) && c.port==port) {
				return c;
			}
		}
		return null;
	}
	
	public void setPacketHandler(Class<? extends PacketHandler> clientManager){
		if(Modifier.isAbstract(clientManager.getModifiers())) { //Class should not be abstract!
			throw new IllegalArgumentException("Given handler class cannot be an abstract class!");
		}
		
		this.clientManager = clientManager;
	}
	
	/* Actions */
	public void start(){
		if(running) return;
		running = true;
		
		serverThread.start();
		clientDropHandlerThread.start();
		
		System.out.println("[RUDPServer] Server started on UDP port "+port);
	}

	public void stop(){
		System.out.println("Stopping server...");
		synchronized(clients){
			stopping = true;
			for(RUDPClient client : clients) {
				client.disconnect("Server shutting down");
			}
		}
		int remainingClients = 0;
		System.out.println("Waiting for every clients to disconnect...");
		while(clients.size() != 0) {
			if(clients.size() != remainingClients) {
				remainingClients = clients.size();
				System.out.println(remainingClients+" client remaining...");
			}
			try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		}
		System.out.println("Closing server...");
		running = false;
		datagramSocket.close();
	}

	public void kick(String address, int port) {
		kick(address, port, "Kicked from server");
	}
	
	public void kick(String address, int port, String reason) {
		synchronized(clients){
			RUDPClient clientToRemove = null;
			for(RUDPClient client : clients) {
				if(client.address.getHostAddress().equals(address) && client.port == port) {
					clientToRemove = client;
					break;
				}
			}
			
			byte[] reasonB = reason.getBytes(StandardCharsets.UTF_8);
			clientToRemove.sendPacket(RUDPConstants.PacketType.DISCONNECT_FROMSERVER, reasonB);
			clientToRemove.state = ConnectionState.STATE_DISCONNECTED;
			
			clients.remove(clientToRemove);
		}
	}
	
	/* Helper Methods */
	private void handlePacket(byte[] data, InetAddress clientAddress, int clientPort){
		//Check if packet is not empty
		if(data.length == 0){
			System.out.println("[RUDPServer] Empty packet received");
			return;
		}
		
		//check if packet is an handshake packet
		if(data[0] == RUDPConstants.PacketType.HANDSHAKE_START){
			//If client is valid, add it to the list and initialize it
			
			if(stopping) {
				byte[] error = "Server closing".getBytes(StandardCharsets.UTF_8);
				byte[] reponse = new byte[error.length+1];
				reponse[0] = RUDPConstants.PacketType.HANDSHAKE_ERROR;
				System.arraycopy(error, 0, reponse, 1, error.length);
				sendPacket(reponse, clientAddress, clientPort);
			}
			else if(NetUtils.asInt(data, 1) == RUDPConstants.VERSION_MAJOR && NetUtils.asInt(data, 5) == RUDPConstants.VERSION_MINOR){//version check
				
				sendPacket(new byte[]{RUDPConstants.PacketType.HANDSHAKE_OK}, clientAddress, clientPort);
				
				final RUDPClient rudpclient = new RUDPClient(clientAddress, clientPort, this, clientManager);
				synchronized(clients) { 
					clients.add(rudpclient);
				}
				System.out.println("[RUDPServer] Added new client !");
				System.out.println("[RUDPServer] Initializing client...");
				new Thread(() -> {rudpclient.initialize();}, "RUDP Client init thread").start();
				return;
				
			}
			else {
				//Else send HANDSHAKE_ERROR "BAD_VERSION"
				
				byte[] error = "Bad version !".getBytes(StandardCharsets.UTF_8);
				byte[] reponse = new byte[error.length+1];
				reponse[0] = RUDPConstants.PacketType.HANDSHAKE_ERROR;
				System.arraycopy(error, 0, reponse, 1, error.length);
				sendPacket(reponse, clientAddress, clientPort);
				
			}
		}
		
		//handle packet in ClientRUDP
		RUDPClient clientToRemove = null;
		for(RUDPClient client : clients) {
			if(Arrays.equals(client.address.getAddress(), clientAddress.getAddress()) && client.port == clientPort){
				
				if(data[0] == RUDPConstants.PacketType.DISCONNECT_FROMCLIENT){
					byte[] reason = new byte[data.length-3];
					System.arraycopy(data, 3, reason, 0, reason.length);
					
					client.disconnected(new String(reason, StandardCharsets.UTF_8));
					clientToRemove = client;
				}
				else{
					client.handlePacket(data);
					return;
				}
				
				break;
			}
		}
		if(clientToRemove != null) {
			synchronized (clients) {
				clients.remove(clientToRemove);
			}
		}
	}
	
	protected void sendPacket(byte[] data, InetAddress address, int port){
		DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        packet.setData(data);
        try {
			datagramSocket.send(packet);
		} catch (IOException e) {e.printStackTrace();}
	}
	
	void remove(RUDPClient client) {
		synchronized(clients){
			clients.remove(client);
		}
	}
	
	private void startServerThread() {
		serverThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(running){
					byte[] buffer = new byte[RUDPConstants.RECEIVE_MAX_SIZE];
					DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
					
					try {
						datagramSocket.receive(datagramPacket);
					} catch (IOException e) {
						if(running){
							System.err.println("[RUDPServer] An error as occured while receiving a packet: ");
							e.printStackTrace();
						}
					}
					if(!running) break;
					byte[] data = new byte[datagramPacket.getLength()];
					System.arraycopy(datagramPacket.getData(), datagramPacket.getOffset(), data, 0, datagramPacket.getLength());
					
					handlePacket(data, datagramPacket.getAddress(), datagramPacket.getPort());
					datagramPacket.setLength(RUDPConstants.RECEIVE_MAX_SIZE);
				}
			}
		}, "RUDPServer packets receiver");
	}
	
	private void initClientDropHandler() {
		clientDropHandlerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(running){
						synchronized(clients){
							long maxMS = System.currentTimeMillis()-RUDPConstants.CLIENT_TIMEOUT_TIME_MILLISECONDS;
							int i=0;
							while(i<clients.size()){
								RUDPClient client = clients.get(i);
								if(client.lastPacketReceiveTime < maxMS){
									client.disconnected("Connection timed out");
								}
								else i++;
							}
						}
						Thread.sleep(250);
					}
				} catch (InterruptedException e) {e.printStackTrace();}
			}
		}, "RUDPServer client drop handler");
	}
}
