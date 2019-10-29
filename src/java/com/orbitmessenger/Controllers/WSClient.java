package com.orbitmessenger.Controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.time.LocalTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletionStage;


public class WSClient {

    private String messages = "";
    private Integer messageCount = 0;
    private String webSocketAddress;

    private Timer timer = new Timer();

    WebSocket webSocket;
    WSClient wsClient;
    HttpClient client;

    public void sendMessage(String message) {
        String action = "{\"action\": \"addMessage\"}";
        String formattedMessage = "{\"message\": \"" + message + "\", \"username\":\"brody\"}";

        webSocket.sendText(action, true);
        webSocket.sendText(formattedMessage, true);
    }

    public JsonArray getAllMessages() {
        JsonArray jsonArray = new JsonArray();
        //try{
            webSocket.sendText("{\"action\": \"getAllMessages\"}", true);

            String json = new String(getMessages());



            //System.out.println("Message: " + json);

            JsonElement jelement = new JsonParser().parse(json.trim());
            jsonArray = jelement.getAsJsonArray();
        //}catch (Exception e){
        //    System.out.println(e);
        //}
        return jsonArray;
    }


    public String getMessages() {
        LocalTime now = LocalTime.now();
        now = now.plusSeconds(5);

        while(LocalTime.now().isBefore(now)){
            if (messages != "") {
                break;
            }
        }

        System.out.println("Messages : " + messages);

        String mp = messages;
        //messages = "";

        return mp;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    WSClient(String server){
        try {
            webSocketAddress = server.replace("http", "ws");

            client = HttpClient.newHttpClient();
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(webSocketAddress + "/ws"), this.wsListener).join();
            //webSocket.sendPing(ByteBuffer.wrap("Ping!".getBytes()));
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        System.out.println(messages);
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            thread.start();
        }catch (Exception e){
            System.out.println(e);
        }
        setIntervalForPing();
    }

    /**
     * Sets the interval of checking for new messages
     */
    private void setIntervalForPing() {
        int begin = 0;
        int timeInterval = 1000;
        timer.schedule(new TimerTask() {
            int counter = 0;
            @Override
            public void run() {
                sendPingToServer();
            }
        }, begin, timeInterval);
    }

    /**
     * Send ping to the server to know we're still here!
     */
    public void sendPingToServer() {
        webSocket.sendPing(ByteBuffer.wrap("Ping!".getBytes()));
    }

    /**
     * Stops the timer
     */
    public void stopIntervalForPing() {
        timer.cancel();
        timer.purge();
    }

    Listener wsListener = new Listener() {
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messages += data.toString();
//            System.out.println("OnText Received: " + data);
//
//            JsonObject jsonObject = new JsonObject();
//            JsonElement jelement = new JsonParser().parse(data.toString());
//            jsonObject = jelement.getAsJsonObject();
//
//            //JsonObject dataObject =  new JsonObject().getAsJsonObject();
//            System.out.println("JSONOBJECT: " + jsonObject.toString());
//            if (!jsonObject.has("messageCount")) {
//                // Update messages!
//                messages += data.toString().trim();
//                System.out.println("Messages: " + messages);
//            } else if (jsonObject.has("messageCount")) {
//                messageCount = jsonObject.getAsInt();
//                System.out.println("Message Count: " + messageCount);
//            }
            return Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            Listener.super.onOpen(webSocket);
            System.out.println("onOpen");
            //webSocket.sendText("{\"action\":\"getAllMessages\"}", true);
            //System.out.println(messages);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("onClose: " + statusCode + " " + reason);
            return Listener.super.onClose(webSocket, statusCode, reason);
        }
    };


}