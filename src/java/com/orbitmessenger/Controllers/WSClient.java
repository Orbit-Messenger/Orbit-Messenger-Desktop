package com.orbitmessenger.Controllers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocketServerFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.DefaultWebSocketServerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;

public class WSClient extends WebSocketClient {

    private JsonObject serverResponse;
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
        setServerResponse(message);
    }

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

    public JsonObject getServerResponse(){
        JsonObject response = this.serverResponse;
        this.serverResponse = null;
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

    /**
     * Method which returns a SSLContext from a keystore or IllegalArgumentException on error
     *
     * @return a valid SSLContext
     * @throws IllegalArgumentException when some exception occurred
     */
    public SSLContext getSSLConextFromKeystore() {
        // load up the key store
        String storeType = "JKS";
        String keystore = "keystore.jks";
        String storePassword = "password";
        String keyPassword = "password";
        KeyStore ks;
        SSLContext sslContext;
        try {
            ks = KeyStore.getInstance(storeType);
            ks.load(Files.newInputStream(Paths.get("././", keystore)), storePassword.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keyPassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e) {
            System.out.println("UnRecoverable: " + e);
            throw new IllegalArgumentException();
        }
        return sslContext;
    }

    public final void setWebSocketFactory( WebSocketServerFactory wsf ) {
        if (this.wsf != null)
            this.wsf.close();
        this.wsf = wsf;
    }
}