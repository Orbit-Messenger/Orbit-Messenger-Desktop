package com.orbitmessenger.Controllers;

import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletionStage;

public class WSClient {

    private String messages = "";

    public String getMessage() {
        String mp = messages;
        messages = "";
        return mp;
    }

    Listener wsListener = new Listener() {
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messages += data.toString().trim();
            return Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            Listener.super.onOpen(webSocket);
            System.out.println("onOpen");
            webSocket.sendText("{\"action\":\"getAllMessages\"}", true);
            System.out.println(messages);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("onClose: " + statusCode + " " + reason);
            return Listener.super.onClose(webSocket, statusCode, reason);
        }
    };


}