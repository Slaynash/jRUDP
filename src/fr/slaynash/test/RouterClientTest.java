package fr.slaynash.test;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Iterator;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import fr.slaynash.communication.handlers.OrderedPacketHandler;
import fr.slaynash.communication.rudp.Packet;
import fr.slaynash.communication.rudp.RUDPClient;
import fr.slaynash.communication.utils.NetUtils;
import javax.swing.ScrollPaneConstants;

public class RouterClientTest extends JFrame {

	public static final String ST_SERVER_HOST = "localhost";
	public static final int ST_SERVER_PORT = 1111;

	private static final long serialVersionUID = 0L;	
	private static RouterClientTest gui_instance;

	public static class ClientPacketHandler extends OrderedPacketHandler {

		public static ClientPacketHandler instance = new ClientPacketHandler();

		private ClientPacketHandler() {
			super(null);
		}

		short prevHandled = -1;

		public ClientPacketHandler(RUDPClient rudpClient) {
			super(rudpClient);
		}

		@Override
		public void onReliablePacketReceived(byte[] data) {
			super.onReliablePacketReceived(data);
			gui_instance.lblRecPacketQueue.setText("Received Packet Queue (Front==index#0) (Size:" + reliableQueue.size() + ")");
			gui_instance.modelRecPackets.clear();

			Iterator<Packet> iter = reliableQueue.iterator();
			while(iter.hasNext()) {
				Packet p = iter.next();
				gui_instance.modelRecPackets.addElement(p.toString());
			}
		}

		@Override
		public void handleReliablePacketOrdered(Packet packet) {
			short next = NetUtils.shortIncrement(prevHandled);
			gui_instance.taHandledPacket.setText("Last Handled Packet:"+ lastHandledSeq +"\n" + packet.toString());
			if(packet.getHeader().getSequenceNo() != next) {
				System.out.printf("HANDLING ERROR PREV:%d, CUR:%d", prevHandled, next);
				gui_instance.disconnectWGui();
			}
			prevHandled = next;
		}
		
		@Override
		public void onDisconnectedByRemote(String reason) {
			super.onDisconnectedByRemote(reason);
			System.out.println("[INFO]Disconnected: " + reason);
			prevHandled = Short.MAX_VALUE;
			gui_instance.disconnectWGui();
		}
		
		@Override
		public void onDisconnectedByLocal(String reason) {
			super.onDisconnectedByLocal(reason);
			System.out.println("[INFO]Disconnected.");
			prevHandled = Short.MAX_VALUE;
		}
	}

	public RUDPClient clientInstance;

	public DefaultListModel<String> modelRecPackets = new DefaultListModel<>();
	private JTextField tfServerPort;
	private JTextField tfServerHost;
	private JButton btnConnection;
	private JTextArea taHandledPacket;
	private JTextArea taConsole;
	private JLabel lblRecPacketQueue;

	private RouterClientTest() {
		setResizable(false);
		setTitle("jRUDP Client Test");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(289, 500);
		setLocationRelativeTo(null);
		getContentPane().setLayout(null);

		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception e) {}

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 69, 263, 156);
		getContentPane().add(scrollPane);

		lblRecPacketQueue = new JLabel("Received Packet Queue (Front==index#0)");
		scrollPane.setColumnHeaderView(lblRecPacketQueue);

		JList<String> listPacketQueue = new JList<>();
		listPacketQueue.setEnabled(false);
		listPacketQueue.setModel(modelRecPackets);
		scrollPane.setViewportView(listPacketQueue);

		btnConnection = new JButton("Connect");
		btnConnection.addActionListener((action)->{
			if(clientInstance != null && clientInstance.isConnected()) {
				disconnectWGui();
			}
			else {
				connectWGui();
			}
		});
		btnConnection.setBounds(10, 438, 263, 23);
		getContentPane().add(btnConnection);

		tfServerPort = new JTextField();
		tfServerPort.setText(ST_SERVER_PORT + "");
		tfServerPort.setBounds(96, 407, 177, 20);
		tfServerPort.setColumns(10);
		getContentPane().add(tfServerPort);

		tfServerHost = new JTextField();
		tfServerHost.setText(ST_SERVER_HOST);
		tfServerHost.setColumns(10);
		tfServerHost.setBounds(96, 376, 177, 20);
		getContentPane().add(tfServerHost);

		JLabel lblServerHost = new JLabel("Server Host:");
		lblServerHost.setBounds(23, 379, 71, 14);
		getContentPane().add(lblServerHost);

		JLabel lblServerPort = new JLabel("Server Port:");
		lblServerPort.setBounds(23, 410, 71, 14);
		getContentPane().add(lblServerPort);

		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane_1.setBounds(10, 236, 263, 126);
		getContentPane().add(scrollPane_1);

		taConsole = new JTextArea();
		taConsole.setLineWrap(true);
		taConsole.setWrapStyleWord(true);
		taConsole.setEditable(false);
		taConsole.setBackground(Color.LIGHT_GRAY);
		taConsole.setFont(new Font("SansSerif", Font.BOLD, 11));
		scrollPane_1.setViewportView(taConsole);
		
		taHandledPacket = new JTextArea();
		taHandledPacket.setEditable(false);
		taHandledPacket.setEnabled(false);
		taHandledPacket.setFont(new Font("SansSerif", Font.BOLD, 11));
		taHandledPacket.setText("Last Handled Packet:\r\nnull");
		taHandledPacket.setBounds(10, 11, 263, 47);
		getContentPane().add(taHandledPacket);
		setVisible(true);

		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				taConsole.append("" + (char)b);
				taConsole.setSize(taConsole.getPreferredSize());
				JScrollBar sb = scrollPane_1.getVerticalScrollBar();
				sb.setValue( sb.getMaximum() );
			}
		}));

		System.out.println("[INFO]Console: on");

		setVisible(true);
	}

	private void connectWGui() {
		btnConnection.setText("Trying to connect..");
		btnConnection.setEnabled(false);
		repaint();

		SwingUtilities.invokeLater(()->{
			try {
				InetAddress host = InetAddress.getByName(tfServerHost.getText());
				int port = Integer.parseInt(tfServerPort.getText());

				clientInstance = new RUDPClient(host, port);
				clientInstance.setPacketHandler(ClientPacketHandler.class);
				clientInstance.connect();

			}
			catch(UnknownHostException e) {
				System.out.println("[ERROR]Cannot resolve host.");
			}
			catch(NumberFormatException e) {
				System.out.println("[ERROR]Given port should be an integer.");
			}
			catch(SocketException e) {
				System.out.println("[ERROR]Cannot allocate socket for the client");
			}
			catch(IOException e) {
				System.out.println("[ERROR]Connection timed out");
			} catch (InstantiationException e) {
				System.out.println("[ERROR]Cannot create an instance of the handler");
			} catch (IllegalAccessException e) {
				System.out.println("[ERROR]Cannot access the constructor of the handler");
			}
			finally {
				btnConnection.setText(clientInstance!=null && clientInstance.isConnected() ? "Disconnect" : "Connect");
				btnConnection.setEnabled(true);
			}
		});

	}

	private void disconnectWGui() {
		clientInstance.disconnect("DC");
		btnConnection.setText("Connect");	
	}

	/* Unique Main Method */
	public static void main(String[] args) {
		gui_instance = new RouterClientTest();
	}
}
