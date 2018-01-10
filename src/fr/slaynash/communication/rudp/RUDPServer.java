package fr.slaynash.communication.rudp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RUDPServer {// receive buffer is bigger (4096B) and client packet is dynamic (<4096B (reliable) / ~21B or ~45B (avoidable))
	//Packet format:
	//
	//data:							 type:	 	size:
	//reliable						 [0//1]		  1
	//long send date (nanoseconds)	 [long]		  8
	//byte[] data					[byte[]]	<4083
	
	private int port;
	private Thread serverThread = null;
	private Thread clientDropHandlerThread = null;
	private DatagramSocket serverDS = null;
	
	private List<RUDPClient> clients = new ArrayList<RUDPClient>();
	private boolean running = false;
	private Class<? extends ClientManager> clientManager = null;
	
	public RUDPServer(int port) throws SocketException{
		this.port = port;
		serverDS = new DatagramSocket(port);
		
		serverThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(running){
					byte[] buffer = new byte[Values.RECEIVE_MAX_SIZE];
					DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
					
					try {
						serverDS.receive(datagramPacket);
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
					datagramPacket.setLength(Values.RECEIVE_MAX_SIZE);
				}
			}
		}, "RUDPServer packets receiver");
		
		clientDropHandlerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(running){
					synchronized(clients){
						long maxNS = System.nanoTime()-Values.CLIENT_TIMEOUT_TIME_NANOSECONDS;
						int i=0;
						while(i<clients.size()){
							RUDPClient client = clients.get(i);
							if(client.lastPacketReceiveTime < maxNS){
								client.disconnected("Connection timed out");
								clients.remove(i);
							}
							else i++;
						}
					}
					try {Thread.sleep(250);} catch (InterruptedException e) {e.printStackTrace();}
				}
			}
		}, "RUDPServer client drop handler");
	}
	
	public void start(){
		if(running) return;
		running = true;
		serverThread.start();
		clientDropHandlerThread.start();
		System.out.println("[RUDPServer] Server started on UDP port "+port);
	}
	
	private void handlePacket(byte[] data, InetAddress clientAddress, int clientPort){
		if(data.length == 0){System.out.println("[RUDPServer] Empty packet received");return;}
		RUDPClient clientToRemove = null;
		for(RUDPClient client:clients) if(Arrays.equals(client.address.getAddress(), clientAddress.getAddress()) && client.port == clientPort){
			if(data[1] == Values.commands.DISCONNECT){
				byte[] reason = new byte[data.length-2];
				System.arraycopy(data, 2, reason, 0, reason.length);
				try {
					client.disconnected(new String(reason, "UTF-8"));
					clientToRemove = client;
				} catch (UnsupportedEncodingException e) {e.printStackTrace();}
			}
			else{client.handlePacket(data);return;}
			break;
		}
		if(clientToRemove != null) synchronized (clients) { clients.remove(clientToRemove);return; }
		
		if(data[1] == Values.commands.HANDSHAKE_START){
			if(BytesUtils.toInt(data, 2) != Values.VERSION_MAJOR || BytesUtils.toInt(data, 6) != Values.VERSION_MINOR){
				byte[] error = "Bad version !".getBytes(StandardCharsets.UTF_8);
				byte[] reponse = new byte[error.length+1];
				reponse[0] = Values.commands.HANDSHAKE_ERROR;
				System.arraycopy(error, 0, reponse, 1, error.length);
				sendPacket(reponse, clientAddress, clientPort);
			}
			else{
				sendPacket(new byte[]{Values.commands.HANDSHAKE_OK}, clientAddress, clientPort);
				
				RUDPClient rudpclient = null;
				synchronized(clients){
					rudpclient = new RUDPClient(clientAddress, clientPort, this, clientManager);
					clients.add(rudpclient);
					System.out.println("[RUDPServer] Added new client !");
				}
				System.out.println("[RUDPServer] Initializing client...");
				rudpclient.initialize();
			}
		}
	}
	
	public int getPort(){
		return port;
	}
	
	void sendPacket(byte[] data, InetAddress address, int port){
		DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        packet.setData(data);
        try {
			serverDS.send(packet);
		} catch (IOException e) {e.printStackTrace();}
	}
	
	public void setClientPacketHandler(Class<? extends ClientManager> clientManager){
		this.clientManager = clientManager;
	}
	
	public List<RUDPClient> getConnectedUsers(){
		synchronized (clients) {
			return new ArrayList<RUDPClient>(clients);
		}
	}
	
	public void stop(){
		System.out.println("Stopping server...");
		synchronized(clients){
			running = false;
			for(RUDPClient client:clients) client.disconnect("Server shutting down");
		}
		/* Seems useless, but it can be usefull in a somes cases, so here it is
		System.out.println("Waiting for all clients to disconnect...");
		int connections = clients.size();
		System.out.println(connections+" connections remaining...");
		while(clients.size() > 0){
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {e.printStackTrace();}
			
			if(clients.size() != connections){
				System.out.println(clients.size()+" connections remaining...");
				connections = clients.size();
			}
		}
		*/
		serverDS.close();
	}
	
	void remove(RUDPClient client) {
		synchronized(clients){
			clients.remove(client);
		}
	}
}
