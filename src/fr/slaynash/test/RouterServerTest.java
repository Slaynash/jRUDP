package fr.slaynash.test;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import fr.slaynash.communication.handlers.OrderedPacketHandler;
import fr.slaynash.communication.rudp.RUDPClient;
import fr.slaynash.communication.rudp.RUDPServer;

public final class RouterServerTest extends JFrame {
	
	private static final int ST_SERVER_PORT = 1111;
	private static final int ST_PACKETS_PER_SEC = 30;
	
	private static final long serialVersionUID = 0L;	
	private static RouterServerTest gui_instance;
	
	public static class SPacketHandler extends OrderedPacketHandler {

		public SPacketHandler(RUDPClient rudpClient) {
			super(rudpClient);
		}
		
		@Override
		public void initializeClient() {
			String info = rudp.getAddress() + ":" + rudp.getPort();
			System.out.println("[DEBUG]" + info + " has connected!");
			gui_instance.modelConnClients.addElement(info);
		}
		
		@Override
		public void onDisconnectedByRemote(String reason) {
			String info = rudp.getAddress() + ":" + rudp.getPort();
			System.out.println("[DEBUG]" + info + " has disconnected");
			gui_instance.modelConnClients.removeElement(info);
		}
	}
	
	public RUDPServer serverInstance;
	public Timer packetTimer = new Timer();
	
	private DefaultListModel<String> modelConnClients = new DefaultListModel<>();
	private JTextField tfSrvPort;
	private JButton btnStartSrv;
	private JTextArea taConsole;
	
	private RouterServerTest() {
		setResizable(false);
		setTitle("jRUDP Server Test");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(289, 500);
		setLocationRelativeTo(null);
		getContentPane().setLayout(null);
		
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception e) {}
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setEnabled(false);
		scrollPane.setBounds(10, 11, 264, 147);
		getContentPane().add(scrollPane);
		
		JList<String> listConnClients = new JList<>();
		listConnClients.setEnabled(false);
		listConnClients.setModel(modelConnClients);
		scrollPane.setViewportView(listConnClients);
		
		JLabel lblConnClients = new JLabel("Connected Clients");
		scrollPane.setColumnHeaderView(lblConnClients);
		
		tfSrvPort = new JTextField();
		tfSrvPort.setText(ST_SERVER_PORT + "");
		tfSrvPort.setBounds(99, 397, 160, 20);
		getContentPane().add(tfSrvPort);
		tfSrvPort.setColumns(10);
		
		JLabel lblSrvPort = new JLabel("Server Port:");
		lblSrvPort.setBounds(23, 400, 66, 14);
		getContentPane().add(lblSrvPort);
		
		btnStartSrv = new JButton("Start Server!");
		btnStartSrv.addActionListener((action)-> {
			if(serverInstance!=null && serverInstance.isRunning()) { //Server running handle
				packetTimer.cancel();
				packetTimer.purge();
				serverInstance.stop();
				btnStartSrv.setEnabled(false);
				btnStartSrv.setText("Server closing..");
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						System.out.println("[INFO]Server stopped");
						btnStartSrv.setEnabled(true);
						btnStartSrv.setText("Start Server!");
					}
				}, 5000);
				modelConnClients.removeAllElements();
			}
			else { //Server not running handle
				try {
					serverInstance = new RUDPServer(Integer.parseInt(tfSrvPort.getText()));
					serverInstance.setClientPacketHandler(SPacketHandler.class); //No handler yet
					serverInstance.start();
					btnStartSrv.setText("Stop Server!");
				}
				catch(SocketException e) {
					System.out.println("[ERROR]Server socket is unable to create.");
					e.printStackTrace();
					System.exit(-1);
				}
				catch(NumberFormatException e) {
					System.out.println("[ERROR]Port should be an integer value");
				}
				
				packetTimer = new Timer();
				packetTimer.schedule(new TimerTask() { //Send every user a packet per X milliseconds
					@Override
					public void run() {
						for(RUDPClient c : serverInstance.getConnectedUsers()) {
							c.sendReliablePacket(new byte[]{0x01});
						}
					}
				}, 0, 1000 / ST_PACKETS_PER_SEC);
			}
		});
		btnStartSrv.setBounds(23, 428, 236, 23);
		getContentPane().add(btnStartSrv);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(10, 169, 264, 216);
		getContentPane().add(scrollPane_1);
		
		taConsole = new JTextArea();
		taConsole.setWrapStyleWord(true);
		taConsole.setLineWrap(true);
		taConsole.setEditable(false);
		taConsole.setBackground(Color.LIGHT_GRAY);
		taConsole.setFont(new Font("SansSerif", Font.BOLD, 11));
		scrollPane_1.setViewportView(taConsole);
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
	}
	
	/* Unique Main Method */
	public static void main(String[] args) {
		gui_instance = new RouterServerTest();
	}
}
