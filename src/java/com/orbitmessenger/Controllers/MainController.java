package com.orbitmessenger.Controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController extends ControllerUtil {

    private String username, password, server;
    private JsonObject properties;

    @FXML
    private VBox messagesVbox;
    @FXML
    private ScrollPane messagesScrollPane;

    @FXML
    private Button btnSend;
    @FXML
    ListView userListView;
    @FXML
    private TextArea messageTextArea;

    private ObservableList<String> users;

    // Server Configuration
    private boolean connected;

    private WSClient wsClient;

    public void initialize() throws URISyntaxException {
        wsClient = new WSClient(new URI(this.getServer() + "/"));
        wsClient.connect();
        updateHandler.start();
        //updateMessages();
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

    private JsonObject getProperties() {
        return properties;
    }

    public void setProperties(JsonObject properties) {
        this.properties = properties;
    }

    private Thread updateHandler = new Thread(new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    JsonObject serverMessage = wsClient.getServerResponse();
                    if(serverMessage != null) {
                        updateMessages(getMessagesFromJsonObject(serverMessage));
                        updateUsers(getUsersFromJsonObject(serverMessage));
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        ;
    });

    private JsonArray getMessagesFromJsonObject(JsonObject serverResponse){
        if(serverResponse.has("messages")){
            return serverResponse.getAsJsonArray("messages");
        }
        return null;
    }

    private JsonArray getUsersFromJsonObject(JsonObject serverResponse){
        String jsonKey = "activeUsers";
        if(serverResponse.has(jsonKey)){
            try{
                return serverResponse.getAsJsonArray(jsonKey);
            } catch (Exception e){
                return null;
            }
        }
        return null;
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
     * Returns the last messageId of all of our messages!
     */

//    public int retrieveLastMessageID() {
//        waitForAllMessages();
//        JsonArray jsonArray = wsClient.getAllMessages();
//        int lastElement = jsonArray.size();
//        JsonObject lastId = jsonArray.get(lastElement - 1).getAsJsonObject();
//        return lastId.get("messageId").getAsInt();
//    }

    /**
     * To send a message to the server
     */
    public void sendMessage() {
        String userText = messageTextArea.getText().trim();
        if(userText.isEmpty()) {
            return;
        }

        JsonObject submitMessage = wsClient.createSubmitObject(
                "add",
                userText,
                getUsername(),
                getProperties()
        );

        System.out.println("Message to send: " + submitMessage.toString());

        wsClient.send(submitMessage.toString().trim());
        messageTextArea.setText("");
        messageTextArea.requestFocus();
    }

    /**
     * To send a message to the console or the GUI
     */
    public void updateMessages(JsonArray newMessages) {
        if(newMessages == null){
            return;
        }
        ArrayList<VBox> messageBoxes = new ArrayList<>();
        for (Object message : newMessages) {
            JsonObject m = (JsonObject) message;
            messageBoxes.add(
                    createMessageBox(
                            m.get("username").toString().replace("\"", ""),
                            m.get("message").toString().replace("\"", ""))
            );
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                messagesVbox.getChildren().addAll(messageBoxes);
            }
        });

        // Scrolls to the bottom
        scrollToBottom();
    }

    public void updateUsers(JsonArray users) {
        if(users == null){
            return;
        }
        ArrayList<Label> userLabels = new ArrayList<>();
        for (Object user : users) {
            Label label = new Label();
            label.setText(user.toString());
            userLabels.add(label);
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                userListView.getItems().clear();
                userListView.getItems().addAll(userLabels);
            }
        });

        // Scrolls to the bottom
        scrollToBottom();
    }

    /**
     * Creates a message box with proper formatting
     */
    private VBox createMessageBox(String username, String message) {
        VBox vbox = new VBox();
        // check if username == the current user or moves messages to the right
        if (!username.equals(this.getUsername())) {
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
        login.changeSceneTo(ctrlUtl.LOGIN_FXML, login, (Stage) messageTextArea.getScene().getWindow());
    }

    /**
     * Scrolls to the chat window to the bottom.
     * Preferable when there are new messages.
     */
    public void scrollToBottom() {
        messagesScrollPane.vvalueProperty().bind(messagesVbox.heightProperty());
    }

    public void selectMessageToDelete(MouseEvent event) {
        String text = event.getTarget().toString();
        String regex = "\"([^\"]*)\"";
        Pattern pat = Pattern.compile(regex);
        Matcher m = pat.matcher(text);
        if (m.find()) {
            System.out.println(m.group(1));
//            action = "delete";
//            inComingMessage = m.group(1);
            sendMessage();
        }
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

}
