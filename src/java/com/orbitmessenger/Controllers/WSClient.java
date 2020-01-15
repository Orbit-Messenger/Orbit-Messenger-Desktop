package com.orbitmessenger.Controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketServerFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.DefaultWebSocketServerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.nio.ByteBuffer;

public class WSClient extends WebSocketClient {

    public final Object monitor = new Object();
    public final Object latencyMonitor = new Object();
    public JsonObject serverResponse;
    public JsonObject submitObject;
    private String username;
    private String password;
    public JsonObject PreferencesObject;
    public long receivedMillis = System.currentTimeMillis();
    public long sentMillis = System.currentTimeMillis();
    public long latency = 0;

    final public String PREF_LOC = "src/java/com/orbitmessenger/preferences/preferences.json";

    private WebSocketServerFactory wsf = new DefaultWebSocketServerFactory();

    public WSClient(URI serverUri, String username, String password, Draft draft) {
        super(serverUri, draft);
        this.username = username;
        this.password = password;
    }

    public WSClient(URI serverURI, String username, String password) {
        super(serverURI);
        this.username = username;
        this.password = password;
        this.connect();
        System.out.println("past wsClient");
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        send(createLoginObject(this.username, this.password).toString());
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

    @Override
    public void onWebsocketPong(WebSocket conn, Framedata f) {
        synchronized (latencyMonitor) {
            super.onWebsocketPong(conn, f);
            receivedMillis = System.currentTimeMillis();
            latency = receivedMillis - sentMillis;
            latencyMonitor.notify();
        }
    }

    @Override
    public void onWebsocketPing(WebSocket conn, Framedata f) {
        super.onWebsocketPing(conn, f);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        System.out.println("received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("an error occurred:" + ex);
    }

    private void setServerResponse(String message){
        JsonObject json = (JsonObject) new JsonParser().parse(message);
       this.serverResponse = json;
    }

    public void sendAPing() {
        sentMillis = System.currentTimeMillis();
        sendPing();
    }

    public long getLatency() {
        long localLatency = this.latency;
        this.latency = 0;
        return localLatency;
    }

    public JsonObject getServerResponse(){
        JsonObject response = this.serverResponse;
        this.serverResponse = new JsonObject();
        return response;
    }

    private JsonObject createLoginObject(String username, String password){
        submitObject = new JsonObject();
        submitObject.addProperty("action", "login");
        submitObject.addProperty("username", username);
        submitObject.addProperty("password", password);
        submitObject.add("properties", getPreferences());
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

    public JsonObject getPreferences() {
        Object returnedPreferences = readPreferencesFile();
        PreferencesObject = (JsonObject) new JsonParser().parse(returnedPreferences.toString());
        return PreferencesObject;
    }

    private Object readPreferencesFile() {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(this.PREF_LOC));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Gson gson = new Gson();
        Object json = gson.fromJson(bufferedReader, Object.class);

        return json;
    }


    public JsonArray getMessagesFromJsonObject(JsonObject serverResponse) {
        String jsonkey = "messages";
        if (serverResponse.has(jsonkey)) {
            try {
                return serverResponse.getAsJsonArray(jsonkey);
            } catch (Exception e) {
                System.out.println("Error getting messages from JsonObject");
                return null;
            }
        }
        return null;
    }

    /**
     * Gets the activeRoom index from the json object passed to it
     */
    public JsonArray getRoomsFromJsonObject(JsonObject serverResponse) {
        String jsonKey = "chatrooms";
        if (serverResponse.has(jsonKey)) {
            try {
                return serverResponse.getAsJsonArray(jsonKey);
            } catch (Exception e) {
                System.out.println("Erorr getting rooms from JsonObject");
                return null;
            }
        }
        return null;
    }

    /**
     * Gets the activeUser index from the json object passed to it
     */
    public JsonArray getUsersFromJsonObject(JsonObject serverResponse) {
        String jsonKey = "allUsers";
        if (serverResponse.has(jsonKey)) {
            try {
                return serverResponse.getAsJsonArray(jsonKey);
            } catch (Exception e) {
                System.out.println("Error getting users from JsonObject");
                return null;
            }
        }
        return null;
    }







}