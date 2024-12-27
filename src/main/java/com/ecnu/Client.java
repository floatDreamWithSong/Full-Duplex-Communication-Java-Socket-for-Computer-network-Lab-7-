package com.ecnu;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {
    private static final int BUFFER_SIZE = 8192;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Client <hostname> <port>");
        } else {
            String hostname = args[0];
            if (hostname.equals("localhost")) {
                hostname = "127.0.0.1";
            }

            try {
                int port = Integer.parseInt(args[1]);
                Socket socket = new Socket(hostname, port);
                System.out.println("connected to " + hostname + ":" + port);
                System.out.println("input format");
                System.out.println("--text <text>  : enter twice to send <text>");
                System.out.println("--file <path>  : read file from <path> and send to server");
                Thread sendThread = new Thread(() -> {
                    try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

                            while(true) {
                                System.out.print(">>> ");
                                String line = stdin.readLine();
                                if (line != null) {
                                    String[] parts = line.trim().split("\\s+", 2);
                                    if (parts.length < 2) {
                                        System.out.println("wrong input format");
                                        continue;
                                    }
                                        switch (parts[0]) {
                                            case "--text":
                                                handleTextInput(out, stdin, parts[1]);
                                                break;
                                            case "--file":
                                                handleFileInput(parts[1], out);
                                                break;
                                            default:
                                                System.out.println("unknown command, using --text or --file");
                                        }

                                }
                            }

                    } catch (IOException e) {
                        System.err.println("An Error occurred when sending : " + e.getMessage());
                    }
                });
                Thread receiveThread = new Thread(() -> {
                    try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
                        while(true) {
                            int length = in.readInt();
                            byte[] data = new byte[length];
                            in.readFully(data);
                            System.out.println("receive message from server: " + new String(data, "UTF-8"));
                        }
                    } catch (IOException e) {
                        System.err.println("An error occurred when receiving message: " + e.getMessage());
                    }
                });
                sendThread.start();
                receiveThread.start();
            } catch (NumberFormatException e) {
                System.err.println("invalid port number" + e.getMessage());
            } catch (IOException e) {
                System.err.println("connection error: " + e.getMessage());
            }

        }
    }

    private static void handleTextInput(DataOutputStream out, BufferedReader stdin, String initialText) throws IOException {
        StringBuilder message = (new StringBuilder(initialText)).append("\n");
        int enterCount = 0;

        while(true) {
            String var5 = stdin.readLine();
            if (var5 == null) {
                break;
            }

            if (var5.isEmpty()) {
                ++enterCount;
                if (enterCount == 1) {
                    byte[] var6 = message.toString().getBytes("UTF-8");
                    out.writeInt(var6.length);
                    out.write(var6);
                    out.flush();
                    break;
                }
            } else {
                enterCount = 0;
                message.append(var5).append("\n");
            }
        }

    }

    private static void handleFileInput(String filepath, DataOutputStream out) throws IOException {
        try (FileInputStream fis = new FileInputStream(filepath)) {
            BufferedInputStream bis = new BufferedInputStream(fis);
                File file = new File(filepath);
                long fileSize = file.length();
                out.writeInt((int)fileSize);
                
                byte[] buffer = new byte[BUFFER_SIZE];

                int bytesRead;
                while((bytesRead = bis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
        } catch (FileNotFoundException e) {
            System.err.println("invalid file path: " + e);
        }

    }
}
