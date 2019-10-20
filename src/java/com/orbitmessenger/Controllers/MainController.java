package com.orbitmessenger.Controllers;

import com.google.gson.JsonObject;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class MainController {

    private String username, password, server;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    @FXML
    private Button btnLogin;
    @FXML
    private Button btnLogout;
    @FXML
    private TextArea txtAreaServerMsgs;
    @FXML
    private TextField txtHostIP;
    @FXML
    private TextField txtUsername;
    @FXML
    private ListView<String> listUser;
    @FXML
    private TextArea txtUserMsg;

    private ObservableList<String> users;

    // Server Configuration
    private boolean connected;

    // for I/O
    private ObjectInputStream sInput;		// to read from the socket
    private ObjectOutputStream sOutput;		// to write on the socket
    private Socket socket;

    public void initialize(){
        this.getAllMessages();
    }
    /**
     * To send a message to the server
     */
    public void sendMessage() {
        System.out.println(txtUserMsg.getText());
        JsonObject json = new JsonObject();
        json.addProperty("message", txtUserMsg.getText());
        HttpResponse<JsonNode> response = Unirest.post(this.getServer() + "/addMessage")
                .basicAuth(this.getUsername(), this.getPassword())
                .header("accept", "application/json")
                .body(json)
                .asJson();
        System.out.println(response.getStatus());
        txtUserMsg.setText("");
        getAllMessages();
    }

    /**
     * Gets all the message from the Server
     */
    public void getAllMessages() {
//        JsonNode jsonData = Unirest.get("http://localhost:3000/getAllMessages")
//                .basicAuth("brody", "test").asJson().getBody();
//        JSONArray test = jsonData.getArray();
//        for (Object x : test){
//            display(test.toString());
//        }
        JSONArray messages = Unirest.get("http://localhost:3000/getAllMessages")
                .basicAuth(this.getUsername(), this.getPassword())
                .asJson().getBody().getArray();
        display(messages);
        //txtAreaServerMsgs.appendText(msg);
        System.out.println(messages);
    }

    /**
     * Sends message to server
     * Used by TextArea txtUserMsg to handle Enter key event
     */
    public void handleEnterPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            sendMessage();
            event.consume();
        }
    }

    /*
     * To send a message to the console or the GUI
     */
    private void display(JSONArray messages) {
        ArrayList<String> messageStrings = new ArrayList<>();
        for (Object message : messages){
            JSONObject m = (JSONObject) message;
            messageStrings.add(
                    m.get("username").toString() + ":\n" + m.get("message").toString() + "\n\n"
            );
        }
        txtAreaServerMsgs.clear();
        txtAreaServerMsgs.appendText(messageStrings.toString().replace("[]", "")); // append to the ServerChatArea
    }
}
