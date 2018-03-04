package fr.slaynash.communication.rudp;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import fr.slaynash.communication.RUDPConstants;
import fr.slaynash.communication.enums.ClientType;
import fr.slaynash.communication.enums.ConnectionState;
import fr.slaynash.communication.handlers.PacketHandler;
import fr.slaynash.communication.utils.NetUtils;

public class RUDPClient { //TODO remove use of ByteBuffers and use functions instead

	private class ReliablePacket{
		private long dateMS;
		private long minDateMS;
		private byte[] data;
		private short seq;

		public ReliablePacket(short seq, long dateMS, byte[] data){
			this.dateMS = dateMS;
			this.minDateMS = dateMS+(latency*2);
			this.data = data;
			this.seq = seq;
		}
	}

	private ClientType type = ClientType.NORMAL_CLIENT;
	private RUDPServer server;

	InetAddress address;
	int port;

	long lastPacketReceiveTime;
	public ConnectionState state = ConnectionState.STATE_DISCONNECTED;
	private DatagramSocket socket;
	private PacketHandler packetHandler;
	private Class<? extends PacketHandler> packetHandlerClass;

	private Thread reliableThread;
	private Thread receiveThread;
	private Thread pingThread;

	private LinkedHashMap<Short, Long> packetsReceived = new LinkedHashMap<Short, Long>();
	private List<ReliablePacket> packetsSent = Collections.synchronizedList(new ArrayList<ReliablePacket>());
	private int latency = 400;
	private RUDPClient instance = this;
	
	short sequenceReliable = 0;
	short sequenceUnreliable = 0;
	
	short lastPingSeq = 0;

	int sent, sentReliable;
	int received, receivedReliable;

	/* Constructor */
	public RUDPClient(InetAddress address, int port) throws SocketException{
		this.address = address;
		this.port = port;
	}

	RUDPClient(InetAddress clientAddress, int clientPort, RUDPServer rudpServer, Class<? extends PacketHandler> clientManager) {
		this.address = clientAddress;
		this.port = clientPort;
		this.server = rudpServer;
		this.type = ClientType.SERVER_CHILD;
		this.sentReliable = 0;
		this.sent = 0;
		Constructor<? extends PacketHandler> constructor;
		try {
			constructor = clientManager.getConstructor();
			this.packetHandler = constructor.newInstance();
			this.packetHandler.rudp = this;
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}

		lastPacketReceiveTime = System.currentTimeMillis();

		state = ConnectionState.STATE_CONNECTING;
	}

	/* Getter & Setters */
	public InetAddress getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}
	
	public boolean isConnected() {
		return state == ConnectionState.STATE_CONNECTED;
	}
	
	public void setPacketHandler(Class<? extends PacketHandler> packetHandler){
		if(Modifier.isAbstract(packetHandler.getModifiers())) { //Class should not be abstract!
			throw new IllegalArgumentException("Given handler class cannot be an abstract class!");
		}
		this.packetHandlerClass = packetHandler;
	}

	public int getLatency(){
		return latency;
	}

	public int getSent() {
		return sent;
	}

	public int getSentReliable() {
		return sentReliable;
	}

	public int getReceived() {
		return received;
	}

	public int getReceivedReliable() {
		return receivedReliable;
	}
	
	private short getReliablePacketSequence() {
		short prev = sequenceReliable;
		sequenceReliable = NetUtils.shortIncrement(sequenceReliable);
		return prev;
	}
	
	private short getUnreliablePacketSequence() {
		short prev = sequenceUnreliable;
		sequenceUnreliable = NetUtils.shortIncrement(sequenceUnreliable);
		return prev;
	}

	/* Actions */
	public void connect() throws SocketTimeoutException, SocketException, UnknownHostException, IOException, InstantiationException, IllegalAccessException{
		System.out.println("[RUDPClient] Connecting to UDP port "+port+"...");
		if(state == ConnectionState.STATE_CONNECTED){System.out.println("[RUDPClient] Client already connected !");return;}
		if(state == ConnectionState.STATE_CONNECTING){System.out.println("[RUDPClient] Client already connecting !");return;}
		if(state == ConnectionState.STATE_DISCONNECTING){System.out.println("[RUDPClient] Client currently disconnecting !");return;}
		
		if(packetHandlerClass == null) throw new IOException("Unable to connect without packet handler");
		packetHandler = packetHandlerClass.newInstance();
		packetHandler.rudp = this;

		socket = new DatagramSocket();
		socket.setSoTimeout(RUDPConstants.CLIENT_TIMEOUT_TIME);

		lastPacketReceiveTime = System.currentTimeMillis();

		state = ConnectionState.STATE_CONNECTING;
		try {
			//Send handshake packet
			byte[] handshakePacket = new byte[9];
			handshakePacket[0] = RUDPConstants.PacketType.HANDSHAKE_START;
			NetUtils.writeBytes(handshakePacket, 1, RUDPConstants.VERSION_MAJOR);
			NetUtils.writeBytes(handshakePacket, 5, RUDPConstants.VERSION_MINOR);
			sendPacketRaw(handshakePacket);

			//Receive handshake response packet
			byte[] buffer = new byte[8196];
			DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, address, port);
			socket.receive(datagramPacket);
			byte[] data = new byte[datagramPacket.getLength()];
			System.arraycopy(datagramPacket.getData(), datagramPacket.getOffset(), data, 0, datagramPacket.getLength());

			//Handle handshake response packet
			if(data[0] != RUDPConstants.PacketType.HANDSHAKE_OK){

				state = ConnectionState.STATE_DISCONNECTED;
				byte[] dataText = new byte[data.length-1];
				System.arraycopy(data, 1, dataText, 0, dataText.length);
				throw new IOException("Unable to connect: "+new String(dataText, "UTF-8"));

			}
			else{

				state = ConnectionState.STATE_CONNECTED;
				initReceiveThread();
				initPingThread();
				initRelyThread();

				reliableThread.start();
				receiveThread.start();
				pingThread.start();

				System.out.println("[RUDPClient] Connected !");

			}
		} catch (IOException e) {
			state = ConnectionState.STATE_DISCONNECTED;
			throw e;
		}
	}

	public void disconnect() {
		disconnect("Disconnected Manually");
	}
	
	public void disconnect(String reason) {
		if(state == ConnectionState.STATE_DISCONNECTED || state == ConnectionState.STATE_DISCONNECTING) return;
		byte[] reponse = reason.getBytes(StandardCharsets.UTF_8);
		
		if(type == ClientType.SERVER_CHILD){
			sendReliablePacket(RUDPConstants.PacketType.DISCONNECT_FROMSERVER, reponse);
			state = ConnectionState.STATE_DISCONNECTING;
		}
		if(type == ClientType.NORMAL_CLIENT){
			sendPacket(RUDPConstants.PacketType.DISCONNECT_FROMCLIENT, reponse);
			state = ConnectionState.STATE_DISCONNECTED;
			socket.close();
		}
		if(packetHandler != null) packetHandler.onDisconnectedByLocal(reason);
	}

	public void sendReliablePacket(byte[] data){
		sendReliablePacket(RUDPConstants.PacketType.RELIABLE, data);
	}

	public void sendReliablePacket(byte packetType, byte[] data){
		if(state == ConnectionState.STATE_DISCONNECTED) return;
		byte[] packet = new byte[data.length+3];
		long timeMS = System.currentTimeMillis();
		short seq = getReliablePacketSequence();

		//byte[] dp = new byte[8];
		//BytesUtils.writeBytes(dp, 0, timeNS);
		//System.out.println("[RUDPClient] reliable packet sent "+toStringRepresentation(dp)+" - "+timeNS);

		packet[0] = packetType;
		NetUtils.writeBytes(packet, 1, seq);
		System.arraycopy(data, 0, packet, 3, data.length);
		if(type == ClientType.SERVER_CHILD) server.sendPacket(packet, address, port);
		else{
			DatagramPacket dpacket = new DatagramPacket(packet, packet.length, address, port);
			try {
				socket.send(dpacket);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		synchronized(packetsSent){
			packetsSent.add(new ReliablePacket(seq, timeMS, packet));
		}
		sentReliable++;
	}

	public void sendPacket(byte[] data){
		sendPacket(RUDPConstants.PacketType.UNRELIABLE, data);
	}
	
	public void requestRemoteStats() {
		sendPacket(RUDPConstants.PacketType.PACKETSSTATS_REQUEST, new byte[0]);
	}
	
	/* Helper Methods */
	void initialize() {
		initRelyThread();
		reliableThread.start();
		state = ConnectionState.STATE_CONNECTED;
		packetHandler.onConnection();
	}

	private void initReceiveThread() {
		receiveThread = new Thread(()->{
			while(state == ConnectionState.STATE_CONNECTED){
				byte[] buffer = new byte[RUDPConstants.RECEIVE_MAX_SIZE];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

				try {
					socket.receive(packet);
				} catch (SocketTimeoutException e) {
					state = ConnectionState.STATE_DISCONNECTED;
					disconnected("Connection timed out");
					return;
				} catch (IOException e) {
					if(state == ConnectionState.STATE_DISCONNECTED) return;
					System.err.println("[RUDPClient] An error as occured while receiving a packet: ");
					e.printStackTrace();
				}
				byte[] data = new byte[packet.getLength()];
				System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
				try {
					handlePacket(data);
				}
				catch(Exception e) {
					System.err.print("[RUDPClient] An error occured while handling packet:");
					e.printStackTrace();
				}
				packet.setLength(RUDPConstants.RECEIVE_MAX_SIZE);
			}
		}, "RUDPClient receive thread");
	}

	private void initPingThread() {
		pingThread = new Thread(()->{
			try {
				while(state == ConnectionState.STATE_CONNECTED){
					byte[] pingPacket = new byte[8];
					NetUtils.writeBytes(pingPacket, 0, System.currentTimeMillis());
					sendPacket(RUDPConstants.PacketType.PING_REQUEST, pingPacket);

					Thread.sleep(RUDPConstants.PING_INTERVAL);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}, "RUDPClient ping thread");
	}

	private void initRelyThread() {
		reliableThread = new Thread(()-> {
			try {
				while(state == ConnectionState.STATE_CONNECTING || state == ConnectionState.STATE_CONNECTED || (state == ConnectionState.STATE_DISCONNECTING && !packetsSent.isEmpty())){
					synchronized(packetsSent){
						long currentMS = System.currentTimeMillis();
						long minMS = currentMS+(latency*2);
						int i=0;
						while(i<packetsSent.size()){
							ReliablePacket rpacket = packetsSent.get(i);

							//byte[] dp = new byte[8];
							//BytesUtils.writeBytes(dp, 0, rpacket.dateNS);

							if(rpacket.dateMS+RUDPConstants.PACKET_TIMEOUT_TIME_MILLISECONDS < currentMS){
								System.out.println("[RUDPClient] Packet "+rpacket.seq+" dropped");
								packetsSent.remove(i);
								continue;
							}
							if(rpacket.minDateMS < currentMS){
								rpacket.minDateMS = minMS;
								sendPacketRaw(rpacket.data);
								//System.out.println("[RUDPClient] Sending reliable packet again "+rpacket.dateNS/*toStringRepresentation(dp)*/);
							}
							i++;
						}
					}

					if(state == ConnectionState.STATE_DISCONNECTING && packetsSent.size() == 0) {
						state = ConnectionState.STATE_DISCONNECTED;
						server.remove(this);
						return;
					}
					Thread.sleep(20);
				}
				state = ConnectionState.STATE_DISCONNECTED;
				if(type == ClientType.SERVER_CHILD) server.remove(instance);
			} catch (InterruptedException e) {e.printStackTrace();}
		}, "RUDPClient rely thread");
	}

	void disconnected(String reason) {
		state = ConnectionState.STATE_DISCONNECTED;
		if(packetHandler != null) packetHandler.onDisconnectedByRemote(reason);
		if(type == ClientType.SERVER_CHILD) server.remove(this);
	}

	void sendPacket(byte packetType, byte[] data){
		if(state == ConnectionState.STATE_DISCONNECTED) return;
		byte[] packet = new byte[data.length+3];
		short seq = getUnreliablePacketSequence();
		
		packet[0] = packetType;
		NetUtils.writeBytes(packet, 1, seq);
		System.arraycopy(data, 0, packet, 3, data.length);

		if(type == ClientType.SERVER_CHILD) server.sendPacket(packet, address, port);
		else{
			DatagramPacket dpacket = new DatagramPacket(packet, packet.length, address, port);
			try {
				socket.send(dpacket);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		sent++;
	}

	/**
	 * Handles received packet assuming server won't send any empty packets. (data.len != 0)
	 * @param data Header and payload of received packet
	 */
	void handlePacket(byte[] data) {
		if((state == ConnectionState.STATE_DISCONNECTING || state == ConnectionState.STATE_DISCONNECTED) && data[0] != RUDPConstants.PacketType.RELY) return;
		//System.out.println("Received Packet: " + NetUtils.asHexString(data)); //Debug received packet

		lastPacketReceiveTime = System.currentTimeMillis(); //Assume packet received when handling started

		//Counter
		if(RUDPConstants.isPacketReliable(data[0])) {
			
			//System.out.println("RELIABLE");
			
			//Send rely packet
			byte[] l = new byte[]{data[1], data[2]};
			sendPacket(RUDPConstants.PacketType.RELY, l);

			//save to received packet list
			short seq = NetUtils.asShort(data, 1);
			Long currentTime = System.currentTimeMillis();
			Long packetOverTime = currentTime+RUDPConstants.PACKET_STORE_TIME_MILLISECONDS;
			
			Iterator<Entry<Short, Long>> it = packetsReceived.entrySet().iterator();
			while(it.hasNext()){
				Entry<Short, Long> storedSeq = it.next();
				if(storedSeq.getKey() == seq){
					//System.out.println("[RUDPClient] Packet already received");
					return;
				}
				if(storedSeq.getValue() < currentTime) it.remove(); //XXX use another thread ?
			}
			receivedReliable++;
			packetsReceived.put(seq, packetOverTime);
		}
		else received++;

		if(data[0] == RUDPConstants.PacketType.RELY) {

			//byte[] dp = new byte[] {data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9]};

			//System.out.println("RELY RECEIVED "+toStringRepresentation(dp)+" - "+BytesUtils.toLong(data, 2));
			synchronized(packetsSent){
				for(int i=0;i<packetsSent.size();i++) {
					if(packetsSent.get(i).seq == NetUtils.asShort(data, 3)){
						packetsSent.remove(i);
						//System.out.println("FOUND AND REMOVED FROM LIST");
						return;
					}
				}
				if(state == ConnectionState.STATE_DISCONNECTING && packetsSent.size() == 0) {
					state = ConnectionState.STATE_DISCONNECTED;
					server.remove(this);
				}
			}
			return;
		}
		else if(data[0] == RUDPConstants.PacketType.PING_REQUEST) {
			short seq = NetUtils.asShort(data, 1);
			if(NetUtils.sequence_greater_than(seq, lastPingSeq)) {
				lastPingSeq = seq;
				byte[] l = new byte[]{data[3], data[4], data[5], data[6], data[7], data[8], data[9], data[10]};
				sendPacket(RUDPConstants.PacketType.PING_RESPONSE, l);//sending time received (long) // ping packet format: [IN:] CMD_PING_REPONSE seqId sendMilliseconds
			}
			return;
		}
		else if(data[0] == RUDPConstants.PacketType.PING_RESPONSE) {
			short seq = NetUtils.asShort(data, 1);
			if(NetUtils.sequence_greater_than(seq, lastPingSeq)) {
				lastPingSeq = seq;
				latency = (int) (System.currentTimeMillis() - NetUtils.asLong(data, 3));
				if(latency < 5) latency = 5;
			}
			//System.out.println("latency: "+latency+"ms");
			return;
		}
		else if(data[0] == RUDPConstants.PacketType.DISCONNECT_FROMSERVER){
			byte[] packetData = new byte[data.length-3];
			System.arraycopy(data, 3, packetData, 0, data.length-3);
			disconnected(new String(packetData, StandardCharsets.UTF_8));
			return;
		}
		else if(data[0] == RUDPConstants.PacketType.RELIABLE) {

			//handle reliable packet
			if(packetHandler != null) {
				try {
					packetHandler.onReliablePacketReceived(data); //pass raw packet payload
				}catch(Exception e) {
					System.err.print("[RUDPClient] An error occured while handling reliable packet:");
					e.printStackTrace();
				}
				/*try {
					byte[] packetData = new byte[data.length - 9];
					System.arraycopy(data, 9, packetData, 0, packetData.length);
					clientManager.handleReliablePacket(packetData, bl);
				}
				catch(Exception e) {
					System.err.print("[RUDPClient] An error occured while handling reliable packet:");
					e.printStackTrace();
				}*/
			}
		}
		else if(data[0] == RUDPConstants.PacketType.PACKETSSTATS_REQUEST) {
			byte[] packet = new byte[17];
			NetUtils.writeBytes(packet, 0, sent+1); // Add one to count the current packet
			NetUtils.writeBytes(packet, 4, sentReliable);
			NetUtils.writeBytes(packet, 8, received);
			NetUtils.writeBytes(packet, 12, receivedReliable);
			sendPacket(RUDPConstants.PacketType.PACKETSSTATS_RESPONSE, packet);
		}
		else if(data[0] == RUDPConstants.PacketType.PACKETSSTATS_RESPONSE) {
			int sentRemote = NetUtils.asInt(data, 3);
			int sentRemoteR = NetUtils.asInt(data, 7);
			int receivedRemote = NetUtils.asInt(data, 11);
			int receivedRemoteR = NetUtils.asInt(data, 15);
			packetHandler.onRemoteStatsReturned(sentRemote, sentRemoteR, receivedRemote, receivedRemoteR);
		}
		else if(packetHandler != null) {
			try {
				packetHandler.onPacketReceived(data); //pass raw packet payload
			}catch(Exception e) {
				System.err.print("[RUDPClient] An error occured while handling packet:");
				e.printStackTrace();
			}
			/*byte[] packetData = new byte[data.length - 1];
			System.arraycopy(data, 1, packetData, 0, packetData.length);
			clientManager.handlePacket(packetData);*/
		}
		
		//System.out.println(); //debug purposes
	}

	private void sendPacketRaw(byte[] data){
		if(state == ConnectionState.STATE_DISCONNECTED) return;
		if(type == ClientType.SERVER_CHILD) server.sendPacket(data, address, port);
		else{
			DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
			try {
				socket.send(packet);
			} catch (IOException e) {e.printStackTrace();}
		}
	}
}
