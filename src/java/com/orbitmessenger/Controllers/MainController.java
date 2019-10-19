package com.orbitmessenger.Controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MainController {
    // Using this project as an example:
    // https://github.com/GabrielRivera21/ChatAppFx

    // Java FX Implementation
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
    private String server, username;
    private int port;

    // for I/O
    private ObjectInputStream sInput;		// to read from the socket
    private ObjectOutputStream sOutput;		// to write on the socket
    private Socket socket;

    /**
     * To send a message to the server
     */
    public void sendMessage() {
        System.out.println(txtUserMsg.getText());
//        if (connected) {
//            ChatMessage msg = new ChatMessage(ChatMessage.MESSAGE, txtUserMsg.getText());
//            try {
//                sOutput.writeObject(msg);
//                txtUserMsg.setText("");
//            }
//            catch(IOException e) {
//                display("Exception writing to server: " + e);
//            }
//        }
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
}
