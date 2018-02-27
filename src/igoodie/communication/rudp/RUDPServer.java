package igoodie.communication.rudp;

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

import igoodie.communication.RUDPConstants;
import igoodie.communication.handlers.PacketHandler;
import igoodie.communication.utils.NetUtils;

public class RUDPServer {// receive buffer is bigger (4096B) and client packet is dynamic (<4096B (reliable) / ~21B or ~45B (avoidable))
	//Packet format:
	//
	//data:							 type:	 	size:
	//reliable						 [0//1]		  1
	//long send date (nanoseconds)	 [long]		  8
	//byte[] data					[byte[]]	<4083
	
	private int port;
	private DatagramSocket datagramSocket = null;
	
	private Thread serverThread = null;
	private Thread clientDropHandlerThread = null;
	
	private boolean running = false;
	private List<RUDPClient> clients = new ArrayList<RUDPClient>();
	
	private Class<? extends PacketHandler> clientManager = null;
	
	
	
	public RUDPServer(int port) throws SocketException{
		this.port = port;
		datagramSocket = new DatagramSocket(port);
		
		startServerThread();
		
		initClientDropHandler();
	}
	
	public void start(){
		if(running) return;
		running = true;
		
		serverThread.start();
		clientDropHandlerThread.start();
		
		System.out.println("[RUDPServer] Server started on UDP port "+port);
	}
	
	private void handlePacket(byte[] data, InetAddress clientAddress, int clientPort){
		//Check if packet is not empty
		if(data.length == 0){
			System.out.println("[RUDPServer] Empty packet received");
			return;
		}
		
		//check if packet is an handshake packet
		if(data[1] == RUDPConstants.Commands.HANDSHAKE_START){
			//If client is valid, add it to the list and initialize it
			
			if(NetUtils.asInt(data, 2) == RUDPConstants.VERSION_MAJOR && NetUtils.asInt(data, 6) == RUDPConstants.VERSION_MINOR){//version check
				
				sendPacket(new byte[]{RUDPConstants.Commands.HANDSHAKE_OK}, clientAddress, clientPort);
				
				final RUDPClient rudpclient = new RUDPClient(clientAddress, clientPort, this, clientManager);
				synchronized(clients) {
					clients.add(rudpclient);
				}
				System.out.println("[RUDPServer] Added new client !");
				System.out.println("[RUDPServer] Initializing client...");
				new Thread(new Runnable() {public void run() {rudpclient.initialize();}}, "RUDP Client init thread").start();
				return;
				
			}
			else {
				//Else send HANDSHAKE_ERROR "BAD_VERSION"
				
				byte[] error = "Bad version !".getBytes(StandardCharsets.UTF_8);
				byte[] reponse = new byte[error.length+1];
				reponse[0] = RUDPConstants.Commands.HANDSHAKE_ERROR;
				System.arraycopy(error, 0, reponse, 1, error.length);
				sendPacket(reponse, clientAddress, clientPort);
				
			}
		}
		
		//handle packet in ClientRUDP
		RUDPClient clientToRemove = null;
		for(RUDPClient client:clients) {
			if(Arrays.equals(client.address.getAddress(), clientAddress.getAddress()) && client.port == clientPort){
				
				if(data[1] == RUDPConstants.Commands.DISCONNECT){
					byte[] reason = new byte[data.length-2];
					System.arraycopy(data, 2, reason, 0, reason.length);
					try {
						client.disconnected(new String(reason, "UTF-8"));
						clientToRemove = client;
					} catch (UnsupportedEncodingException e) {e.printStackTrace();}
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
	
	void sendPacket(byte[] data, InetAddress address, int port){
		DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        packet.setData(data);
        try {
			datagramSocket.send(packet);
		} catch (IOException e) {e.printStackTrace();}
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
		datagramSocket.close();
	}
	
	void remove(RUDPClient client) {
		synchronized(clients){
			clients.remove(client);
		}
	}
	
	public void setClientPacketHandler(Class<? extends PacketHandler> clientManager){
		this.clientManager = clientManager;
	}
	
	public int getPort(){
		return port;
	}

	public boolean isRunning() {
		return running;
	}
	
	
	
	public void startServerThread() {
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
	
	public void initClientDropHandler() {
		clientDropHandlerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(running){
						synchronized(clients){
							long maxNS = System.nanoTime()-RUDPConstants.CLIENT_TIMEOUT_TIME_NANOSECONDS;
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
						Thread.sleep(250);
					}
				} catch (InterruptedException e) {e.printStackTrace();}
			}
		}, "RUDPServer client drop handler");
	}
}
