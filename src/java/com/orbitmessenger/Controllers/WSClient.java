package com.orbitmessenger.Controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocketServerFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.DefaultWebSocketServerFactory;
import java.net.URI;
import java.nio.ByteBuffer;

public class WSClient extends WebSocketClient {

    public final Object monitor = new Object();
    public JsonObject serverResponse;
    public JsonObject submitObject;
    private String username;

    private WebSocketServerFactory wsf = new DefaultWebSocketServerFactory();

    public WSClient(URI serverUri, String username, Draft draft) {
        super(serverUri, draft);
        this.username = username;
    }

    public WSClient(URI serverURI, String username) {
        super(serverURI);
        this.username = username;
        this.connect();
        System.out.println("past wsClient");
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        send(createLoginObject(this.username).toString());
        System.out.println("new connection opened");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        //send(createLogoutObject().toString());
        System.out.println("closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        synchronized (monitor) {
            setServerResponse(message);
            // This tells the update thread in MainController that yo, its time for an update.
            monitor.notify();
        }
    }

    public int size() {
        return serverResponse.size();
    }

//    @Override
//    public void onWebsocketPong(WebSocket conn, Framedata f) {
//        super.onWebsocketPong(conn, f);
//        System.out.println("PONG: " + conn.getResourceDescriptor().);
//    }

//    @Override
//    public void onWebsocketPing(WebSocket conn, Framedata f) {
//        super.onWebsocketPing(conn, f);
//        System.out.println("PING: " + conn.toString());
//    }

    @Override
    public void onMessage(ByteBuffer message) {
        System.out.println("received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("an error occurred:" + ex);
    }

    private void setServerResponse(String message){
        JsonObject json = (JsonObject) new JsonParser().parse(message);
       this.serverResponse = json;
    }

    public void ping(){
        sendPing();
    }

    public JsonObject getServerResponse(){
        JsonObject response = this.serverResponse;
        this.serverResponse = new JsonObject();
        return response;
    }

    private JsonObject createLoginObject(String username){
        submitObject = new JsonObject();
        submitObject.addProperty("action", "login");
        submitObject.addProperty("username", username);
        return submitObject;
    }

    private JsonObject createLogoutObject(){
        submitObject = new JsonObject();
        submitObject.addProperty("action", "logout");
        return submitObject;
    }

    public JsonObject createSubmitObject(String action,
                                         String chatRoom,
                                         String message,
                                         String username,
                                         JsonObject properties) {
        submitObject = new JsonObject();
        submitObject.addProperty("action", action);
        submitObject.addProperty("chatroom", chatRoom);
        submitObject.addProperty("message", message);
        submitObject.addProperty("username", username);
        submitObject.add("properties", properties);
        return submitObject;
    }
}