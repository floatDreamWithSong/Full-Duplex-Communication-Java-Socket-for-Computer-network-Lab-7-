package com.ecnu;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Server {
    private static final int BUFFER_SIZE = 8192;
    private static final List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Server <port>");
            return;
        }
        try {
                int port = Integer.parseInt(args[0]);
                if(port < 1024 || port > 65535) {
                    System.out.println("port number should be between 1024 and 65535");
                    return;
                }
                ServerSocket var2 = new ServerSocket(port);
                System.out.println("Server is running on port: " + port);
                Thread serverInputStream = new Thread(() -> {
                    try (BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in))) {
                        while(true) {
                            String message = stdin.readLine();
                            if (message != null && !message.isEmpty()) {
                                broadcast("server broadcast: " + message);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("server input error: " + e.getMessage());
                    }
                });
                serverInputStream.start();

                while(true) {
                    Socket clientSocket = var2.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    (new Thread(clientHandler)).start();
                    System.out.println("new connection established, total count: " + clients.size());
                }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }

    }

    private static void broadcast(String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        synchronized(clients) {
            Iterator<ClientHandler> iterator = clients.iterator();

            while(iterator.hasNext()) {
                ClientHandler clientHandler = iterator.next();

                try {
                    clientHandler.sendMessage(data);
                } catch (IOException e) {
                    System.out.println("Client disconnected: " + e.getMessage());
                    iterator.remove();
                }
            }

        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            try {
                while(true) {
                    int length = in.readInt();
                    byte[] buffer = new byte[Math.min(length, BUFFER_SIZE)];
                    StringBuilder message = new StringBuilder();

                    int remaining = length;
                    while(remaining > 0) {
                        int read = in.read(buffer, 0, Math.min(remaining, buffer.length));
                        if (read == -1) {
                            break;
                        }
                        String bufferString = new String(buffer, 0, read, StandardCharsets.UTF_8);
                        System.out.println("\n\nreceied buffer:\n\n"+bufferString);
                        message.append(bufferString);
                        remaining -= read;
                    }

                    System.out.println("receive from client: " + message);
                    sendMessage(message.toString().getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                System.out.println("Client disconnect: " + this.socket.getPort());
                synchronized(Server.clients) {
                    Server.clients.remove(this);
                }
            }
        }

        public void sendMessage(byte[] data) throws IOException {
            this.out.writeInt(data.length);
            this.out.write(data);
            this.out.flush();
        }
    }
}