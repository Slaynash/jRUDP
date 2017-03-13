package fr.slaynash.communication.rudp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RUDPClient {//TODO remove use of ByteBuffers and use functions instead
	
	private int type = Values.clientType.NORMAL_CLIENT;
	private RUDPServer server = null;
	
	InetAddress address;
	int port;
	long lastPacketReceiveTime;
	public int state = Values.connectionStates.STATE_DISCONNECTED;
	private DatagramSocket socket = null;
	private ClientManager clientManager = null;
	private Thread thread = null;
	private Thread reliableThread = null;
	private Thread receiveThread = null;
	private Thread pingThread = null;
	
	private List<Long> packetsReceived = Collections.synchronizedList(new ArrayList<Long>());
	private List<ReliablePacket> packetsSent = Collections.synchronizedList(new ArrayList<ReliablePacket>());
	private int latency = 400;
	private RUDPClient instance = this;

	public RUDPClient(InetAddress address, int port) throws SocketException{
		this.address = address;
		this.port = port;
		
		socket = new DatagramSocket();
		socket.setSoTimeout(Values.CLIENT_TIMEOUT_TIME);
		
		lastPacketReceiveTime = System.nanoTime();
	}
	
	RUDPClient(InetAddress clientAddress, int clientPort, RUDPServer rudpServer, Class<? extends ClientManager> clientManager) {
		this.address = clientAddress;
		this.port = clientPort;
		this.server = rudpServer;
		this.type = Values.clientType.SERVER_CHILD;
		Constructor<? extends ClientManager> constructor;
		try {
			constructor = clientManager.getConstructor(RUDPClient.class);
			this.clientManager = constructor.newInstance(this);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		lastPacketReceiveTime = System.nanoTime();
		state = Values.connectionStates.STATE_CONNECTING;
	}

	public void connect() throws SocketTimeoutException, SocketException, UnknownHostException, IOException{
		System.out.println("Connecting to UDP port "+port+"...");
		if(state == Values.connectionStates.STATE_CONNECTED){System.out.println("Client already connected !");return;}
		if(state == Values.connectionStates.STATE_CONNECTING){System.out.println("Client already connecting !");return;}
		
		state = Values.connectionStates.STATE_CONNECTING;
		try {
			byte[] vmaj = ByteBuffer.allocate(4).putInt(Values.VERSION_MAJOR).array();
			byte[] vmin = ByteBuffer.allocate(4).putInt(Values.VERSION_MINOR).array();
			sendPacket(new byte[]{Values.commands.HANDSHAKE_START, vmaj[0], vmaj[1], vmaj[2], vmaj[3], vmin[0], vmin[1], vmin[2], vmin[3]});
			
			byte[] buffer = new byte[8196];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
			socket.receive(packet);
			byte[] data = new byte[packet.getLength()];
			System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
			
			if(data[0] != Values.commands.HANDSHAKE_OK){
				state = Values.connectionStates.STATE_DISCONNECTED;
				byte[] dataText = new byte[data.length-1];
				System.arraycopy(data, 0, dataText, 0, dataText.length);
				System.err.println("Unable to connect: "+new String(dataText, "UTF-8"));//TODO throw Exception
			}
			else{
				state = Values.connectionStates.STATE_CONNECTED;
				receiveThread = new Thread(new Runnable() {
					@Override
					public void run() {
						while(state == Values.connectionStates.STATE_CONNECTED){
							byte[] buffer = new byte[Values.RECEIVE_MAX_SIZE];
							DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
							
							try {
								socket.receive(packet);
							} catch (SocketTimeoutException e) {
								state = Values.connectionStates.STATE_DISCONNECTED;
								disconnected("Connection timed out");
								return;
							} catch (IOException e) {
								System.err.println("An error as occured while receiving a packet: ");
								e.printStackTrace();
							}
							byte[] data = new byte[packet.getLength()];
							System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
							handlePacket(data);
							packet.setLength(Values.RECEIVE_MAX_SIZE);
						}
					}
				});
				pingThread = new Thread(new Runnable() {
					@Override
					public void run() {
						ByteBuffer timeNSBuffer = ByteBuffer.allocate(8);
						while(state == Values.connectionStates.STATE_CONNECTED){
							byte[] timeNSBytes = timeNSBuffer.putLong(0, System.nanoTime()).array();
							sendPacket(new byte[]{Values.commands.PING_REQUEST, timeNSBytes[0], timeNSBytes[1], timeNSBytes[2], timeNSBytes[3], timeNSBytes[4], timeNSBytes[5], timeNSBytes[6], timeNSBytes[7]});
							
							try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
						}
					}
				});
				reliableThread = new Thread(new Runnable() {
					@Override
					public void run() {
						while(state == Values.connectionStates.STATE_CONNECTED || (state == Values.connectionStates.STATE_DISCONNECTING && packetsSent.size() != 0)){
							synchronized(packetsSent){
								long currentNS = System.nanoTime();
								long minResendTime = currentNS+(latency*2*1000000L);
								long maxResendTime = currentNS+Values.PACKET_TIMEOUT_TIME_NANOSECONDS;
								int i=0;
								while(i<packetsSent.size()){
									ReliablePacket rpacket = packetsSent.get(i);
									if(rpacket.dateNS > maxResendTime){
										packetsSent.remove(i);
										continue;
									}
									if(rpacket.dateNS > minResendTime){
										sendPacketRaw(rpacket.data);
									}
									i++;
								}
							}
							if(latency < 25)
								try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}
							else
								try {Thread.sleep(latency*2);} catch (InterruptedException e) {e.printStackTrace();}
						}
						state = Values.connectionStates.STATE_DISCONNECTED;
					}
				});
				reliableThread.start();
				receiveThread.start();
				pingThread.start();
				System.out.println("Connected !");
			}
		} catch (SocketTimeoutException e) {
			state = Values.connectionStates.STATE_DISCONNECTED;
			//System.err.println("Unable to connect: Connection failed");//TODO throw Exception
			throw e;
		} catch (SocketException e) {
			state = Values.connectionStates.STATE_DISCONNECTED;
			throw e;
		} catch (UnknownHostException e) {
			state = Values.connectionStates.STATE_DISCONNECTED;
			throw e;
		} catch (IOException e) {
			state = Values.connectionStates.STATE_DISCONNECTED;
			throw e;
		}
	}
	
	public void sendReliablePacket(byte[] data){
		byte[] packet = new byte[data.length+9];
		long timeNS = System.nanoTime();
		byte[] timeNSBytes = ByteBuffer.allocate(8).putLong(timeNS).array();
		packet[0] = (byte)1;
		
		packet[1] = timeNSBytes[0];
		packet[2] = timeNSBytes[1];
		packet[3] = timeNSBytes[2];
		packet[4] = timeNSBytes[3];
		packet[5] = timeNSBytes[4];
		packet[6] = timeNSBytes[5];
		packet[7] = timeNSBytes[6];
		packet[8] = timeNSBytes[7];
		System.arraycopy(data, 0, packet, 9, data.length);
		
		if(type == Values.clientType.SERVER_CHILD) server.sendPacket(packet, address, port);
		else{
			DatagramPacket dpacket = new DatagramPacket(packet, packet.length, address, port);
	        try {
	        	socket.send(dpacket);
			} catch (IOException e) {e.printStackTrace();}
			synchronized(packetsSent){
				packetsSent.add(new ReliablePacket(timeNS, packet));
			}
		}
	}
	
	public void sendPacket(byte[] data){
		byte[] packetData = new byte[data.length+1];
		packetData[0] = (byte)0;
		System.arraycopy(data, 0, packetData, 1, data.length);
		
		if(type == Values.clientType.SERVER_CHILD) server.sendPacket(packetData, address, port);
		else{
			DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address, port);
	        try {
	        	socket.send(packet);
			} catch (IOException e) {e.printStackTrace();}
		}
	}

	void handlePacket(byte[] data) {
		lastPacketReceiveTime = System.nanoTime();//TODO verify if it's enough efficient (System.nanoTime() is reputed for being slow)
		if(data[0] == (byte)0 && data[1] == Values.commands.PING_REQUEST){
			byte[] l = new byte[]{Values.commands.PING_RESPONSE, data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9]};
			sendPacket(l);//sending time received (long) // ping packet format: [IN:] Reliable CMD_PING_REPONSE sendTimeNano
			return;
		}
		else if(data[0] == (byte)0 && data[1] == Values.commands.PING_RESPONSE){
			byte[] l = new byte[]{data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9]};
			ByteBuffer buffer = ByteBuffer.allocate(8);
			buffer.put(l, 0, l.length);
	        buffer.flip();
	        latency = (int) ((System.nanoTime() - buffer.getLong())/1e6);
			return;
		}
		else if(data[0] == (byte)1){
			byte[] l = new byte[]{Values.commands.RELY, data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9]};
			sendPacket(l);
			ByteBuffer buffer = ByteBuffer.allocate(8);
			buffer.put(l, 0, l.length);
			buffer.flip();//need flip
			long bl = buffer.getLong();
			Long packetOverTime = bl-(2000000000L);//2 seconds
			synchronized(packetsReceived){
				int i = 0;
				while(packetsReceived.size() > i){
					long sl = packetsReceived.get(i);
					if(sl == bl) return;
					if(sl < packetOverTime) packetsReceived.remove(i);
					else i++;
				}
				packetsReceived.add(bl);
			}
			if(data[9] == Values.commands.DISCONNECT){
				byte[] packetData = new byte[data.length-10];
				System.arraycopy(data, 10, packetData, 0, data.length-10);
				try {
					disconnected(new String(packetData, "UTF-8"));
				} catch (UnsupportedEncodingException e) {e.printStackTrace();}
				return;
			}
			byte[] packetData = new byte[data.length-9];
			System.arraycopy(data, 9, packetData, 0, data.length-9);
			if(clientManager != null) clientManager.handleReliablePacket(data, bl);
		}
		else if(data[0] == (byte)0 && data[1] == Values.commands.RELY){
			System.out.println("RELY PACKET");
			byte[] l = new byte[]{data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9]};
			ByteBuffer buffer = ByteBuffer.allocate(8);
			buffer.put(l, 0, l.length);
			buffer.flip();//need flip
			long bl = buffer.getLong();
			synchronized(packetsSent){
				for(int i=0;i<packetsSent.size();i++) if(packetsSent.get(i).dateNS == bl){packetsSent.remove(i);return;}
			}
			return;
		}
		else if(clientManager != null) clientManager.handlePacket(data);
	}
	
	void disconnected(String reason) {
		state = Values.connectionStates.STATE_DISCONNECTED;
		if(clientManager != null) clientManager.onDisconnected(reason);
	}
	
	public void disconnect(String reason) {
		if(state == Values.connectionStates.STATE_DISCONNECTED) return;
		byte[] reasonB = null;
		try {reasonB = reason.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {e.printStackTrace();}
		byte[] reponse = new byte[reasonB.length+1];
		System.arraycopy(reasonB, 0, reponse, 1, reasonB.length);
		
		reponse[0] = Values.commands.DISCONNECT;
		if(type == Values.clientType.SERVER_CHILD){
			sendReliablePacket(reponse);
			state = Values.connectionStates.STATE_DISCONNECTING;
		}
		if(type == Values.clientType.NORMAL_CLIENT){
			sendPacket(reponse);
			state = Values.connectionStates.STATE_DISCONNECTED;
		}
		//clientManager.disconnect(reason);
	}
	
	public void setPacketHandler(ClientManager packetHandler){
		packetHandler.rudp = this;
		this.clientManager = packetHandler;
	}

	void initialize() {
		reliableThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(state == Values.connectionStates.STATE_CONNECTING || state == Values.connectionStates.STATE_CONNECTED || (state == Values.connectionStates.STATE_DISCONNECTING && packetsSent.size() != 0)){
					synchronized(packetsSent){
						long currentNS = System.nanoTime();
						long minResendTime = currentNS+(latency*2*1000000L);
						long maxResendTime = currentNS+Values.PACKET_TIMEOUT_TIME_NANOSECONDS;
						int i=0;
						while(i<packetsSent.size()){
							ReliablePacket rpacket = packetsSent.get(i);
							if(rpacket.dateNS > maxResendTime){
								packetsSent.remove(i);
								continue;
							}
							if(rpacket.dateNS > minResendTime){
								sendPacketRaw(rpacket.data);
							}
							i++;
						}
					}
					try {Thread.sleep(latency*2);} catch (InterruptedException e) {e.printStackTrace();}
				}
				state = Values.connectionStates.STATE_DISCONNECTED;
				server.remove(instance);
			}
		});
		reliableThread.start();
		state = Values.connectionStates.STATE_CONNECTED;
		thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				clientManager.initializeClient();
			}
		}, "Client thread (init)");
		thread.start();
	}
	
	public int getLatency(){
		return latency;
	}
	
	private void sendPacketRaw(byte[] data){
		if(type == Values.clientType.SERVER_CHILD) server.sendPacket(data, address, port);
		else{
			DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
	        try {
	        	socket.send(packet);
			} catch (IOException e) {e.printStackTrace();}
		}
	}
	
	private static class ReliablePacket{
		protected long dateNS;
		protected byte[] data;

		public ReliablePacket(long dateNS, byte[] data){
			this.dateNS = dateNS;
			this.data = data;
		}
	}
}
