package com.orbitmessenger.Controllers;

import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletionStage;

public class WSClient {

    public String messages = "";

    Listener wsListener = new Listener() {
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {

            System.out.println("onText: " + data);

            messages += data;

            return Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("onOpen");
            Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("onClose: " + statusCode + " " + reason);
            return Listener.super.onClose(webSocket, statusCode, reason);
        }
    };


}