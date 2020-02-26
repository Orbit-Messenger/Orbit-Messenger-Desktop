package com.orbitmessenger.utils;

import com.google.gson.JsonObject;

import java.util.ArrayList;

public class Message {
    private int messageId;
    private String username;
    private String timestamp;
    private String message;

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Message(JsonObject json){
       this.setMessageId(json.get("messageId").getAsInt());
       this.setUsername(json.get("username").toString().replace("\"",""));
        this.setMessage(json.get("message").toString().replace("\"",""));
        this.setTimestamp(json.get("timestamp").toString().replace("\"",""));

    }
}

class MessageStorage{
   private ArrayList<Message> messages = new ArrayList<>();

   public ArrayList<Integer> getAllMessageIds(){
       ArrayList<Integer> messageIds = new ArrayList<>();
       for (Message message: this.messages){
           messageIds.add(message.getMessageId());
       }
       return messageIds;
   }

   public boolean deleteMessage(int index) {
       for (int i = 0; i < messages.size(); i++) {
           if (messages.get(i).getMessageId() == index) {
               messages.remove(i);
               return true;
           }
       }
       return false;
   }
}
