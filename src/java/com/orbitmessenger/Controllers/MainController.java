package com.orbitmessenger.Controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainController extends ControllerUtil {

    private String username, password, server;
    private int localMessageCount = 0;

    @FXML
    VBox messagesVbox;
    @FXML
    ScrollPane messagesScrollPane;

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

    WebSocket webSocket;
    WSClient wsClient;
    HttpClient client;

    public void initialize(){
        while(wsClient == null){
           createWebsocketConnection();
        }
        //this.setIntervalForNewMessages();
        this.display();
    }

    public void createWebsocketConnection(){
        try {
            client = HttpClient.newHttpClient();
            wsClient = new WSClient();
            //System.out.println("URI: " + this.getServer()+"/ws");
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(this.getServer() + "/ws"), wsClient.wsListener).join();
            //webSocket.sendPing(ByteBuffer.wrap("Ping!".getBytes()));
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        System.out.println(wsClient.getMessage());
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
    }

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

    private Timer timer = new Timer();

    /**
     * Sets the interval of checking for new messages
     */
    private void setIntervalForNewMessages() {
        int begin = 0;
        int timeInterval = 1000;
        timer.schedule(new TimerTask() {
            int counter = 0;
            @Override
            public void run() {
                // Needed to update the UI without exploding!
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        display();
                    }
                });
            }
        }, begin, timeInterval);
    }

    /**
     * Stops the timer
     */
    private void stopIntervalForNewMessages() {
        timer.cancel();
        timer.purge();
    }

    /**
     * Sends message to server
     * Used by TextArea txtUserMsg to handle Enter key event
     */
    public void handleEnterPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            sendMessage();
        }
    }

    /**
     * To send a message to the server
     */
    public void sendMessage() {
        String message = txtUserMsg.getText().trim();
        if (!checkForEmptyMessage(message)) {
            System.out.println(message);
            String action = "{\"action\": \"addMessage\"}";
            String formattedMessage = "{\"message\": \"" + message + "\", \"username\":\"brody\"}";
            System.out.println(action);
            System.out.println(formattedMessage);
            webSocket.sendText(action, true);
            webSocket.sendText(formattedMessage, true);
            txtUserMsg.setText("");
            //getAllMessages();
        } else {
            System.out.println("Empty message. Not sending!");
            txtUserMsg.setText("");
        }
    }

    /**
     * Checks to see if there are new messages. If so, update them!
     */
//    public void checkForNewMessages() {
//        String returnedMessageCount = Unirest.get(this.getServer()+"/getMessageCount")
//                .basicAuth(this.getUsername(), this.getPassword())
//                .asString()
//                .getBody().trim();
//        int returnedMessageCountInt = Integer.parseInt(returnedMessageCount);
//        if (localMessageCount < returnedMessageCountInt) {
//            JSONArray messages = Unirest.get(this.getServer()+"/getAllMessages")
//                    .basicAuth(this.getUsername(), this.getPassword())
//                    .asJson().getBody().getArray();
//            display();
//            localMessageCount = returnedMessageCountInt;
//        }
//    }

    /**
     * Gets all the message from the Server
     */
//    public void getAllMessages() {
//        String json = new String(wsClient.messages);
//        JsonElement jelement = new JsonParser().parse(json.trim());
//        JsonArray  jsonArray = jelement.getAsJsonArray();
//
//        display();
//
//        System.out.println("LOOK HERE: " + wsClient.messages.toString());
//    }

    /**
     * To send a message to the console or the GUI
     */
    private void display() {
        JsonArray jsonArray = new JsonArray();
        try{
            wsClient.wsListener.onText(webSocket, "", true);
            String json = new String(wsClient.getMessage());
            JsonElement jelement = new JsonParser().parse(json.trim());
            jsonArray = jelement.getAsJsonArray();
        }catch (Exception e){
            System.out.println(e);
        }

        System.out.println("HIT");
        ArrayList<VBox> messageBoxes = new ArrayList<>();
        for (Object message : jsonArray){
            JsonObject m = (JsonObject) message;
            System.out.println("Username: " +m.get("username").toString());
            messageBoxes.add(
                    createMessageBox(m.get("username").toString().replace("\"", ""),
                    m.get("message").toString().replace("\"", ""))
            );
        }
        this.messagesVbox.getChildren().clear();
        this.messagesVbox.getChildren().addAll(messageBoxes);

        // Scrolls to the bottom
        messagesScrollPane.setVvalue(1.0);
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
        alert.setContentText("Designed and built by Brody and Maxwell in Utah!");
        // create a hyperlink
        Hyperlink hyperlink = new Hyperlink("hyperlink");
        alert.setContentText("Github: ");
        alert.showAndWait();
    }

    /**
     * Switch back to the login scene
     */
    public void switchToLogin() {
        // Stop timer
        //stopIntervalForNewMessages();
        // Switches back to the Login Controller/Window
        LoginController login = new LoginController();
        ControllerUtil ctrlUtl = new ControllerUtil();
        login.changeSceneTo(ctrlUtl.LOGIN_FXML, login, (Stage) txtUserMsg.getScene().getWindow());
    }
}
