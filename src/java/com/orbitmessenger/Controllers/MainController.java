package com.orbitmessenger.Controllers;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.ExecutionException;

public class MainController extends ControllerUtil {

    private String username, password, server;
    private JsonObject properties;

    @FXML
    private VBox messagesVbox;
    @FXML
    private ScrollPane messagesScrollPane;

    @FXML
    private Button btnLogin;
    @FXML
    private Button btnLogout;
    @FXML
    private Button btnSend;
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

    private WSClient wsClient;

    public void initialize() throws URISyntaxException {
        System.out.println("Server: " + this.getServer());
        wsClient = new WSClient( new URI( this.getServer()+"/ws" ));
        wsClient.connect();
        updateMessages();
    }

    private String getUsername() {
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

    private String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    private JsonObject getProperties() { return properties; }

    public void setProperties(JsonObject properties) {this.properties = properties; }

    private Timer timer = new Timer();

    /**
     * Sends message to server
     * Used by TextArea txtUserMsg to handle Enter key event
     */
    public void handleEnterPressed(KeyEvent event) throws ExecutionException, InterruptedException {
        if (event.getCode() == KeyCode.ENTER) {
            sendMessage();
        }
    }

    /**
     * Returns the last messageId of all of our messages!
     */

    public int retrieveLastMessageID() {
        waitForAllMessages();
        JsonArray jsonArray = wsClient.getAllMessages();
        int lastElement = jsonArray.size();
        JsonObject lastId = jsonArray.get(lastElement - 1).getAsJsonObject();
        return lastId.get("messageId").getAsInt();
    }

    /**
     * To send a message to the server
     */
    public void sendMessage() {
        String message = txtUserMsg.getText().trim();
        if (!checkForEmptyMessage(message)) {
            String action = "add";
            Integer lastMessageID = retrieveLastMessageID();

            JsonObject submitMessage = wsClient.createSubmitObject(action,
                    message,
                    getUsername(),
                    lastMessageID,
                    getProperties());

            wsClient.send(submitMessage.toString().trim());
            txtUserMsg.setText("");
            txtUserMsg.requestFocus();
            wsClient.resetAllMessages();
            updateMessages();
        } else {
            System.out.println("Empty message. Not sending!");
            txtUserMsg.setText("");
            txtUserMsg.requestFocus();
        }
    }

    /**
     * To send a message to the console or the GUI
     */
    public void updateMessages() {
        waitForAllMessages();
        JsonArray jsonArray = wsClient.getAllMessages();
        ArrayList<VBox> messageBoxes = new ArrayList<>();
        for (Object message : jsonArray){
            JsonObject m = (JsonObject) message;
            messageBoxes.add(
                    createMessageBox(m.get("username").toString().replace("\"", ""),
                    m.get("message").toString().replace("\"", ""))
            );
        }
        //this.messagesVbox.getChildren().clear();
        this.messagesVbox.getChildren().addAll(messageBoxes);

        // Scrolls to the bottom
        scrollToBottom();
    }

    /**
     * Creates a message box with proper formatting
     */
    private VBox createMessageBox(String username, String message){
        VBox vbox = new VBox();
        // check if username == the current user or moves messages to the right
        if(!username.equals(this.getUsername())){
            vbox.setAlignment(Pos.CENTER_RIGHT);
        }
        vbox.setStyle(".messageBox");
        vbox.getStyleClass().add("messageBox");
        Label usernameLabel = new Label();
        Label messageLabel = new Label();
        usernameLabel.setText(username);
        messageLabel.setText(message);
        vbox.getChildren().add(usernameLabel);
        vbox.getChildren().add(messageLabel);
        return vbox;
    }

    /**
     * Checks to make sure that the message we're trying to send isn't empty!
     */
    private boolean checkForEmptyMessage(String message) {
        return (message.isEmpty());
    }

    /**
     * Closes the program
     */
    public void closeProgram() {
        System.out.println("Calling Platform.exit():");
        Platform.exit();
        System.out.println("Calling System.exit(0):");
        System.exit(0);
    }

    /**
     * Popup dialog box displaying About information!
     */
    public void popupAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Orbit Messenger");
        alert.setHeaderText(null);
        String line2 = ("Designed and built by Brody and Maxwell in Utah!" + "\n" + "https://github.com/MaxwellM/Orbit-Messenger");
        // create a hyperlink
        Hyperlink hyperlink = new Hyperlink("hyperlink");
        alert.setContentText(line2);
        alert.showAndWait();
    }

    /**
     * Switch back to the login scene
     */
    public void switchToLogin() {
        // Stop timer
        //wsClient.stopIntervalForPing();
        // Switches back to the Login Controller/Window
        LoginController login = new LoginController();
        ControllerUtil ctrlUtl = new ControllerUtil();
        login.changeSceneTo(ctrlUtl.LOGIN_FXML, login, (Stage) txtUserMsg.getScene().getWindow());
    }

    /**
     * Scrolls to the chat window to the bottom.
     * Preferable when there are new messages.
     */
    public void scrollToBottom() {
        //messagesScrollPane.setVvalue(1);
        messagesScrollPane.vvalueProperty().bind(messagesVbox.heightProperty());
        //messagesScrollPane.vvalueProperty().bind(messagesVbox.heightProperty());
    }

    /**
     * This will return when allMessages from wsClient is > 0.
     */
    private void waitForAllMessages() {
        Thread waitForMessages = new Thread(() -> {
            while (wsClient.getAllMessages().size() == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        waitForMessages.run();
    }
}
