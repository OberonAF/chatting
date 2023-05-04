package cn.edu.sustech.cs209.chatting.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws IOException {
        final int serverPort = 8888;
        System.out.println("Starting server");
        ServerSocket serverSocket = new ServerSocket(serverPort);
        ServerData serverData = new ServerData();
        System.out.println("Waiting...");


        while (true) {
            Socket socket=serverSocket.accept();
            Thread t = new Thread(new ServerService(socket, serverData));

            t.start();
        }
    }
}
