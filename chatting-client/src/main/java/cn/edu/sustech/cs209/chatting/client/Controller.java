package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class Controller implements Initializable {
    final Socket socket=new Socket("localhost", 8888);

    @FXML
    ListView<Message> chatContentList;

    @FXML
    ListView<String> chatList=new ListView<>();

    @FXML
    TextArea inputArea=new TextArea();

    @FXML
    Label currentUsername=new Label();

    @FXML
    Label currentOnlineCnt=new Label();

    String username;
    String toName;
    List<String> online=new ArrayList<>();
    List<String> not_read=new ArrayList<>();
    Map<String, List<Message>> history=new HashMap<>();

    public Controller() throws IOException {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            ObjectInputStream ois=new ObjectInputStream(socket.getInputStream());
            online = (List<String>) ois.readObject();
        } catch (IOException | ClassNotFoundException ignored) {
        }

        Dialog<String> dialog=new TextInputDialog();
        dialog.setTitle("Login");
        dialog.setHeaderText(null);
        dialog.setContentText("Username:");

        Optional<String> input=dialog.showAndWait();
        if (input.isPresent() && !input.get().isEmpty()) {
            while (input.isPresent() && online.contains(input.get())) {
                warning("Name exists!");
                input = dialog.showAndWait();
            }

            while (input.isPresent() && input.get().contains(",")) {
                warning("Illegal name format!");
                input=dialog.showAndWait();
            }

            username=input.get();
            currentUsername.setText("Current Username: " + username);
        } else {
            System.out.println("Invalid username " + input + ", exiting");
            Platform.exit();
        }

        if (username != null) {
            try {
                send_name();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        chatList.setCellFactory(new ClientCellFactory());
        chatContentList.setCellFactory(new MessageCellFactory());
        inputArea.setEditable(false);
    }

    @FXML
    public void createPrivateChat() {
        AtomicReference<String> user=new AtomicReference<>();

        Stage stage = new Stage();
        ComboBox<String> userSel=new ComboBox<>();

        userSel.getItems().addAll(online);

        Button okBtn=new Button("OK");
        okBtn.setOnAction(e -> {
            user.set(userSel.getSelectionModel().getSelectedItem());
            stage.close();
        });

        HBox box=new HBox(60);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.getChildren().addAll(userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        if (userSel.getValue() != null) {
            toName=userSel.getValue();
            inputArea.setEditable(true);
            chatContentList.getItems().clear();
            chatContentList.getItems().addAll(history.get(toName));

            if (!chatList.getItems().contains(toName)) {
                chatList.getItems().add(0, toName);
            }
        }
    }

    @FXML
    public void createGroupChat() {
        List<String> group=new ArrayList<>();
        List<String> selected=new ArrayList<>();
        Stage stage=new Stage();
        VBox vbox=new VBox(5);
        for (String s : online) {
            CheckBox cb=new CheckBox(s);
            vbox.getChildren().add(cb);
            cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observableValue,
                    Boolean aBoolean, Boolean t1) {
                    if (cb.isSelected()) {
                        selected.add(cb.getText());
                    } else selected.remove(cb.getText());
                }
            });
        }

        Button okBtn=new Button("OK");
        okBtn.setOnAction(e -> {
            group.add(username);
            group.addAll(selected);
            if (group.size() > 1){
                if (history.containsKey(toString(group))) {
                    toName=toString(group);
                    chatContentList.getItems().clear();
                    chatContentList.getItems().addAll(history.get(toName));
                } else {
                    try {
                        send_group(group.toString());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            stage.close();
        });

        HBox hbox=new HBox(vbox, okBtn);
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(20, 20, 20, 20));
        stage.setScene(new Scene(hbox));
        stage.showAndWait();

        if (group.size() > 1) {
            chatContentList.getItems().clear();
            inputArea.setEditable(true);
            toName=toString(group);
        }
    }

    @FXML
    public void doSendMessage() throws IOException {
        if (inputArea.isEditable()) {
            if (inputArea.getText().length() > 0) {
                Message m=new Message("One", username, toName, inputArea.getText());
                chatContentList.getItems().add(m);
                history.get(toName).add(m);
                inputArea.setText("");
                send_message(m);
                sort(m.getSendTo());
            }
        } else {
            warning("Please create a new chat.");
        }
    }

    private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {

        @Override
        public ListCell<Message> call(ListView<Message> param) {
            return new ListCell<Message>() {
                @Override
                public void updateItem(Message msg, boolean empty) {
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }

                    HBox wrapper=new HBox();
                    Label nameLabel=new Label(msg.getSentBy());
                    Label msgLabel=new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        nameLabel.setTextFill(Color.web("#0066CC"));
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                        msgLabel.setWrapText(true);
                    } else {
                        nameLabel.setTextFill(Color.web("#CC9999"));
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                        msgLabel.setWrapText(true);
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }

    private class ClientCellFactory implements Callback<ListView<String>, ListCell<String>> {

        @Override
        public ListCell<String> call(ListView<String> param) {
            return new ListCell<String>() {
                @Override
                public void updateItem(String name, boolean empty) {
                    super.updateItem(name, empty);
                    if (empty || Objects.isNull(name)) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }

                    HBox wrapper=new HBox();
                    Label nameLabel=new Label(name);
                    Label conLabel=new Label();
                    Label markLabel=new Label();

                    wrapper.getChildren().addAll(conLabel, nameLabel);

                    nameLabel.setPrefSize(130, 20);
                    nameLabel.setWrapText(true);
                    conLabel.setPrefSize(60, 20);
                    markLabel.setPrefSize(30, 5);
                    markLabel.setStyle("-fx-background-color: #FF0000");


                    wrapper.addEventFilter(MouseDragEvent.MOUSE_CLICKED, e -> {
                        toName=nameLabel.getText();
                        chatContentList.getItems().clear();
                        chatContentList.getItems().addAll(history.get(toName));
                        inputArea.setEditable(true);
                        not_read.remove(toName);
                        wrapper.getChildren().remove(markLabel);
                    });

                    if (!name.contains(",")) {
                        if (online.contains(name)) {
                            conLabel.setText("[Online]");
                        } else conLabel.setText("[Offline]");
                    } else {
                        wrapper.getChildren().remove(conLabel);
                        nameLabel.setPrefSize(170, 20);
                    }

                    if (not_read.contains(name) && !wrapper.getChildren().contains(markLabel)) {
                        wrapper.getChildren().add(markLabel);
                    }
                    setGraphic(wrapper);
                }
            };
        }
    }

    private void send_name() throws IOException {
        Message m=new Message("All", username, "server", username);
        ObjectOutputStream oos=new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(m);
        oos.flush();
    }

    private void send_group(String name) throws IOException {
        Message m=new Message("Group", username, "server", name);
        ObjectOutputStream oos=new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(m);
        oos.flush();
    }

    public void sort(String name) {
        chatList.getItems().remove(name);
        chatList.getItems().add(0, name);
    }

    private void send_message(Message m) throws IOException {
        if (m.getSendTo().contains(", ") || online.contains(m.getSendTo())) {
            ObjectOutputStream oos=new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(m);
            oos.flush();
        }
    }

    private void warning(String s) {
        Alert alert=new Alert(AlertType.INFORMATION);
        alert.setTitle(null);
        alert.setHeaderText(null);
        alert.setContentText(s);
        alert.showAndWait();
    }

    public String toString(List<String> list) {
        StringBuilder s=new StringBuilder();
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
}
