package com.orbitmessenger.Controllers;

import java.net.URI;
import java.nio.ByteBuffer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

public class WSClient extends WebSocketClient {

    public StringBuilder allMessages = new StringBuilder();

    public WSClient(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public WSClient(URI serverURI) {
        super(serverURI);
    }

    public JsonObject submitObject;

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        send("{\"action\":\"getAllMessages\"}");
        //send("Hello, it is me. Mario :)");
        System.out.println("new connection opened");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        allMessages.append(message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        System.out.println("received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("an error occurred:" + ex);
    }

    public String formatMessage(String message) {
        String formattedMessage = "{}";

        return formattedMessage;
    }

    public boolean isAllMessagesEmpty() {
        return allMessages == null;
    }

    public JsonArray getAllMessages() {
        try {
            //System.out.println("All Messages: " + allMessages.toString());
            JsonParser parser = new JsonParser();
            JsonElement tradeElement = parser.parse(allMessages.toString());
            JsonArray trade = tradeElement.getAsJsonArray();
            //System.out.println(trade);
            return trade;
        } catch (Exception e) {
            return null;
        }
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
        submitObject.addProperty("properties", String.valueOf(properties));
        return submitObject;
    }
}