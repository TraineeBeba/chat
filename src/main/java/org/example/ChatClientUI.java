package org.example;


import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClientUI {

    // Main frame components
    private JFrame mainFrame;
    private JTextArea chatArea;
    private JTextField messageField;
    private PrintWriter out;
    private BufferedReader in;

    // Login frame components
    private JFrame loginFrame;
    private JTextField nicknameField;
    private String clientNickname; // New member to store client's nickname
    private JComboBox<String> userSelector;
    // Networking components
    private Socket clientSocket;

    public ChatClientUI(String serverAddress, int port) {
        try {
            clientSocket = new Socket(serverAddress, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Initialize frames
        initializeLoginFrame();
        initializeMainFrame();
        startListening();
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("NAMEACCEPTED")) {
                        String[] parts = line.split(" ", 2);
                        clientNickname = parts[1]; // Store the client's nickname
                        mainFrame.setTitle("Chat Application - " + parts[1]);
                        loginFrame.setVisible(false);
                        mainFrame.setVisible(true);
                    } else if (line.startsWith("CLIENTLIST:")) {
                        String finalLine = line;
                        SwingUtilities.invokeLater(() -> updateClientList(finalLine.substring(11)));
                    } else {
                        chatArea.append(line + "\n"); // Display other messages in chat area
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void initializeLoginFrame() {
        loginFrame = new JFrame("Login");
        loginFrame.setLayout(new FlowLayout());
        loginFrame.setSize(300, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        nicknameField = new JTextField(20);
        JButton submitButton = new JButton("Submit");

        submitButton.addActionListener(e -> {
            String nickname = nicknameField.getText();
            out.println(nickname);
            mainFrame.setTitle("Chat Application - " + nickname);
            loginFrame.setVisible(false);
            mainFrame.setVisible(true);
        });

        loginFrame.add(new JLabel("Enter Nickname:"));
        loginFrame.add(nicknameField);
        loginFrame.add(submitButton);
    }

    private void initializeMainFrame() {
        mainFrame = new JFrame("Chat Application");
        mainFrame.setLayout(new BorderLayout());
        mainFrame.setSize(500, 400);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        messageField = new JTextField(30);
        JButton sendButton = new JButton("Send");

        mainFrame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JPanel southPanel = new JPanel(new FlowLayout());
        southPanel.add(messageField);
        southPanel.add(sendButton);
        mainFrame.add(southPanel, BorderLayout.SOUTH);

        userSelector = new JComboBox<>(new String[]{"Send to All"});
        southPanel.add(userSelector);

        messageField.addActionListener(e -> sendButtonActionPerformed());
        sendButton.addActionListener(e -> {
            sendButtonActionPerformed();
        });
    }

    private void sendButtonActionPerformed() {
        String selectedUser = (String) userSelector.getSelectedItem();
        String message = messageField.getText();

        if (selectedUser.equals("Send to All")) {
            out.println("ALL:" + message);
        } else {
            out.println("PRIVATE:" + selectedUser + ":" + message);
        }

        messageField.setText("");
    }

    private void updateClientList(String clientListStr) {
        String[] clients = clientListStr.split(",");
        userSelector.removeAllItems();
        userSelector.addItem("Send to All");
        for (String client : clients) {
            if (!client.isEmpty() && !client.equals(clientNickname)) { // Exclude client's own name
                userSelector.addItem(client);
            }
        }
    }


    public static void main(String[] args) {
        String serverAddress = args.length > 0 ? args[0] : "localhost";
        int port = 59002; // Default port or you can get it from args
        SwingUtilities.invokeLater(() -> new ChatClientUI(serverAddress, port).loginFrame.setVisible(true));
    }
}
