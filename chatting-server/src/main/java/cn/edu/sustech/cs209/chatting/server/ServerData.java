package cn.edu.sustech.cs209.chatting.server;


import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerData {
    private Map<String, Socket> users = new HashMap<>();
    private Map<String, List<String>> group = new HashMap<>();

    public Map<String, Socket> getUsers() {
        return users;
    }

    public Map<String, List<String>> getGroup() {
        return group;
    }
}
