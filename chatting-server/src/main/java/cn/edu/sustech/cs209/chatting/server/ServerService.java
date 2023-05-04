package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ServerService implements Runnable {
    private Socket socket;
    private ServerData serverData;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String name;

    public ServerService(Socket socket, ServerData serverData) {
        this.socket = socket;
        this.serverData = serverData;
    }

    private void service() throws IOException, ClassNotFoundException {
        ois = new ObjectInputStream(socket.getInputStream());
        Message m = (Message) ois.readObject();
        switch (m.getType()) {
            case "All":
                name = m.getData();
                serverData.getUsers().put(name, socket);
                System.out.println("Client " + name + " connect");
                updateUsers();
                break;
            case "One":
                send_message(m);
                break;
            case "Group":
                createGroup(m);
        }
    }

    private void check_name() throws IOException {
        List<String> users = new ArrayList<>(serverData.getUsers().keySet());
        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(users);
        oos.flush();
    }

    private void updateUsers() throws IOException {
        List<String> users = new ArrayList<>(serverData.getUsers().keySet());
        for (Socket s : serverData.getUsers().values()) {
            oos = new ObjectOutputStream(s.getOutputStream());
            oos.writeObject("1");
            oos.flush();

            oos = new ObjectOutputStream(s.getOutputStream());
            oos.writeObject(users);
            oos.flush();
        }
    }

    private void createGroup(Message m) throws IOException {
        List<String> members = new ArrayList<>(
            Arrays.asList(m.getData().substring(1, m.getData().length() - 1).trim().split(", ")));
        serverData.getGroup().put(toString(members), members);
        for (String s : members) {
            oos = new ObjectOutputStream(serverData.getUsers().get(s).getOutputStream());
            oos.writeObject("3");
            oos.flush();

            oos = new ObjectOutputStream(serverData.getUsers().get(s).getOutputStream());
            oos.writeObject(toString(members));
            oos.flush();
        }
    }

    private void send_message(Message m) throws IOException {
        if (serverData.getGroup().containsKey(m.getSendTo())) {
            for (String name : serverData.getGroup().get(m.getSendTo())) {
                if (!Objects.equals(name, m.getSentBy())) {
                    Socket s = serverData.getUsers().get(name);
                    m.setType("Group");
                    oos = new ObjectOutputStream(s.getOutputStream());
                    oos.writeObject("2");
                    oos.flush();

                    oos = new ObjectOutputStream(s.getOutputStream());
                    oos.writeObject(m);
                    oos.flush();
                }
            }
        } else {
            Socket s = serverData.getUsers().get(m.getSendTo());
            oos = new ObjectOutputStream(s.getOutputStream());
            oos.writeObject("2");
            oos.flush();

            oos = new ObjectOutputStream(s.getOutputStream());
            oos.writeObject(m);
            oos.flush();
        }
    }

    public String toString(List<String> list) {
        StringBuilder s = new StringBuilder();
        if (list.size() <= 3) {
            for (String value : list) {
                s.append(value);
                s.append(", ");
            }
            s.delete(s.length() - 2, s.length());
        } else {
            s.append(list.get(0)).append(", ").append(list.get(1)).append(", ").append(list.get(2))
                .append("...");
        }
        return s.toString();
    }

    @Override
    public void run() {
        try {
            check_name();
            while (socket.isConnected()) {
                service();
            }
        } catch (IOException | ClassNotFoundException e) {
            if (name != null) {
                serverData.getUsers().remove(name);
                for (String s : serverData.getGroup().keySet()) {
                    serverData.getGroup().get(s).remove(name);
                }
                System.out.println("Client " + name + " disconnect");
                try {
                    updateUsers();
                } catch (IOException ex) {
                    e.printStackTrace();
                }
            }
        }
    }
}
