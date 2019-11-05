package com.orbitmessenger.Controllers;

import java.net.URI;
import java.nio.ByteBuffer;

import com.google.gson.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

public class WSClient extends WebSocketClient {

    public JsonArray allMessages = new JsonArray();;

    public WSClient(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public WSClient(URI serverURI) {
        super(serverURI);
    }

    public JsonObject submitObject;

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        JsonObject getAllMessages = createSubmitObject("getAllMessages", null, null, null, null);
        send(getAllMessages.toString());
        System.out.println("new connection opened");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        addIncomingMessages(message);
        System.out.println("OnMessage: " + message);
        System.out.println("All Messages: " + allMessages.toString());
    }

    @Override
    public void onMessage(ByteBuffer message) {
        System.out.println("received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("an error occurred:" + ex);
    }

    private void addIncomingMessages(String messages) {
        JsonParser parser = new JsonParser();
        JsonArray messageArray = (JsonArray) parser.parse(messages);
        allMessages.addAll(messageArray);
    }

    public boolean isAllMessagesEmpty() {
        return allMessages == null;
    }

    public JsonArray getAllMessages() {
        try {
            return allMessages;
        } catch (Exception e) {
            return null;
        }
    }

    public void resetAllMessages() {
        allMessages = new JsonArray();
    }

    public JsonObject createSubmitObject(String action,
                                         String message,
                                         String username,
                                         Integer lastMessageId,
                                         JsonObject properties) {
        submitObject = new JsonObject();
        submitObject.addProperty("action", action);
        submitObject.addProperty("message", message);
        submitObject.addProperty("username", username);
        submitObject.addProperty("lastMessageId", lastMessageId);
        submitObject.add("properties", properties);
        return submitObject;
    }
}