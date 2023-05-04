package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;

public class Message implements Serializable {

    private String type;
    private String sentBy;
    private String sendTo;
    private String data;

    public Message(String type, String sentBy, String sendTo, String data) {
        this.type = type;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

    public void setType(String type) {
        this.type = type;
    }
}
