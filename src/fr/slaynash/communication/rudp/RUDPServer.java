package fr.slaynash.communication.rudp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RUDPServer {// receive buffer is bigger (8096B) and client packet is dynamic (<8096B (reliable) / ~21B or ~45B (avoidable))
	//Packet format:
	//
	//data:							 type:	 	size:
	//reliable						 [0//1]		  1
	//long send date (nanoseconds)	 [long]		  8
	//byte[] data					[byte[]]	<8083
	
	private int port;
	private Thread serverThread = null;
	private Thread clientDropHandlerThread = null;
	private DatagramSocket serverDS = null;
	
	private List<RUDPClient> clients = Collections.synchronizedList(new ArrayList<RUDPClient>());
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
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					
					try {
						serverDS.receive(packet);
						//System.out.println("packet received");
					} catch (IOException e) {
						if(running){
							System.err.println("An error as occured while receiving a packet: ");
							e.printStackTrace();
						}
					}
					if(!running) break;
					byte[] data = new byte[packet.getLength()];
					System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
					if(data[0] == (byte)1) System.out.println("RPacket received. handling... (l55)");
					/*
					for(int i=0;i<data.length;i++) System.out.println("data["+i+"]: "+data[i]);
					System.out.println("Reliable: "+(data[0] == (byte)1 ? "yes" : "no"));
					System.out.println("data.length: "+data.length);
					System.out.println("Ping packet ?: "+(data[1] == Values.commands.PING ? "yes" : "no"));
					System.out.println("handshake packet ?: "+(data[1] == Values.commands.HANDSHAKE_START ? "yes" : "no"));
					*/
					//System.out.println("B:");
					//for(Byte b:data) System.out.println(b);
					//System.out.println("END");
					handlePacket(data, packet.getAddress(), packet.getPort());
					packet.setLength(Values.RECEIVE_MAX_SIZE);
				}
			}
		}, "Packets receiver");
		
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
					try {Thread.sleep(3000);} catch (InterruptedException e) {e.printStackTrace();}
				}
			}
		}, "client drop handler");
	}
	
	public void start(){
		if(running) return;
		running = true;
		serverThread.start();
		clientDropHandlerThread.start();
		System.out.println("server started on UDP port "+port);
	}
	
	private void handlePacket(byte[] data, InetAddress clientAddress, int clientPort){
		if(data.length == 0){System.out.println("empty packet received");return;}
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
			if(ByteBuffer.wrap(new byte[]{data[2], data[3], data[4], data[5]}).getInt() != Values.VERSION_MAJOR || ByteBuffer.wrap(new byte[]{data[6], data[7], data[8], data[9]}).getInt() != Values.VERSION_MINOR){
				byte[] error = null;
				try {error = "Bad version !".getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {e.printStackTrace();}
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
					System.out.println("added new client !");
				}
				System.out.println("initializing client...");
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
		return clients;
	}
	
	public void stop(){
		System.out.println("Stopping server...");
		synchronized(clients){
			running = false;
			for(RUDPClient client:clients) client.disconnect("Server shutting down");
		}
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
		serverDS.close();
	}
	
	void remove(RUDPClient client) {
		synchronized(clients){
			clients.remove(client);
		}
	}
}
