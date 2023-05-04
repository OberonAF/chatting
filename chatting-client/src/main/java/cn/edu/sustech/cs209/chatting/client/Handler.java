package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Handler implements Runnable {
    private Socket socket;
    private Controller controller;
    private ObjectInputStream ois;

    public Handler(Controller controller) {
        this.controller=controller;
        this.socket=controller.socket;
    }
    private void get_name() throws IOException, ClassNotFoundException {
        ois = new ObjectInputStream(socket.getInputStream());
        List<String> users = (List<String>) ois.readObject();
        controller.online.clear();
        for (String name : users) {
            if (!Objects.equals(name, controller.username)) {
                controller.online.add(name);
                if (!controller.history.containsKey(name)) {
                    controller.history.put(name, new ArrayList<>());
                }
            }
        }

        Platform.runLater(() -> {
            controller.chatList.refresh();
            int cnt = controller.online.size() + 1;
            controller.currentOnlineCnt.setText("Online: " + cnt);
        });
    }

    private void get_message() throws IOException, ClassNotFoundException {
        ois = new ObjectInputStream(socket.getInputStream());
        Message m = (Message) ois.readObject();
        if (Objects.equals(m.getType(), "One")) {
            controller.history.get(m.getSentBy()).add(m);

            Platform.runLater(() -> {
                if (Objects.equals(controller.toName, m.getSentBy())) {
                    controller.chatContentList.getItems().add(m);
                } else {
                    controller.not_read.add(m.getSentBy());
                    controller.sort(m.getSentBy());
                }
            });
        } else if (Objects.equals(m.getType(), "Group")) {
            controller.history.get(m.getSendTo()).add(m);

            Platform.runLater(() -> {
                if (Objects.equals(controller.toName, m.getSendTo())) {
                    controller.chatContentList.getItems().add(m);
                } else {
                    controller.not_read.add(m.getSendTo());
                    controller.sort(m.getSendTo());
                }
            });
        }

    }

    private void get_group() throws IOException, ClassNotFoundException {
        ois = new ObjectInputStream(socket.getInputStream());
        String groupName = (String) ois.readObject();
        controller.history.put(groupName, new ArrayList<>());
        Platform.runLater(() -> {
            controller.chatList.getItems().add(0, groupName);
        });
    }

    @Override
    public void run() {

        try {
            while (socket.isConnected()) {
                ois = new ObjectInputStream(socket.getInputStream());
                String type = (String) ois.readObject();
                switch (type) {
                    case "1":
                        get_name();
                        break;
                    case "2":
                        get_message();
                        break;
                    case "3":
                        get_group();
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Prompt");
                alert.setHeaderText(null);
                alert.setContentText("Server closed");
                alert.showAndWait();
            });
        }

    }
}
