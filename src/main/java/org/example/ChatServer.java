package org.example;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;

public class ChatServer {
	private static JFrame serverFrame;
	private static JTextArea logArea;
	private static class ClientHandler implements Runnable {
		private Socket socket;
		private PrintWriter out;
		private BufferedReader in;
		private String name;
		private boolean hasSentInitialJoinMessage = false;  // New flag

		public ClientHandler(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			System.out.println("client connected " + socket.getInetAddress());

			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);

				while (true) {
					String message = in.readLine();

					if (message == null) {
						break;
					}

					if (!hasSentInitialJoinMessage) {
						name = message;
						synchronized (connectedClients) {
							if (!name.isEmpty() && !connectedClients.containsKey(name)) {
								connectedClients.put(name, out);
								out.println("NAMEACCEPTED " + name);
								sendUpdatedClientList();
								broadcastMessage(name + " has joined");
								hasSentInitialJoinMessage = true;  // Set the flag after broadcasting join message
								continue;  // Skip the rest of the loop to avoid broadcasting the name again
							} else {
								out.println("INVALIDNAME");
							}
						}
					} else {
						if (message.toLowerCase().equals("/quit")) {
							break;
						}
						if (message.startsWith("ALL:")) {
							synchronized (connectedClients) {
								broadcastMessage(name + ": " + message.substring(4));
							}
						} else if (message.startsWith("PRIVATE:")) {
							String[] parts = message.split(":", 3);
							String targetUser = parts[1];
							String privateMessage = parts[2];
							synchronized (connectedClients) {
								sendPrivateMessage(targetUser, name + ": " + privateMessage);
							}
						}
					}
				}
			} catch (Exception e) {
				System.out.println(e);
			}

			finally {
				// Remove client from map on disconnect or user quit
				// - disconnect results in a caught exception which is printed
				// to
				// server console
				// - quit results from leaving client input loop, thread is then
				// terminated, disconnecting the client
				if (name != null) {
//					System.out.println(name + " is leaving");

					connectedClients.remove(name);
					sendUpdatedClientList();
					synchronized (connectedClients) {
						broadcastMessage(name + " has left");
					}
				}
			}
		}

		private void sendPrivateMessage(String targetUser, String message) {
			log(message);
			PrintWriter targetOut = connectedClients.get(targetUser);
			if (targetOut != null) {
				targetOut.println(message);
			}
		}

	}
//----------------------

	// Map of clients connected to the server
	private static HashMap<String, PrintWriter> connectedClients = new HashMap<>();

	// Set maximum amount of connected clients
	private static final int MAX_CONNECTED = 50;
	// Server port
	private static final int PORT = 59002;
	private static ServerSocket listener;

	// Broadcast to all clients in map by writing to their output streams
	private static void broadcastMessage(String message) {
		log(message);
		for (PrintWriter p : connectedClients.values())
		{
			p.println(message);
		}
	}

	public static void start() {
		setupServerUI();

		try {
			listener = new ServerSocket(PORT);

			log("Server started on port: " + PORT);
			log("Now listening for connections ..." + PORT);

			// client connection loop to accept new socket connections
			while (true) {
				// limit to a maximum amount of connected clients (a client
				// could disconnect, allowing a new connection)
				if (connectedClients.size() <= MAX_CONNECTED) {
					// dispatch a new ClientHandler thread to the socket
					// connection
					Thread newClient = new Thread(new ClientHandler(listener.accept()));
					newClient.start();
				}

			}
		} catch (BindException e) {
			// server already started on this port ... continue
		} catch (Exception e) {
			log("\nError occured: \n");
				e.printStackTrace();
			log("\nExiting...");
			}
	}

	public static void stop() throws IOException {
		if (!listener.isClosed())
			listener.close();
	}

	private static void sendUpdatedClientList() {
		StringBuilder clientList = new StringBuilder("CLIENTLIST:");
		for (String clientName : connectedClients.keySet()) {
			clientList.append(clientName).append(",");
		}
		synchronized (connectedClients) {
			broadcastMessage(clientList.toString());
		}
	}

	private static void setupServerUI() {
		serverFrame = new JFrame("Chat Server");
		serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		serverFrame.setSize(600, 400);

		logArea = new JTextArea();
		logArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(logArea);
		serverFrame.add(scrollPane);

		serverFrame.setVisible(true);
	}
	private static void log(String message) {
		SwingUtilities.invokeLater(() -> {
			logArea.append(message + "\n");
		});
	}

	public static void main(String[] args) throws IOException {
		start();
	}
}